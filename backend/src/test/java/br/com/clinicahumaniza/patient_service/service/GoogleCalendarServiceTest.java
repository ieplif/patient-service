package br.com.clinicahumaniza.patient_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.Calendar.Events;
import com.google.api.services.calendar.Calendar.Events.Delete;
import com.google.api.services.calendar.Calendar.Events.Insert;
import com.google.api.services.calendar.Calendar.Events.Update;
import com.google.api.services.calendar.model.Event;

import br.com.clinicahumaniza.patient_service.model.*;
import br.com.clinicahumaniza.patient_service.repository.AgendamentoRepository;

@ExtendWith(MockitoExtension.class)
class GoogleCalendarServiceTest {

    @Mock
    private Calendar calendar;

    @Mock
    private AgendamentoRepository agendamentoRepository;

    @Mock
    private Events events;

    @Mock
    private Insert insert;

    @Mock
    private Update update;

    @Mock
    private Delete delete;

    private GoogleCalendarService googleCalendarService;

    private Agendamento agendamento;
    private static final String CLINIC_CALENDAR_ID = "clinic@group.calendar.google.com";

    @BeforeEach
    void setUp() {
        googleCalendarService = new GoogleCalendarService(calendar, agendamentoRepository);
        ReflectionTestUtils.setField(googleCalendarService, "clinicCalendarId", CLINIC_CALENDAR_ID);

        Atividade atividade = new Atividade();
        atividade.setNome("Pilates");

        Plano plano = new Plano();
        plano.setNome("Mensal");

        Servico servico = new Servico();
        servico.setAtividade(atividade);
        servico.setPlano(plano);

        Patient paciente = new Patient();
        paciente.setId(UUID.randomUUID());
        paciente.setNomeCompleto("Maria Santos");

        Profissional profissional = new Profissional();
        profissional.setNome("Dr. Ana");
        profissional.setGoogleCalendarId(null);

        agendamento = new Agendamento();
        agendamento.setId(UUID.randomUUID());
        agendamento.setPaciente(paciente);
        agendamento.setProfissional(profissional);
        agendamento.setServico(servico);
        agendamento.setDataHora(LocalDateTime.of(2025, 6, 2, 10, 0));
        agendamento.setDuracaoMinutos(50);
    }

    @Test
    @DisplayName("Deve criar evento no calendário da clínica com sucesso")
    void createEvent_Success() throws IOException {
        Event createdEvent = new Event();
        createdEvent.setId("event123");

        when(calendar.events()).thenReturn(events);
        when(events.insert(eq(CLINIC_CALENDAR_ID), any(Event.class))).thenReturn(insert);
        when(insert.execute()).thenReturn(createdEvent);
        when(agendamentoRepository.save(any(Agendamento.class))).thenReturn(agendamento);

        googleCalendarService.createEvent(agendamento);

        verify(events).insert(eq(CLINIC_CALENDAR_ID), any(Event.class));
        verify(agendamentoRepository).save(agendamento);
        assertThat(agendamento.getGoogleCalendarEventId()).isEqualTo("event123");
    }

    @Test
    @DisplayName("Deve criar evento também no calendário do profissional quando configurado")
    void createEvent_WithProfessionalCalendar() throws IOException {
        String profCalendarId = "dr.ana@gmail.com";
        agendamento.getProfissional().setGoogleCalendarId(profCalendarId);

        Event createdEvent = new Event();
        createdEvent.setId("event123");

        when(calendar.events()).thenReturn(events);
        when(events.insert(any(String.class), any(Event.class))).thenReturn(insert);
        when(insert.execute()).thenReturn(createdEvent);
        when(agendamentoRepository.save(any(Agendamento.class))).thenReturn(agendamento);

        googleCalendarService.createEvent(agendamento);

        // Should insert twice: clinic + professional
        verify(events, times(2)).insert(any(String.class), any(Event.class));
        verify(events).insert(eq(CLINIC_CALENDAR_ID), any(Event.class));
        verify(events).insert(eq(profCalendarId), any(Event.class));
    }

    @Test
    @DisplayName("Deve tratar erro do Google Calendar sem lançar exceção")
    void createEvent_IOException_DoesNotThrow() throws IOException {
        when(calendar.events()).thenReturn(events);
        when(events.insert(eq(CLINIC_CALENDAR_ID), any(Event.class))).thenReturn(insert);
        when(insert.execute()).thenThrow(new IOException("Google Calendar unavailable"));

        // Should not throw - just log the error
        googleCalendarService.createEvent(agendamento);

        verify(agendamentoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve atualizar evento existente no Google Calendar")
    void updateEvent_Success() throws IOException {
        agendamento.setGoogleCalendarEventId("event123");

        Event updatedEvent = new Event();
        updatedEvent.setId("event123");

        when(calendar.events()).thenReturn(events);
        when(events.update(eq(CLINIC_CALENDAR_ID), eq("event123"), any(Event.class)))
                .thenReturn(update);
        when(update.execute()).thenReturn(updatedEvent);

        googleCalendarService.updateEvent(agendamento);

        verify(events).update(eq(CLINIC_CALENDAR_ID), eq("event123"), any(Event.class));
    }

    @Test
    @DisplayName("Deve deletar evento do Google Calendar")
    void deleteEvent_Success() throws IOException {
        agendamento.setGoogleCalendarEventId("event123");

        when(calendar.events()).thenReturn(events);
        when(events.delete(CLINIC_CALENDAR_ID, "event123")).thenReturn(delete);

        googleCalendarService.deleteEvent(agendamento);

        verify(events).delete(CLINIC_CALENDAR_ID, "event123");
    }

    @Test
    @DisplayName("Deve criar evento mesmo sem profissional (vai só para a agenda da clínica)")
    void createEvent_SemProfissional() throws IOException {
        agendamento.setProfissional(null);

        Event createdEvent = new Event();
        createdEvent.setId("event123");

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        when(calendar.events()).thenReturn(events);
        when(events.insert(eq(CLINIC_CALENDAR_ID), eventCaptor.capture())).thenReturn(insert);
        when(insert.execute()).thenReturn(createdEvent);
        when(agendamentoRepository.save(any(Agendamento.class))).thenReturn(agendamento);

        googleCalendarService.createEvent(agendamento);

        // Só a agenda da clínica recebe o evento — sem NPE por profissional nulo
        verify(events, times(1)).insert(any(String.class), any(Event.class));
        verify(events).insert(eq(CLINIC_CALENDAR_ID), any(Event.class));
        // Título minimalista (primeiro + último nome), sem profissional
        assertThat(eventCaptor.getValue().getSummary()).isEqualTo("Maria Santos");
        assertThat(agendamento.getGoogleCalendarEventId()).isEqualTo("event123");
    }

    @Test
    @DisplayName("Título = primeiro + último nome; sem profissional/serviço; Pilates = azul (9)")
    void createEvent_TituloMinimalista() throws IOException {
        agendamento.getPaciente().setNomeCompleto("Maria das Graças Silva");

        Event createdEvent = new Event();
        createdEvent.setId("event123");

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

        when(calendar.events()).thenReturn(events);
        when(events.insert(eq(CLINIC_CALENDAR_ID), eventCaptor.capture())).thenReturn(insert);
        when(insert.execute()).thenReturn(createdEvent);
        when(agendamentoRepository.save(any(Agendamento.class))).thenReturn(agendamento);

        googleCalendarService.createEvent(agendamento);

        Event capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getSummary()).isEqualTo("Maria Silva"); // ignora conectivo "das"
        assertThat(capturedEvent.getColorId()).isEqualTo("9"); // Pilates = azul (Blueberry)
        assertThat(capturedEvent.getStart()).isNotNull();
        assertThat(capturedEvent.getEnd()).isNotNull();
    }

    @Test
    @DisplayName("Serviço define a cor do evento (Drenagem=teal/7, Fisio=6, Abdômen 360=3)")
    void createEvent_CorPorServico() throws IOException {
        agendamento.getServico().getAtividade().setNome("Drenagem");

        Event createdEvent = new Event();
        createdEvent.setId("event123");

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        when(calendar.events()).thenReturn(events);
        when(events.insert(eq(CLINIC_CALENDAR_ID), eventCaptor.capture())).thenReturn(insert);
        when(insert.execute()).thenReturn(createdEvent);
        when(agendamentoRepository.save(any(Agendamento.class))).thenReturn(agendamento);

        googleCalendarService.createEvent(agendamento);

        assertThat(eventCaptor.getValue().getColorId()).isEqualTo("7");
    }
}
