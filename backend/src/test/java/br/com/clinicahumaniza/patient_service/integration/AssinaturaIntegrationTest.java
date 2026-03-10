package br.com.clinicahumaniza.patient_service.integration;

import br.com.clinicahumaniza.patient_service.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AssinaturaIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        RegisterRequestDTO registerRequest = new RegisterRequestDTO("Admin", "admin@email.com", "senha123");

        String body = objectMapper.writeValueAsString(registerRequest);
        MvcResult result = mockMvc.perform(post("/api/auth/registrar")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        token = objectMapper.readTree(responseBody).get("token").asText();
    }

    @Test
    @DisplayName("Fluxo completo: criar paciente → criar atividade → criar plano → criar serviço → criar assinatura → registrar sessões → verificar finalização")
    void fullAssinaturaFlow() throws Exception {
        // Criar paciente
        PatientRequestDTO pacienteDTO = new PatientRequestDTO();
        pacienteDTO.setNomeCompleto("Maria Santos");
        pacienteDTO.setEmail("maria@email.com");
        pacienteDTO.setCpf("12345678901");
        pacienteDTO.setDataNascimento(LocalDate.of(1990, 5, 15));
        pacienteDTO.setTelefone("11999990000");

        MvcResult pacienteResult = mockMvc.perform(post("/api/v1/patients")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(pacienteDTO)))
                .andExpect(status().isCreated())
                .andReturn();

        String pacienteId = objectMapper.readTree(pacienteResult.getResponse().getContentAsString()).get("id").asText();

        // Criar atividade
        AtividadeRequestDTO atividadeDTO = new AtividadeRequestDTO();
        atividadeDTO.setNome("Pilates");
        atividadeDTO.setDescricao("Método de exercícios físicos");
        atividadeDTO.setDuracaoPadrao(50);

        MvcResult atividadeResult = mockMvc.perform(post("/api/v1/atividades")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(atividadeDTO)))
                .andExpect(status().isCreated())
                .andReturn();

        String atividadeId = objectMapper.readTree(atividadeResult.getResponse().getContentAsString()).get("id").asText();

        // Criar plano
        PlanoRequestDTO planoDTO = new PlanoRequestDTO();
        planoDTO.setNome("Mensal");
        planoDTO.setDescricao("Plano mensal");
        planoDTO.setTipoPlano("mensal");
        planoDTO.setValidadeDias(30);
        planoDTO.setSessoesIncluidas(2);

        MvcResult planoResult = mockMvc.perform(post("/api/v1/planos")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(planoDTO)))
                .andExpect(status().isCreated())
                .andReturn();

        String planoId = objectMapper.readTree(planoResult.getResponse().getContentAsString()).get("id").asText();

        // Criar serviço
        ServicoRequestDTO servicoDTO = new ServicoRequestDTO();
        servicoDTO.setAtividadeId(UUID.fromString(atividadeId));
        servicoDTO.setPlanoId(UUID.fromString(planoId));
        servicoDTO.setTipoAtendimento("individual");
        servicoDTO.setQuantidade(2);
        servicoDTO.setUnidadeServico("sessao");
        servicoDTO.setModalidadeLocal("clinica");
        servicoDTO.setValor(new BigDecimal("350.00"));

        MvcResult servicoResult = mockMvc.perform(post("/api/v1/servicos")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(servicoDTO)))
                .andExpect(status().isCreated())
                .andReturn();

        String servicoId = objectMapper.readTree(servicoResult.getResponse().getContentAsString()).get("id").asText();

        // Criar assinatura
        AssinaturaRequestDTO assinaturaDTO = new AssinaturaRequestDTO();
        assinaturaDTO.setPacienteId(UUID.fromString(pacienteId));
        assinaturaDTO.setServicoId(UUID.fromString(servicoId));
        assinaturaDTO.setDataInicio(LocalDate.of(2025, 1, 1));
        assinaturaDTO.setSessoesContratadas(2);
        assinaturaDTO.setValor(new BigDecimal("350.00"));

        MvcResult assinaturaResult = mockMvc.perform(post("/api/v1/assinaturas")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(assinaturaDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pacienteNome").value("Maria Santos"))
                .andExpect(jsonPath("$.servicoDescricao").value("Pilates - Mensal"))
                .andExpect(jsonPath("$.sessoesContratadas").value(2))
                .andExpect(jsonPath("$.sessoesRealizadas").value(0))
                .andExpect(jsonPath("$.sessoesRestantes").value(2))
                .andExpect(jsonPath("$.status").value("ATIVO"))
                .andExpect(jsonPath("$.dataVencimento").value("2025-01-31"))
                .andReturn();

        String assinaturaId = objectMapper.readTree(assinaturaResult.getResponse().getContentAsString()).get("id").asText();

        // Registrar sessão 1
        mockMvc.perform(patch("/api/v1/assinaturas/{id}/registrar-sessao", assinaturaId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessoesRealizadas").value(1))
                .andExpect(jsonPath("$.sessoesRestantes").value(1))
                .andExpect(jsonPath("$.status").value("ATIVO"));

        // Registrar sessão 2 — deve finalizar automaticamente
        mockMvc.perform(patch("/api/v1/assinaturas/{id}/registrar-sessao", assinaturaId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessoesRealizadas").value(2))
                .andExpect(jsonPath("$.sessoesRestantes").value(0))
                .andExpect(jsonPath("$.status").value("FINALIZADO"));

        // Listar assinaturas por paciente
        mockMvc.perform(get("/api/v1/assinaturas/paciente/{pacienteId}", pacienteId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].pacienteNome").value("Maria Santos"));
    }

    @Test
    @DisplayName("Deve retornar 401 sem token")
    void accessWithoutToken_401() throws Exception {
        mockMvc.perform(get("/api/v1/assinaturas"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Deve retornar 404 ao criar assinatura com paciente inexistente")
    void createAssinatura_PacienteNotFound_404() throws Exception {
        // Criar atividade
        AtividadeRequestDTO atividadeDTO = new AtividadeRequestDTO();
        atividadeDTO.setNome("Yoga");
        atividadeDTO.setDescricao("Prática de yoga");
        atividadeDTO.setDuracaoPadrao(60);

        MvcResult atividadeResult = mockMvc.perform(post("/api/v1/atividades")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(atividadeDTO)))
                .andExpect(status().isCreated())
                .andReturn();

        String atividadeId = objectMapper.readTree(atividadeResult.getResponse().getContentAsString()).get("id").asText();

        // Criar plano
        PlanoRequestDTO planoDTO = new PlanoRequestDTO();
        planoDTO.setNome("Trimestral");
        planoDTO.setDescricao("Plano trimestral");
        planoDTO.setTipoPlano("trimestral");
        planoDTO.setValidadeDias(90);

        MvcResult planoResult = mockMvc.perform(post("/api/v1/planos")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(planoDTO)))
                .andExpect(status().isCreated())
                .andReturn();

        String planoId = objectMapper.readTree(planoResult.getResponse().getContentAsString()).get("id").asText();

        // Criar serviço
        ServicoRequestDTO servicoDTO = new ServicoRequestDTO();
        servicoDTO.setAtividadeId(UUID.fromString(atividadeId));
        servicoDTO.setPlanoId(UUID.fromString(planoId));
        servicoDTO.setValor(new BigDecimal("500.00"));

        MvcResult servicoResult = mockMvc.perform(post("/api/v1/servicos")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(servicoDTO)))
                .andExpect(status().isCreated())
                .andReturn();

        String servicoId = objectMapper.readTree(servicoResult.getResponse().getContentAsString()).get("id").asText();

        // Criar assinatura com paciente inexistente
        AssinaturaRequestDTO assinaturaDTO = new AssinaturaRequestDTO();
        assinaturaDTO.setPacienteId(UUID.randomUUID());
        assinaturaDTO.setServicoId(UUID.fromString(servicoId));
        assinaturaDTO.setDataInicio(LocalDate.of(2025, 1, 1));
        assinaturaDTO.setSessoesContratadas(4);
        assinaturaDTO.setValor(new BigDecimal("500.00"));

        mockMvc.perform(post("/api/v1/assinaturas")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(assinaturaDTO)))
                .andExpect(status().isNotFound());
    }
}
