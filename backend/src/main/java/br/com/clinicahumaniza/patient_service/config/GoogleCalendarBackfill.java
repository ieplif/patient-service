package br.com.clinicahumaniza.patient_service.config;

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
 * Backfill único do Google Calendar: na subida, cria eventos para os agendamentos
 * FUTUROS e ativos (AGENDADO/CONFIRMADO) que ainda não foram sincronizados.
 *
 * Acionado por google.calendar.backfill-on-startup=true (env GOOGLE_CALENDAR_BACKFILL).
 * Idempotente: só processa quem está sem googleCalendarEventId, então pode rodar
 * mais de uma vez sem duplicar. Recomenda-se desligar a flag após o primeiro uso.
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
        List<Agendamento> futuros = agendamentoRepository.findByStatusInAndDataHoraGreaterThanEqual(
                List.of(StatusAgendamento.AGENDADO, StatusAgendamento.CONFIRMADO), LocalDateTime.now());

        log.info("Sincronização do Google Calendar: {} agendamento(s) futuro(s).", futuros.size());
        if (futuros.isEmpty()) return;

        GoogleCalendarService service = googleCalendarService.get();
        // Roda em série, com pausa entre chamadas, para não estourar o limite de uso do
        // Google (403 em rajada). Em thread de fundo para não travar a subida. Cria os que
        // ainda não têm evento e atualiza (repinta) os que já têm.
        Thread worker = new Thread(
                () -> {
                    for (Agendamento agendamento : futuros) {
                        if (agendamento.getGoogleCalendarEventId() == null) {
                            service.createEventSync(agendamento);
                        } else {
                            service.updateEventSync(agendamento);
                        }
                        try {
                            Thread.sleep(INTERVALO_MS);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                    log.info("Sincronização do Google Calendar concluída ({} processado[s]).", futuros.size());
                },
                "gcal-backfill");
        worker.setDaemon(true);
        worker.start();
    }
}
