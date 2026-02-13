package br.com.clinicahumaniza.patient_service.service;

import br.com.clinicahumaniza.patient_service.model.Agendamento;
import br.com.clinicahumaniza.patient_service.repository.AgendamentoRepository;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Service
@ConditionalOnProperty(name = "google.calendar.enabled", havingValue = "true")
public class GoogleCalendarService {

    private static final Logger log = LoggerFactory.getLogger(GoogleCalendarService.class);
    private static final String TIMEZONE = "America/Sao_Paulo";

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
        try {
            Event event = buildEvent(agendamento);

            // Create on clinic calendar
            Event createdEvent = calendar.events().insert(clinicCalendarId, event).execute();
            String eventId = createdEvent.getId();

            // Save event ID on agendamento
            agendamento.setGoogleCalendarEventId(eventId);
            agendamentoRepository.save(agendamento);

            log.info("Google Calendar event created: {} for agendamento {}", eventId, agendamento.getId());

            // Create on professional's calendar if configured
            String profCalendarId = agendamento.getProfissional().getGoogleCalendarId();
            if (profCalendarId != null && !profCalendarId.isBlank()) {
                try {
                    calendar.events().insert(profCalendarId, event).execute();
                    log.info("Google Calendar event created on professional calendar: {}", profCalendarId);
                } catch (IOException e) {
                    log.warn("Failed to create event on professional calendar {}: {}", profCalendarId, e.getMessage());
                }
            }
        } catch (IOException e) {
            log.error("Failed to create Google Calendar event for agendamento {}: {}", agendamento.getId(), e.getMessage());
        }
    }

    @Async("googleCalendarExecutor")
    public void updateEvent(Agendamento agendamento) {
        String eventId = agendamento.getGoogleCalendarEventId();
        if (eventId == null) {
            log.warn("No Google Calendar event ID for agendamento {}, creating new event", agendamento.getId());
            createEvent(agendamento);
            return;
        }

        try {
            Event event = buildEvent(agendamento);

            // Update on clinic calendar
            calendar.events().update(clinicCalendarId, eventId, event).execute();
            log.info("Google Calendar event updated: {} for agendamento {}", eventId, agendamento.getId());

            // Update on professional's calendar if configured
            String profCalendarId = agendamento.getProfissional().getGoogleCalendarId();
            if (profCalendarId != null && !profCalendarId.isBlank()) {
                try {
                    calendar.events().update(profCalendarId, eventId, event).execute();
                    log.info("Google Calendar event updated on professional calendar: {}", profCalendarId);
                } catch (IOException e) {
                    log.warn("Failed to update event on professional calendar {}: {}", profCalendarId, e.getMessage());
                }
            }
        } catch (IOException e) {
            log.error("Failed to update Google Calendar event for agendamento {}: {}", agendamento.getId(), e.getMessage());
        }
    }

    @Async("googleCalendarExecutor")
    public void deleteEvent(Agendamento agendamento) {
        String eventId = agendamento.getGoogleCalendarEventId();
        if (eventId == null) {
            log.warn("No Google Calendar event ID for agendamento {}, nothing to delete", agendamento.getId());
            return;
        }

        try {
            // Delete from clinic calendar
            calendar.events().delete(clinicCalendarId, eventId).execute();
            log.info("Google Calendar event deleted: {} for agendamento {}", eventId, agendamento.getId());

            // Delete from professional's calendar if configured
            String profCalendarId = agendamento.getProfissional().getGoogleCalendarId();
            if (profCalendarId != null && !profCalendarId.isBlank()) {
                try {
                    calendar.events().delete(profCalendarId, eventId).execute();
                    log.info("Google Calendar event deleted from professional calendar: {}", profCalendarId);
                } catch (IOException e) {
                    log.warn("Failed to delete event from professional calendar {}: {}", profCalendarId, e.getMessage());
                }
            }
        } catch (IOException e) {
            log.error("Failed to delete Google Calendar event for agendamento {}: {}", agendamento.getId(), e.getMessage());
        }
    }

    private Event buildEvent(Agendamento agendamento) {
        String atividadeNome = agendamento.getServico().getAtividade().getNome();
        String planoNome = agendamento.getServico().getPlano().getNome();
        String pacienteNome = agendamento.getPaciente().getNomeCompleto();
        String profissionalNome = agendamento.getProfissional().getNome();

        String title = atividadeNome + " - " + planoNome + " | " + pacienteNome;

        StringBuilder description = new StringBuilder();
        description.append("Profissional: ").append(profissionalNome).append("\n");
        description.append("Paciente: ").append(pacienteNome).append("\n");
        description.append("Serviço: ").append(atividadeNome).append(" - ").append(planoNome).append("\n");
        if (agendamento.getObservacoes() != null && !agendamento.getObservacoes().isBlank()) {
            description.append("Observações: ").append(agendamento.getObservacoes());
        }

        ZonedDateTime start = agendamento.getDataHora().atZone(ZoneId.of(TIMEZONE));
        ZonedDateTime end = start.plusMinutes(agendamento.getDuracaoMinutos());

        Event event = new Event();
        event.setSummary(title);
        event.setDescription(description.toString());

        event.setStart(new EventDateTime()
                .setDateTime(new DateTime(start.toInstant().toEpochMilli()))
                .setTimeZone(TIMEZONE));

        event.setEnd(new EventDateTime()
                .setDateTime(new DateTime(end.toInstant().toEpochMilli()))
                .setTimeZone(TIMEZONE));

        return event;
    }
}
