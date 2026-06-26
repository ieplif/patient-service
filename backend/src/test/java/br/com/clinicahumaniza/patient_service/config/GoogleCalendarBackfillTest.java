package br.com.clinicahumaniza.patient_service.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.clinicahumaniza.patient_service.model.Agendamento;
import br.com.clinicahumaniza.patient_service.repository.AgendamentoRepository;
import br.com.clinicahumaniza.patient_service.service.GoogleCalendarService;

@ExtendWith(MockitoExtension.class)
class GoogleCalendarBackfillTest {

    @Mock
    private AgendamentoRepository agendamentoRepository;

    @Mock
    private GoogleCalendarService googleCalendarService;

    @Test
    @DisplayName("Cria os futuros sem evento e repinta (update) os que já têm")
    void backfill_sincronizaFuturos() {
        Agendamento semEvento = new Agendamento();
        semEvento.setId(UUID.randomUUID());
        Agendamento comEvento = new Agendamento();
        comEvento.setId(UUID.randomUUID());
        comEvento.setGoogleCalendarEventId("evt-existente");

        when(agendamentoRepository.findByStatusInAndDataHoraGreaterThanEqual(anyList(), any()))
                .thenReturn(List.of(semEvento, comEvento));

        GoogleCalendarBackfill backfill =
                new GoogleCalendarBackfill(agendamentoRepository, Optional.of(googleCalendarService));
        backfill.run(null);

        // Roda em thread de fundo, em série com pausa — espera com timeout.
        verify(googleCalendarService, timeout(3000)).createEventSync(semEvento);
        verify(googleCalendarService, timeout(3000)).updateEventSync(comEvento);
    }

    @Test
    @DisplayName("Não faz nada quando a integração está desligada")
    void backfill_integracaoDesligada_naoFaz() {
        GoogleCalendarBackfill backfill = new GoogleCalendarBackfill(agendamentoRepository, Optional.empty());
        backfill.run(null);

        verifyNoInteractions(agendamentoRepository);
    }
}
