package br.com.clinicahumaniza.patient_service.controller;

import br.com.clinicahumaniza.patient_service.dto.AssinaturaRequestDTO;
import br.com.clinicahumaniza.patient_service.dto.AssinaturaResponseDTO;
import br.com.clinicahumaniza.patient_service.dto.AssinaturaStatusDTO;
import br.com.clinicahumaniza.patient_service.dto.AssinaturaUpdateDTO;
import br.com.clinicahumaniza.patient_service.mapper.AssinaturaMapper;
import br.com.clinicahumaniza.patient_service.model.*;
import br.com.clinicahumaniza.patient_service.security.JwtAuthenticationFilter;
import br.com.clinicahumaniza.patient_service.security.JwtService;
import br.com.clinicahumaniza.patient_service.security.SecurityConfig;
import br.com.clinicahumaniza.patient_service.service.AssinaturaService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AssinaturaController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AssinaturaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AssinaturaService assinaturaService;

    @MockitoBean
    private AssinaturaMapper assinaturaMapper;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private ObjectMapper objectMapper;
    private Assinatura assinatura;
    private AssinaturaResponseDTO responseDTO;
    private UUID assinaturaId;
    private UUID pacienteId;
    private UUID servicoId;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        assinaturaId = UUID.randomUUID();
        pacienteId = UUID.randomUUID();
        servicoId = UUID.randomUUID();

        Atividade atividade = new Atividade();
        atividade.setId(UUID.randomUUID());
        atividade.setNome("Pilates");

        Plano plano = new Plano();
        plano.setId(UUID.randomUUID());
        plano.setNome("Mensal");

        Patient paciente = new Patient();
        paciente.setId(pacienteId);
        paciente.setNomeCompleto("João Silva");

        Servico servico = new Servico();
        servico.setId(servicoId);
        servico.setAtividade(atividade);
        servico.setPlano(plano);

        assinatura = new Assinatura();
        assinatura.setId(assinaturaId);
        assinatura.setPaciente(paciente);
        assinatura.setServico(servico);
        assinatura.setDataInicio(LocalDate.of(2025, 1, 1));
        assinatura.setDataVencimento(LocalDate.of(2025, 1, 31));
        assinatura.setSessoesContratadas(4);
        assinatura.setSessoesRealizadas(0);
        assinatura.setStatus(StatusAssinatura.ATIVO);
        assinatura.setValor(new BigDecimal("350.00"));
        assinatura.setAtivo(true);

        responseDTO = new AssinaturaResponseDTO();
        responseDTO.setId(assinaturaId);
        responseDTO.setPacienteId(pacienteId);
        responseDTO.setPacienteNome("João Silva");
        responseDTO.setServicoId(servicoId);
        responseDTO.setServicoDescricao("Pilates - Mensal");
        responseDTO.setDataInicio(LocalDate.of(2025, 1, 1));
        responseDTO.setDataVencimento(LocalDate.of(2025, 1, 31));
        responseDTO.setSessoesContratadas(4);
        responseDTO.setSessoesRealizadas(0);
        responseDTO.setSessoesRestantes(4);
        responseDTO.setStatus(StatusAssinatura.ATIVO);
        responseDTO.setValor(new BigDecimal("350.00"));
        responseDTO.setAtivo(true);
        responseDTO.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Deve criar assinatura com autenticação - 201")
    @WithMockUser
    void createAssinatura_Authenticated_201() throws Exception {
        AssinaturaRequestDTO requestDTO = new AssinaturaRequestDTO();
        requestDTO.setPacienteId(pacienteId);
        requestDTO.setServicoId(servicoId);
        requestDTO.setDataInicio(LocalDate.of(2025, 1, 1));
        requestDTO.setDataVencimento(LocalDate.of(2025, 1, 31));
        requestDTO.setSessoesContratadas(4);
        requestDTO.setValor(new BigDecimal("350.00"));

        when(assinaturaService.createAssinatura(any(AssinaturaRequestDTO.class))).thenReturn(assinatura);
        when(assinaturaMapper.toResponseDTO(assinatura)).thenReturn(responseDTO);

        mockMvc.perform(post("/api/v1/assinaturas")
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pacienteNome").value("João Silva"))
                .andExpect(jsonPath("$.servicoDescricao").value("Pilates - Mensal"));
    }

    @Test
    @DisplayName("Deve listar assinaturas com autenticação - 200")
    @WithMockUser
    void getAllAssinaturas_Authenticated_200() throws Exception {
        when(assinaturaService.getAllAssinaturas()).thenReturn(List.of(assinatura));
        when(assinaturaMapper.toResponseDTO(assinatura)).thenReturn(responseDTO);

        mockMvc.perform(get("/api/v1/assinaturas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].pacienteNome").value("João Silva"));
    }

    @Test
    @DisplayName("Deve buscar assinatura por ID com autenticação - 200")
    @WithMockUser
    void getAssinaturaById_Authenticated_200() throws Exception {
        when(assinaturaService.getAssinaturaById(assinaturaId)).thenReturn(assinatura);
        when(assinaturaMapper.toResponseDTO(assinatura)).thenReturn(responseDTO);

        mockMvc.perform(get("/api/v1/assinaturas/{id}", assinaturaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pacienteNome").value("João Silva"));
    }

    @Test
    @DisplayName("Deve listar assinaturas por paciente - 200")
    @WithMockUser
    void getAssinaturasByPaciente_200() throws Exception {
        when(assinaturaService.getAssinaturasByPaciente(pacienteId)).thenReturn(List.of(assinatura));
        when(assinaturaMapper.toResponseDTO(assinatura)).thenReturn(responseDTO);

        mockMvc.perform(get("/api/v1/assinaturas/paciente/{pacienteId}", pacienteId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].pacienteNome").value("João Silva"));
    }

    @Test
    @DisplayName("Deve listar assinaturas por serviço - 200")
    @WithMockUser
    void getAssinaturasByServico_200() throws Exception {
        when(assinaturaService.getAssinaturasByServico(servicoId)).thenReturn(List.of(assinatura));
        when(assinaturaMapper.toResponseDTO(assinatura)).thenReturn(responseDTO);

        mockMvc.perform(get("/api/v1/assinaturas/servico/{servicoId}", servicoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].servicoDescricao").value("Pilates - Mensal"));
    }

    @Test
    @DisplayName("Deve atualizar assinatura com autenticação - 200")
    @WithMockUser
    void updateAssinatura_Authenticated_200() throws Exception {
        AssinaturaUpdateDTO updateDTO = new AssinaturaUpdateDTO();
        updateDTO.setValor(new BigDecimal("400.00"));

        when(assinaturaService.updateAssinatura(any(UUID.class), any(AssinaturaUpdateDTO.class))).thenReturn(assinatura);
        when(assinaturaMapper.toResponseDTO(assinatura)).thenReturn(responseDTO);

        mockMvc.perform(put("/api/v1/assinaturas/{id}", assinaturaId)
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Deve alterar status com autenticação - 200")
    @WithMockUser
    void updateStatus_Authenticated_200() throws Exception {
        AssinaturaStatusDTO statusDTO = new AssinaturaStatusDTO(StatusAssinatura.CANCELADO);

        when(assinaturaService.updateStatus(any(UUID.class), any(AssinaturaStatusDTO.class))).thenReturn(assinatura);
        when(assinaturaMapper.toResponseDTO(assinatura)).thenReturn(responseDTO);

        mockMvc.perform(patch("/api/v1/assinaturas/{id}/status", assinaturaId)
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(statusDTO)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Deve registrar sessão com autenticação - 200")
    @WithMockUser
    void registrarSessao_Authenticated_200() throws Exception {
        when(assinaturaService.registrarSessao(assinaturaId)).thenReturn(assinatura);
        when(assinaturaMapper.toResponseDTO(assinatura)).thenReturn(responseDTO);

        mockMvc.perform(patch("/api/v1/assinaturas/{id}/registrar-sessao", assinaturaId)
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Deve deletar assinatura com autenticação - 204")
    @WithMockUser
    void deleteAssinatura_Authenticated_204() throws Exception {
        doNothing().when(assinaturaService).deleteAssinatura(assinaturaId);

        mockMvc.perform(delete("/api/v1/assinaturas/{id}", assinaturaId)
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Deve retornar 401 sem autenticação")
    void getAllAssinaturas_Unauthenticated_401() throws Exception {
        mockMvc.perform(get("/api/v1/assinaturas"))
                .andExpect(status().isUnauthorized());
    }
}
