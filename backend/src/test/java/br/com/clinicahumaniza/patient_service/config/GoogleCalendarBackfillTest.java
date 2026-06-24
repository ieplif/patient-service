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
    @DisplayName("Chama createEvent para cada agendamento futuro sem evento")
    void backfill_sincronizaFuturos() {
        Agendamento a1 = new Agendamento();
        a1.setId(UUID.randomUUID());
        Agendamento a2 = new Agendamento();
        a2.setId(UUID.randomUUID());
        when(agendamentoRepository.findByGoogleCalendarEventIdIsNullAndStatusInAndDataHoraGreaterThanEqual(
                        anyList(), any()))
                .thenReturn(List.of(a1, a2));

        GoogleCalendarBackfill backfill =
                new GoogleCalendarBackfill(agendamentoRepository, Optional.of(googleCalendarService));
        backfill.run(null);

        verify(googleCalendarService).createEvent(a1);
        verify(googleCalendarService).createEvent(a2);
    }

    @Test
    @DisplayName("Não faz nada quando a integração está desligada")
    void backfill_integracaoDesligada_naoFaz() {
        GoogleCalendarBackfill backfill = new GoogleCalendarBackfill(agendamentoRepository, Optional.empty());
        backfill.run(null);

        verifyNoInteractions(agendamentoRepository);
    }
}
