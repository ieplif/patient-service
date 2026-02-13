package br.com.clinicahumaniza.patient_service.controller;

import br.com.clinicahumaniza.patient_service.dto.*;
import br.com.clinicahumaniza.patient_service.exception.ResourceNotFoundException;
import br.com.clinicahumaniza.patient_service.mapper.PagamentoMapper;
import br.com.clinicahumaniza.patient_service.model.*;
import br.com.clinicahumaniza.patient_service.security.JwtAuthenticationFilter;
import br.com.clinicahumaniza.patient_service.security.JwtService;
import br.com.clinicahumaniza.patient_service.security.SecurityConfig;
import br.com.clinicahumaniza.patient_service.service.PagamentoService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PagamentoController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class PagamentoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PagamentoService pagamentoService;

    @MockitoBean
    private PagamentoMapper pagamentoMapper;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private ObjectMapper objectMapper;
    private Pagamento pagamento;
    private PagamentoResponseDTO responseDTO;
    private UUID pagamentoId;
    private UUID pacienteId;
    private UUID assinaturaId;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        pagamentoId = UUID.randomUUID();
        pacienteId = UUID.randomUUID();
        assinaturaId = UUID.randomUUID();

        Patient paciente = new Patient();
        paciente.setId(pacienteId);
        paciente.setNomeCompleto("João Silva");

        pagamento = new Pagamento();
        pagamento.setId(pagamentoId);
        pagamento.setPaciente(paciente);
        pagamento.setValor(new BigDecimal("300.00"));
        pagamento.setFormaPagamento(FormaPagamento.PIX);
        pagamento.setStatus(StatusPagamento.PENDENTE);
        pagamento.setNumeroParcelas(1);
        pagamento.setDataVencimento(LocalDate.of(2025, 2, 15));
        pagamento.setAtivo(true);

        ParcelaResponseDTO parcelaDTO = new ParcelaResponseDTO();
        parcelaDTO.setId(UUID.randomUUID());
        parcelaDTO.setNumero(1);
        parcelaDTO.setValor(new BigDecimal("300.00"));
        parcelaDTO.setDataVencimento(LocalDate.of(2025, 2, 15));
        parcelaDTO.setStatus(StatusParcela.PENDENTE);

        responseDTO = new PagamentoResponseDTO();
        responseDTO.setId(pagamentoId);
        responseDTO.setPacienteId(pacienteId);
        responseDTO.setPacienteNome("João Silva");
        responseDTO.setAssinaturaId(assinaturaId);
        responseDTO.setAssinaturaDescricao("Pilates - Mensal");
        responseDTO.setValor(new BigDecimal("300.00"));
        responseDTO.setFormaPagamento(FormaPagamento.PIX);
        responseDTO.setStatus(StatusPagamento.PENDENTE);
        responseDTO.setNumeroParcelas(1);
        responseDTO.setDataVencimento(LocalDate.of(2025, 2, 15));
        responseDTO.setAtivo(true);
        responseDTO.setCreatedAt(LocalDateTime.now());
        responseDTO.setParcelas(List.of(parcelaDTO));
    }

    @Test
    @DisplayName("Deve criar pagamento com autenticação - 201")
    @WithMockUser
    void createPagamento_Authenticated_201() throws Exception {
        PagamentoRequestDTO requestDTO = new PagamentoRequestDTO();
        requestDTO.setPacienteId(pacienteId);
        requestDTO.setAssinaturaId(assinaturaId);
        requestDTO.setValor(new BigDecimal("300.00"));
        requestDTO.setFormaPagamento(FormaPagamento.PIX);
        requestDTO.setNumeroParcelas(1);
        requestDTO.setDataVencimento(LocalDate.of(2025, 2, 15));

        when(pagamentoService.createPagamento(any(PagamentoRequestDTO.class))).thenReturn(pagamento);
        when(pagamentoMapper.toResponseDTO(pagamento)).thenReturn(responseDTO);

        mockMvc.perform(post("/api/v1/pagamentos")
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pacienteNome").value("João Silva"))
                .andExpect(jsonPath("$.formaPagamento").value("PIX"))
                .andExpect(jsonPath("$.parcelas").isArray());
    }

    @Test
    @DisplayName("Deve rejeitar criação sem campos obrigatórios - 400")
    @WithMockUser
    void createPagamento_ValidationError_400() throws Exception {
        PagamentoRequestDTO requestDTO = new PagamentoRequestDTO();
        // Sem campos obrigatórios

        mockMvc.perform(post("/api/v1/pagamentos")
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve listar pagamentos com autenticação - 200")
    @WithMockUser
    void getAllPagamentos_Authenticated_200() throws Exception {
        when(pagamentoService.getAllPagamentos()).thenReturn(List.of(pagamento));
        when(pagamentoMapper.toResponseDTO(pagamento)).thenReturn(responseDTO);

        mockMvc.perform(get("/api/v1/pagamentos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].pacienteNome").value("João Silva"));
    }

    @Test
    @DisplayName("Deve buscar pagamento por ID com autenticação - 200")
    @WithMockUser
    void getPagamentoById_Authenticated_200() throws Exception {
        when(pagamentoService.getPagamentoById(pagamentoId)).thenReturn(pagamento);
        when(pagamentoMapper.toResponseDTO(pagamento)).thenReturn(responseDTO);

        mockMvc.perform(get("/api/v1/pagamentos/{id}", pagamentoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pacienteNome").value("João Silva"));
    }

    @Test
    @DisplayName("Deve retornar 404 quando pagamento não encontrado")
    @WithMockUser
    void getPagamentoById_NotFound_404() throws Exception {
        when(pagamentoService.getPagamentoById(pagamentoId))
                .thenThrow(new ResourceNotFoundException("Pagamento", pagamentoId));

        mockMvc.perform(get("/api/v1/pagamentos/{id}", pagamentoId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deve listar pagamentos por paciente - 200")
    @WithMockUser
    void getPagamentosByPaciente_200() throws Exception {
        when(pagamentoService.getPagamentosByPaciente(pacienteId)).thenReturn(List.of(pagamento));
        when(pagamentoMapper.toResponseDTO(pagamento)).thenReturn(responseDTO);

        mockMvc.perform(get("/api/v1/pagamentos/paciente/{pacienteId}", pacienteId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].pacienteNome").value("João Silva"));
    }

    @Test
    @DisplayName("Deve listar pagamentos por assinatura - 200")
    @WithMockUser
    void getPagamentosByAssinatura_200() throws Exception {
        when(pagamentoService.getPagamentosByAssinatura(assinaturaId)).thenReturn(List.of(pagamento));
        when(pagamentoMapper.toResponseDTO(pagamento)).thenReturn(responseDTO);

        mockMvc.perform(get("/api/v1/pagamentos/assinatura/{assinaturaId}", assinaturaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].assinaturaDescricao").value("Pilates - Mensal"));
    }

    @Test
    @DisplayName("Deve listar pagamentos por período - 200")
    @WithMockUser
    void getPagamentosByPeriodo_200() throws Exception {
        when(pagamentoService.getPagamentosByPeriodo(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(pagamento));
        when(pagamentoMapper.toResponseDTO(pagamento)).thenReturn(responseDTO);

        mockMvc.perform(get("/api/v1/pagamentos/periodo")
                        .param("inicio", "2025-02-01")
                        .param("fim", "2025-02-28"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].valor").value(300.00));
    }

    @Test
    @DisplayName("Deve atualizar pagamento com autenticação - 200")
    @WithMockUser
    void updatePagamento_Authenticated_200() throws Exception {
        PagamentoUpdateDTO updateDTO = new PagamentoUpdateDTO();
        updateDTO.setFormaPagamento(FormaPagamento.CARTAO_CREDITO);

        when(pagamentoService.updatePagamento(any(UUID.class), any(PagamentoUpdateDTO.class))).thenReturn(pagamento);
        when(pagamentoMapper.toResponseDTO(pagamento)).thenReturn(responseDTO);

        mockMvc.perform(put("/api/v1/pagamentos/{id}", pagamentoId)
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Deve alterar status do pagamento - 200")
    @WithMockUser
    void updateStatus_Authenticated_200() throws Exception {
        PagamentoStatusDTO statusDTO = new PagamentoStatusDTO(StatusPagamento.PAGO);

        when(pagamentoService.updateStatus(any(UUID.class), any(PagamentoStatusDTO.class))).thenReturn(pagamento);
        when(pagamentoMapper.toResponseDTO(pagamento)).thenReturn(responseDTO);

        mockMvc.perform(patch("/api/v1/pagamentos/{id}/status", pagamentoId)
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(statusDTO)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Deve alterar status da parcela - 200")
    @WithMockUser
    void updateParcelaStatus_Authenticated_200() throws Exception {
        UUID parcelaId = UUID.randomUUID();
        ParcelaStatusDTO parcelaStatusDTO = new ParcelaStatusDTO(StatusParcela.PAGO, null);

        when(pagamentoService.updateParcelaStatus(any(UUID.class), any(UUID.class), any(ParcelaStatusDTO.class)))
                .thenReturn(pagamento);
        when(pagamentoMapper.toResponseDTO(pagamento)).thenReturn(responseDTO);

        mockMvc.perform(patch("/api/v1/pagamentos/{id}/parcelas/{parcelaId}/status", pagamentoId, parcelaId)
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(parcelaStatusDTO)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Deve deletar pagamento com autenticação - 204")
    @WithMockUser
    void deletePagamento_Authenticated_204() throws Exception {
        doNothing().when(pagamentoService).deletePagamento(pagamentoId);

        mockMvc.perform(delete("/api/v1/pagamentos/{id}", pagamentoId)
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Deve retornar 401 sem autenticação")
    void getAllPagamentos_Unauthenticated_401() throws Exception {
        mockMvc.perform(get("/api/v1/pagamentos"))
                .andExpect(status().isUnauthorized());
    }
}
