package br.com.clinicahumaniza.patient_service.service;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.googleapis.services.AbstractGoogleClientRequest;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;

import br.com.clinicahumaniza.patient_service.model.Agendamento;
import br.com.clinicahumaniza.patient_service.repository.AgendamentoRepository;
import br.com.clinicahumaniza.patient_service.util.NomeCurto;

@Service
@ConditionalOnProperty(name = "google.calendar.enabled", havingValue = "true")
public class GoogleCalendarService {

    private static final Logger log = LoggerFactory.getLogger(GoogleCalendarService.class);
    private static final String TIMEZONE = "America/Sao_Paulo";
    // Tentativas em caso de limite de uso do Google (403/429), com backoff exponencial.
    private static final int MAX_TENTATIVAS = 4;

    private final Calendar calendar;
    private final AgendamentoRepository agendamentoRepository;

    @Value("${google.calendar.clinic-calendar-id}")
    private String clinicCalendarId;

    public GoogleCalendarService(Calendar calendar, AgendamentoRepository agendamentoRepository) {
        this.calendar = calendar;
        this.agendamentoRepository = agendamentoRepository;
    }

    @Async("googleCalendarExecutor")
    public void createEvent(Agendamento agendamento) {
        doCreateEvent(agendamento);
    }

    /** Versão síncrona (sem @Async) — usada pelo backfill, que controla o ritmo das chamadas. */
    public void createEventSync(Agendamento agendamento) {
        doCreateEvent(agendamento);
    }

    private void doCreateEvent(Agendamento agendamento) {
        try {
            Event event = buildEvent(agendamento);

            // Create on clinic calendar
            Event createdEvent = executarComRetry(calendar.events().insert(clinicCalendarId, event));
            String eventId = createdEvent.getId();

            // Save event ID on agendamento
            agendamento.setGoogleCalendarEventId(eventId);
            agendamentoRepository.save(agendamento);

            log.info("Google Calendar event created: {} for agendamento {}", eventId, agendamento.getId());

            // Create on professional's calendar if configured
            String profCalendarId = agendamento.getProfissional() != null
                    ? agendamento.getProfissional().getGoogleCalendarId()
                    : null;
            if (profCalendarId != null && !profCalendarId.isBlank()) {
                try {
                    calendar.events().insert(profCalendarId, event).execute();
                    log.info("Google Calendar event created on professional calendar: {}", profCalendarId);
                } catch (IOException e) {
                    log.warn("Failed to create event on professional calendar {}: {}", profCalendarId, e.getMessage());
                }
            }
        } catch (IOException e) {
            log.error(
                    "Failed to create Google Calendar event for agendamento {}: {}",
                    agendamento.getId(),
                    e.getMessage());
        }
    }

    @Async("googleCalendarExecutor")
    public void updateEvent(Agendamento agendamento) {
        doUpdateEvent(agendamento);
    }

    /** Versão síncrona (sem @Async) — usada pelo backfill/ressincronização. */
    public void updateEventSync(Agendamento agendamento) {
        doUpdateEvent(agendamento);
    }

    private void doUpdateEvent(Agendamento agendamento) {
        String eventId = agendamento.getGoogleCalendarEventId();
        if (eventId == null) {
            log.warn("No Google Calendar event ID for agendamento {}, creating new event", agendamento.getId());
            doCreateEvent(agendamento);
            return;
        }

        try {
            Event event = buildEvent(agendamento);

            // Update on clinic calendar
            executarComRetry(calendar.events().update(clinicCalendarId, eventId, event));
            log.info("Google Calendar event updated: {} for agendamento {}", eventId, agendamento.getId());

            // Update on professional's calendar if configured
            String profCalendarId = agendamento.getProfissional() != null
                    ? agendamento.getProfissional().getGoogleCalendarId()
                    : null;
            if (profCalendarId != null && !profCalendarId.isBlank()) {
                try {
                    calendar.events().update(profCalendarId, eventId, event).execute();
                    log.info("Google Calendar event updated on professional calendar: {}", profCalendarId);
                } catch (IOException e) {
                    log.warn("Failed to update event on professional calendar {}: {}", profCalendarId, e.getMessage());
                }
            }
        } catch (IOException e) {
            log.error(
                    "Failed to update Google Calendar event for agendamento {}: {}",
                    agendamento.getId(),
                    e.getMessage());
        }
    }

    @Async("googleCalendarExecutor")
    public void deleteEvent(Agendamento agendamento) {
        doDeleteEvent(agendamento);
    }

    /** Versão síncrona (sem @Async) — usada pela limpeza de órfãos no backfill. */
    public void deleteEventSync(Agendamento agendamento) {
        doDeleteEvent(agendamento);
    }

    private void doDeleteEvent(Agendamento agendamento) {
        String eventId = agendamento.getGoogleCalendarEventId();
        if (eventId == null) {
            log.warn("No Google Calendar event ID for agendamento {}, nothing to delete", agendamento.getId());
            return;
        }

        try {
            // Delete from clinic calendar
            executarComRetry(calendar.events().delete(clinicCalendarId, eventId));
            log.info("Google Calendar event deleted: {} for agendamento {}", eventId, agendamento.getId());

            // Delete from professional's calendar if configured
            String profCalendarId = agendamento.getProfissional() != null
                    ? agendamento.getProfissional().getGoogleCalendarId()
                    : null;
            if (profCalendarId != null && !profCalendarId.isBlank()) {
                try {
                    calendar.events().delete(profCalendarId, eventId).execute();
                    log.info("Google Calendar event deleted from professional calendar: {}", profCalendarId);
                } catch (IOException e) {
                    log.warn(
                            "Failed to delete event from professional calendar {}: {}", profCalendarId, e.getMessage());
                }
            }

            // Limpa o id após apagar — evita re-tentativas e identifica órfãos já resolvidos.
            agendamento.setGoogleCalendarEventId(null);
            agendamentoRepository.save(agendamento);
        } catch (IOException e) {
            log.error(
                    "Failed to delete Google Calendar event for agendamento {}: {}",
                    agendamento.getId(),
                    e.getMessage());
        }
    }

    /**
     * Executa uma requisição ao Google repetindo em caso de limite de uso (403/429),
     * com backoff exponencial. Demais erros são propagados na hora.
     */
    private <T> T executarComRetry(AbstractGoogleClientRequest<T> requisicao) throws IOException {
        long espera = 1000;
        for (int tentativa = 1; ; tentativa++) {
            try {
                return requisicao.execute();
            } catch (GoogleJsonResponseException e) {
                int code = e.getStatusCode();
                if ((code == 403 || code == 429) && tentativa < MAX_TENTATIVAS) {
                    log.warn(
                            "Limite do Google ({}). Tentativa {}/{} — aguardando {}ms.",
                            code,
                            tentativa,
                            MAX_TENTATIVAS,
                            espera);
                    try {
                        Thread.sleep(espera);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw e;
                    }
                    espera *= 2;
                } else {
                    throw e;
                }
            }
        }
    }

    private Event buildEvent(Agendamento agendamento) {
        String atividadeNome = agendamento.getServico().getAtividade().getNome();
        String planoNome = agendamento.getServico().getPlano().getNome();
        String pacienteNome = agendamento.getPaciente().getNomeCompleto();

        Event event = new Event();
        // Privacidade: título mostra só primeiro + último nome da paciente.
        // Sem nome de profissional e sem texto do serviço — o serviço é indicado pela COR.
        event.setSummary(NomeCurto.primeiroEUltimo(pacienteNome));

        String colorId = corDoServico(atividadeNome, planoNome);
        if (colorId != null) event.setColorId(colorId);

        // Mantém apenas observações operacionais, se houver.
        if (agendamento.getObservacoes() != null
                && !agendamento.getObservacoes().isBlank()) {
            event.setDescription(agendamento.getObservacoes());
        }

        ZonedDateTime start = agendamento.getDataHora().atZone(ZoneId.of(TIMEZONE));
        int duracaoMinutos = agendamento.getDuracaoMinutos() != null ? agendamento.getDuracaoMinutos() : 60;
        ZonedDateTime end = start.plusMinutes(duracaoMinutos);

        event.setStart(new EventDateTime()
                .setDateTime(new DateTime(start.toInstant().toEpochMilli()))
                .setTimeZone(TIMEZONE));

        event.setEnd(new EventDateTime()
                .setDateTime(new DateTime(end.toInstant().toEpochMilli()))
                .setTimeZone(TIMEZONE));

        return event;
    }

    /**
     * Cor do evento por serviço. IDs de cor do Google Calendar:
     * 3=Grape (violeta, Abdômen 360°), 7=Peacock (ciano, Drenagem), 9=Blueberry (azul,
     * Pilates), 6=Tangerine (laranja, Fisioterapia). Demais serviços ficam na cor padrão.
     *
     * Pilates e Drenagem usam startsWith para não capturar o combo
     * "Pacote Pilates, Fisio e Drenagem" (que cai na cor da Fisioterapia).
     */
    private String corDoServico(String atividadeNome, String planoNome) {
        String descricao = (atividadeNome + " - " + planoNome).toLowerCase();
        if (descricao.contains("abdômen 360") || descricao.contains("abdomen 360")) return "3";
        if (descricao.startsWith("drenagem")) return "7";
        if (descricao.startsWith("pilates")) return "9";
        if (descricao.contains("fisio")) return "6";
        return null;
    }
}
