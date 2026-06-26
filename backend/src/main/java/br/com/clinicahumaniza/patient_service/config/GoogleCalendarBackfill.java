package br.com.clinicahumaniza.patient_service.config;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import br.com.clinicahumaniza.patient_service.model.Agendamento;
import br.com.clinicahumaniza.patient_service.model.StatusAgendamento;
import br.com.clinicahumaniza.patient_service.repository.AgendamentoRepository;
import br.com.clinicahumaniza.patient_service.service.GoogleCalendarService;
import lombok.RequiredArgsConstructor;

/**
 * Ressincronização do Google Calendar na subida. Acionada por
 * google.calendar.backfill-on-startup=true (env GOOGLE_CALENDAR_BACKFILL). Cobre do
 * início do mês corrente em diante (semana atual + futuro) e faz, em série e pausado
 * (limite do Google):
 *  - AGENDADO/CONFIRMADO/REALIZADO: cria os que faltam e repinta os que já têm;
 *  - CANCELADO que ainda tem evento (órfão de delete que falhou): apaga do Google.
 *
 * Idempotente — pode rodar mais de uma vez. Recomenda-se desligar a flag após o uso.
 */
@Component
@ConditionalOnProperty(name = "google.calendar.backfill-on-startup", havingValue = "true")
@RequiredArgsConstructor
public class GoogleCalendarBackfill implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(GoogleCalendarBackfill.class);

    // Pausa entre criações no backfill (~2/s) para respeitar o limite de uso do Google.
    private static final long INTERVALO_MS = 500;

    private final AgendamentoRepository agendamentoRepository;
    private final Optional<GoogleCalendarService> googleCalendarService;

    @Override
    public void run(ApplicationArguments args) {
        if (googleCalendarService.isEmpty()) {
            log.warn("Backfill do Google Calendar solicitado, mas a integração está desligada. Ignorando.");
            return;
        }
        // Cobre do início do mês corrente em diante (semana atual + futuro). Inclui
        // REALIZADO para repintar o histórico recente; CANCELADO entra na limpeza de órfãos.
        LocalDateTime desde = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        List<Agendamento> aSincronizar = agendamentoRepository.findByStatusInAndDataHoraGreaterThanEqual(
                List.of(StatusAgendamento.AGENDADO, StatusAgendamento.CONFIRMADO, StatusAgendamento.REALIZADO), desde);
        List<Agendamento> orfaos =
                agendamentoRepository.findByStatusAndDataHoraGreaterThanEqualAndGoogleCalendarEventIdIsNotNull(
                        StatusAgendamento.CANCELADO, desde);

        log.info(
                "Sincronização do Google Calendar: {} a sincronizar, {} órfão(s) a remover (desde {}).",
                aSincronizar.size(),
                orfaos.size(),
                desde.toLocalDate());
        if (aSincronizar.isEmpty() && orfaos.isEmpty()) return;

        GoogleCalendarService service = googleCalendarService.get();
        // Roda em série, pausado (limite do Google), em thread de fundo para não travar a subida.
        Thread worker = new Thread(
                () -> {
                    // Cria os que faltam e repinta/atualiza os que já têm.
                    for (Agendamento agendamento : aSincronizar) {
                        if (agendamento.getGoogleCalendarEventId() == null) {
                            service.createEventSync(agendamento);
                        } else {
                            service.updateEventSync(agendamento);
                        }
                        if (!pausar()) return;
                    }
                    // Órfãos (cancelados com evento): apaga do Google.
                    for (Agendamento agendamento : orfaos) {
                        service.deleteEventSync(agendamento);
                        if (!pausar()) return;
                    }
                    log.info(
                            "Sincronização do Google Calendar concluída ({} sincronizado[s], {} órfão[s]).",
                            aSincronizar.size(),
                            orfaos.size());
                },
                "gcal-backfill");
        worker.setDaemon(true);
        worker.start();
    }

    /** Pausa entre chamadas; retorna false se a thread foi interrompida. */
    private boolean pausar() {
        try {
            Thread.sleep(INTERVALO_MS);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
