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

    private final AgendamentoRepository agendamentoRepository;
    private final Optional<GoogleCalendarService> googleCalendarService;

    @Override
    public void run(ApplicationArguments args) {
        if (googleCalendarService.isEmpty()) {
            log.warn("Backfill do Google Calendar solicitado, mas a integração está desligada. Ignorando.");
            return;
        }
        List<Agendamento> futuros =
                agendamentoRepository.findByGoogleCalendarEventIdIsNullAndStatusInAndDataHoraGreaterThanEqual(
                        List.of(StatusAgendamento.AGENDADO, StatusAgendamento.CONFIRMADO), LocalDateTime.now());

        log.info("Backfill do Google Calendar: {} agendamento(s) futuro(s) a sincronizar.", futuros.size());
        // createEvent é assíncrono (executor dedicado) — dispara e grava o eventId em cada um.
        futuros.forEach(agendamento -> googleCalendarService.get().createEvent(agendamento));
    }
}
