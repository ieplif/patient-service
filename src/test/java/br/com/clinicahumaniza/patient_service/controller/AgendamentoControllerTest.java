package br.com.clinicahumaniza.patient_service.controller;

import br.com.clinicahumaniza.patient_service.dto.*;
import br.com.clinicahumaniza.patient_service.mapper.AgendamentoMapper;
import br.com.clinicahumaniza.patient_service.model.*;
import br.com.clinicahumaniza.patient_service.security.JwtAuthenticationFilter;
import br.com.clinicahumaniza.patient_service.security.JwtService;
import br.com.clinicahumaniza.patient_service.security.SecurityConfig;
import br.com.clinicahumaniza.patient_service.service.AgendamentoRecorrenteService;
import br.com.clinicahumaniza.patient_service.service.AgendamentoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AgendamentoController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AgendamentoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AgendamentoService agendamentoService;

    @MockitoBean
    private AgendamentoRecorrenteService recorrenteService;

    @MockitoBean
    private AgendamentoMapper agendamentoMapper;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private ObjectMapper objectMapper;
    private Agendamento agendamento;
    private AgendamentoResponseDTO responseDTO;
    private UUID agendamentoId;
    private UUID pacienteId;
    private UUID profissionalId;
    private UUID servicoId;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        agendamentoId = UUID.randomUUID();
        pacienteId = UUID.randomUUID();
        profissionalId = UUID.randomUUID();
        servicoId = UUID.randomUUID();

        Atividade atividade = new Atividade();
        atividade.setId(UUID.randomUUID());
        atividade.setNome("Pilates");

        Plano plano = new Plano();
        plano.setId(UUID.randomUUID());
        plano.setNome("Mensal");

        Patient paciente = new Patient();
        paciente.setId(pacienteId);
        paciente.setNomeCompleto("Maria Santos");

        Profissional profissional = new Profissional();
        profissional.setId(profissionalId);
        profissional.setNome("Dr. Ana");

        Servico servico = new Servico();
        servico.setId(servicoId);
        servico.setAtividade(atividade);
        servico.setPlano(plano);

        agendamento = new Agendamento();
        agendamento.setId(agendamentoId);
        agendamento.setPaciente(paciente);
        agendamento.setProfissional(profissional);
        agendamento.setServico(servico);
        agendamento.setDataHora(LocalDateTime.of(2025, 6, 2, 10, 0));
        agendamento.setDuracaoMinutos(50);
        agendamento.setStatus(StatusAgendamento.AGENDADO);
        agendamento.setAtivo(true);

        responseDTO = new AgendamentoResponseDTO();
        responseDTO.setId(agendamentoId);
        responseDTO.setPacienteId(pacienteId);
        responseDTO.setPacienteNome("Maria Santos");
        responseDTO.setProfissionalId(profissionalId);
        responseDTO.setProfissionalNome("Dr. Ana");
        responseDTO.setServicoId(servicoId);
        responseDTO.setServicoDescricao("Pilates - Mensal");
        responseDTO.setDataHora(LocalDateTime.of(2025, 6, 2, 10, 0));
        responseDTO.setDuracaoMinutos(50);
        responseDTO.setStatus(StatusAgendamento.AGENDADO);
        responseDTO.setAtivo(true);
        responseDTO.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Deve criar agendamento com autenticação - 201")
    @WithMockUser
    void createAgendamento_Authenticated_201() throws Exception {
        AgendamentoRequestDTO requestDTO = new AgendamentoRequestDTO();
        requestDTO.setPacienteId(pacienteId);
        requestDTO.setProfissionalId(profissionalId);
        requestDTO.setServicoId(servicoId);
        requestDTO.setDataHora(LocalDateTime.of(2025, 6, 2, 10, 0));
        requestDTO.setDuracaoMinutos(50);

        when(agendamentoService.createAgendamento(any(AgendamentoRequestDTO.class))).thenReturn(agendamento);
        when(agendamentoMapper.toResponseDTO(agendamento)).thenReturn(responseDTO);

        mockMvc.perform(post("/api/v1/agendamentos")
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pacienteNome").value("Maria Santos"))
                .andExpect(jsonPath("$.profissionalNome").value("Dr. Ana"))
                .andExpect(jsonPath("$.servicoDescricao").value("Pilates - Mensal"));
    }

    @Test
    @DisplayName("Deve listar agendamentos com autenticação - 200")
    @WithMockUser
    void getAllAgendamentos_Authenticated_200() throws Exception {
        when(agendamentoService.getAllAgendamentos()).thenReturn(List.of(agendamento));
        when(agendamentoMapper.toResponseDTO(agendamento)).thenReturn(responseDTO);

        mockMvc.perform(get("/api/v1/agendamentos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].pacienteNome").value("Maria Santos"));
    }

    @Test
    @DisplayName("Deve buscar agendamento por ID com autenticação - 200")
    @WithMockUser
    void getAgendamentoById_Authenticated_200() throws Exception {
        when(agendamentoService.getAgendamentoById(agendamentoId)).thenReturn(agendamento);
        when(agendamentoMapper.toResponseDTO(agendamento)).thenReturn(responseDTO);

        mockMvc.perform(get("/api/v1/agendamentos/{id}", agendamentoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pacienteNome").value("Maria Santos"));
    }

    @Test
    @DisplayName("Deve atualizar agendamento com autenticação - 200")
    @WithMockUser
    void updateAgendamento_Authenticated_200() throws Exception {
        AgendamentoUpdateDTO updateDTO = new AgendamentoUpdateDTO();
        updateDTO.setObservacoes("Remarcado");

        when(agendamentoService.updateAgendamento(any(UUID.class), any(AgendamentoUpdateDTO.class)))
                .thenReturn(agendamento);
        when(agendamentoMapper.toResponseDTO(agendamento)).thenReturn(responseDTO);

        mockMvc.perform(put("/api/v1/agendamentos/{id}", agendamentoId)
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Deve alterar status com autenticação - 200")
    @WithMockUser
    void updateStatus_Authenticated_200() throws Exception {
        AgendamentoStatusDTO statusDTO = new AgendamentoStatusDTO(StatusAgendamento.CONFIRMADO);

        when(agendamentoService.updateStatus(any(UUID.class), any(AgendamentoStatusDTO.class)))
                .thenReturn(agendamento);
        when(agendamentoMapper.toResponseDTO(agendamento)).thenReturn(responseDTO);

        mockMvc.perform(patch("/api/v1/agendamentos/{id}/status", agendamentoId)
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(statusDTO)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Deve deletar agendamento com autenticação - 204")
    @WithMockUser
    void deleteAgendamento_Authenticated_204() throws Exception {
        doNothing().when(agendamentoService).deleteAgendamento(agendamentoId);

        mockMvc.perform(delete("/api/v1/agendamentos/{id}", agendamentoId)
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Deve listar agendamentos por paciente - 200")
    @WithMockUser
    void getAgendamentosByPaciente_200() throws Exception {
        when(agendamentoService.getAgendamentosByPaciente(pacienteId)).thenReturn(List.of(agendamento));
        when(agendamentoMapper.toResponseDTO(agendamento)).thenReturn(responseDTO);

        mockMvc.perform(get("/api/v1/agendamentos/paciente/{pacienteId}", pacienteId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].pacienteNome").value("Maria Santos"));
    }

    @Test
    @DisplayName("Deve listar agendamentos por profissional - 200")
    @WithMockUser
    void getAgendamentosByProfissional_200() throws Exception {
        when(agendamentoService.getAgendamentosByProfissional(profissionalId)).thenReturn(List.of(agendamento));
        when(agendamentoMapper.toResponseDTO(agendamento)).thenReturn(responseDTO);

        mockMvc.perform(get("/api/v1/agendamentos/profissional/{profissionalId}", profissionalId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].profissionalNome").value("Dr. Ana"));
    }

    @Test
    @DisplayName("Deve consultar slots disponíveis - 200")
    @WithMockUser
    void getAvailableSlots_200() throws Exception {
        LocalDate data = LocalDate.of(2025, 6, 2);
        List<LocalDateTime> slots = List.of(
                data.atTime(8, 0),
                data.atTime(8, 50)
        );

        when(agendamentoService.getAvailableSlots(profissionalId, data, 50, 1)).thenReturn(slots);

        mockMvc.perform(get("/api/v1/agendamentos/profissional/{profissionalId}/slots-disponiveis", profissionalId)
                        .param("data", "2025-06-02")
                        .param("duracaoMinutos", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("Deve listar agendamentos por período - 200")
    @WithMockUser
    void getAgendamentosByPeriodo_200() throws Exception {
        when(agendamentoService.getAgendamentosByPeriodo(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(agendamento));
        when(agendamentoMapper.toResponseDTO(agendamento)).thenReturn(responseDTO);

        mockMvc.perform(get("/api/v1/agendamentos/data")
                        .param("inicio", "2025-06-01")
                        .param("fim", "2025-06-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].pacienteNome").value("Maria Santos"));
    }

    @Test
    @DisplayName("Deve retornar 401 sem autenticação")
    void getAllAgendamentos_Unauthenticated_401() throws Exception {
        mockMvc.perform(get("/api/v1/agendamentos"))
                .andExpect(status().isUnauthorized());
    }

    // --- Testes de recorrência ---

    @Test
    @DisplayName("Deve criar recorrência com autenticação - 201")
    @WithMockUser
    void createRecorrente_Authenticated_201() throws Exception {
        AgendamentoRecorrenteRequestDTO requestDTO = new AgendamentoRecorrenteRequestDTO();
        requestDTO.setPacienteId(pacienteId);
        requestDTO.setProfissionalId(profissionalId);
        requestDTO.setServicoId(servicoId);
        requestDTO.setFrequencia(FrequenciaRecorrencia.SEMANAL);
        requestDTO.setDiasSemana(List.of(java.time.DayOfWeek.MONDAY, java.time.DayOfWeek.WEDNESDAY));
        requestDTO.setHoraInicio(java.time.LocalTime.of(10, 0));
        requestDTO.setTotalSessoes(4);

        AgendamentoRecorrenteResponseDTO recorrenteResponse = new AgendamentoRecorrenteResponseDTO();
        recorrenteResponse.setId(UUID.randomUUID());
        recorrenteResponse.setPacienteId(pacienteId);
        recorrenteResponse.setPacienteNome("Maria Santos");
        recorrenteResponse.setFrequencia(FrequenciaRecorrencia.SEMANAL);
        recorrenteResponse.setAgendamentosCriados(List.of(responseDTO));
        recorrenteResponse.setDatasIgnoradas(List.of());

        when(recorrenteService.createRecorrente(any(AgendamentoRecorrenteRequestDTO.class)))
                .thenReturn(recorrenteResponse);

        mockMvc.perform(post("/api/v1/agendamentos/recorrente")
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pacienteNome").value("Maria Santos"))
                .andExpect(jsonPath("$.agendamentosCriados").isArray())
                .andExpect(jsonPath("$.datasIgnoradas").isArray());
    }

    @Test
    @DisplayName("Deve criar recorrência com datas ignoradas - 201")
    @WithMockUser
    void createRecorrente_ComDatasIgnoradas_201() throws Exception {
        AgendamentoRecorrenteRequestDTO requestDTO = new AgendamentoRecorrenteRequestDTO();
        requestDTO.setPacienteId(pacienteId);
        requestDTO.setProfissionalId(profissionalId);
        requestDTO.setServicoId(servicoId);
        requestDTO.setFrequencia(FrequenciaRecorrencia.SEMANAL);
        requestDTO.setDiasSemana(List.of(java.time.DayOfWeek.MONDAY));
        requestDTO.setHoraInicio(java.time.LocalTime.of(10, 0));
        requestDTO.setTotalSessoes(4);

        AgendamentoRecorrenteResponseDTO recorrenteResponse = new AgendamentoRecorrenteResponseDTO();
        recorrenteResponse.setId(UUID.randomUUID());
        recorrenteResponse.setAgendamentosCriados(List.of(responseDTO));
        recorrenteResponse.setDatasIgnoradas(List.of(
                new DataIgnoradaDTO(LocalDate.of(2025, 6, 9), "Conflito de horário")));

        when(recorrenteService.createRecorrente(any(AgendamentoRecorrenteRequestDTO.class)))
                .thenReturn(recorrenteResponse);

        mockMvc.perform(post("/api/v1/agendamentos/recorrente")
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.datasIgnoradas[0].motivo").value("Conflito de horário"));
    }

    @Test
    @DisplayName("Deve retornar 400 sem campos obrigatórios na recorrência")
    @WithMockUser
    void createRecorrente_SemCamposObrigatorios_400() throws Exception {
        AgendamentoRecorrenteRequestDTO requestDTO = new AgendamentoRecorrenteRequestDTO();
        // Sem campos obrigatórios

        mockMvc.perform(post("/api/v1/agendamentos/recorrente")
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve cancelar recorrência com cancelarFuturos=true - 200")
    @WithMockUser
    void cancelarRecorrencia_CancelarFuturos_200() throws Exception {
        when(recorrenteService.cancelarRecorrencia(eq(agendamentoId), eq(true)))
                .thenReturn(List.of(responseDTO));

        mockMvc.perform(delete("/api/v1/agendamentos/{id}/recorrencia", agendamentoId)
                        .with(csrf())
                        .param("cancelarFuturos", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("Deve cancelar recorrência com cancelarFuturos=false - 200")
    @WithMockUser
    void cancelarRecorrencia_SomenteUm_200() throws Exception {
        when(recorrenteService.cancelarRecorrencia(eq(agendamentoId), eq(false)))
                .thenReturn(List.of(responseDTO));

        mockMvc.perform(delete("/api/v1/agendamentos/{id}/recorrencia", agendamentoId)
                        .with(csrf())
                        .param("cancelarFuturos", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("Deve retornar 401 ao criar recorrência sem autenticação")
    void createRecorrente_Unauthenticated_401() throws Exception {
        mockMvc.perform(post("/api/v1/agendamentos/recorrente")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
