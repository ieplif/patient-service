package br.com.clinicahumaniza.patient_service.integration;

import br.com.clinicahumaniza.patient_service.dto.AtividadeRequestDTO;
import br.com.clinicahumaniza.patient_service.dto.ProfissionalRequestDTO;
import br.com.clinicahumaniza.patient_service.dto.ProfissionalUpdateDTO;
import br.com.clinicahumaniza.patient_service.dto.RegisterRequestDTO;
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

import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ProfissionalIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        RegisterRequestDTO registerRequest = new RegisterRequestDTO("Admin", "admin@email.com", "senha123");

        MvcResult result = mockMvc.perform(post("/api/auth/registrar")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        adminToken = objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    @Test
    @DisplayName("Fluxo completo: criar atividade → criar profissional → listar por atividade → atualizar → verificar")
    void fullProfissionalCrudFlow() throws Exception {
        // 1. Criar atividade
        AtividadeRequestDTO atividadeDTO = new AtividadeRequestDTO();
        atividadeDTO.setNome("Fisioterapia Pélvica");
        atividadeDTO.setDescricao("Tratamento especializado");
        atividadeDTO.setDuracaoPadrao(50);

        MvcResult atividadeResult = mockMvc.perform(post("/api/v1/atividades")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(atividadeDTO)))
                .andExpect(status().isCreated())
                .andReturn();

        String atividadeId = objectMapper.readTree(atividadeResult.getResponse().getContentAsString()).get("id").asText();

        // 2. Criar segunda atividade
        AtividadeRequestDTO atividade2DTO = new AtividadeRequestDTO();
        atividade2DTO.setNome("Drenagem Linfática");
        atividade2DTO.setDescricao("Drenagem especializada");
        atividade2DTO.setDuracaoPadrao(60);

        MvcResult atividade2Result = mockMvc.perform(post("/api/v1/atividades")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(atividade2DTO)))
                .andExpect(status().isCreated())
                .andReturn();

        String atividade2Id = objectMapper.readTree(atividade2Result.getResponse().getContentAsString()).get("id").asText();

        // 3. Criar profissional
        ProfissionalRequestDTO profDTO = new ProfissionalRequestDTO();
        profDTO.setNome("Maria Silva");
        profDTO.setTelefone("11999999999");
        profDTO.setEmail("maria@email.com");
        profDTO.setSenha("senha123");
        profDTO.setAtividadeIds(Set.of(UUID.fromString(atividadeId)));

        MvcResult profResult = mockMvc.perform(post("/api/v1/profissionais")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(profDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Maria Silva"))
                .andExpect(jsonPath("$.email").value("maria@email.com"))
                .andExpect(jsonPath("$.atividades[0].nome").value("Fisioterapia Pélvica"))
                .andReturn();

        String profissionalId = objectMapper.readTree(profResult.getResponse().getContentAsString()).get("id").asText();

        // 4. Listar profissionais por atividade
        mockMvc.perform(get("/api/v1/profissionais/atividade/{atividadeId}", atividadeId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Maria Silva"));

        // 5. Atualizar profissional - mudar atividades
        ProfissionalUpdateDTO updateDTO = new ProfissionalUpdateDTO();
        updateDTO.setNome("Maria Silva Santos");
        updateDTO.setAtividadeIds(Set.of(UUID.fromString(atividadeId), UUID.fromString(atividade2Id)));

        mockMvc.perform(put("/api/v1/profissionais/{id}", profissionalId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Maria Silva Santos"))
                .andExpect(jsonPath("$.atividades.length()").value(2));

        // 6. Buscar por ID
        mockMvc.perform(get("/api/v1/profissionais/{id}", profissionalId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Maria Silva Santos"));

        // 7. Deletar (soft delete)
        mockMvc.perform(delete("/api/v1/profissionais/{id}", profissionalId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Profissional faz login com email/senha próprios e recebe token")
    void profissionalLogin() throws Exception {
        // Criar atividade
        AtividadeRequestDTO atividadeDTO = new AtividadeRequestDTO();
        atividadeDTO.setNome("Pilates");
        atividadeDTO.setDescricao("Pilates clínico");
        atividadeDTO.setDuracaoPadrao(50);

        MvcResult atividadeResult = mockMvc.perform(post("/api/v1/atividades")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(atividadeDTO)))
                .andExpect(status().isCreated())
                .andReturn();

        String atividadeId = objectMapper.readTree(atividadeResult.getResponse().getContentAsString()).get("id").asText();

        // Criar profissional
        ProfissionalRequestDTO profDTO = new ProfissionalRequestDTO();
        profDTO.setNome("Ana Souza");
        profDTO.setTelefone("11888888888");
        profDTO.setEmail("ana@email.com");
        profDTO.setSenha("senha123");
        profDTO.setAtividadeIds(Set.of(UUID.fromString(atividadeId)));

        mockMvc.perform(post("/api/v1/profissionais")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(profDTO)))
                .andExpect(status().isCreated());

        // Login com credenciais do profissional
        String loginBody = objectMapper.writeValueAsString(
                new java.util.LinkedHashMap<>() {{
                    put("email", "ana@email.com");
                    put("senha", "senha123");
                }}
        );

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.role").value("ROLE_PROFISSIONAL"));
    }

    @Test
    @DisplayName("Deve retornar 401 sem token")
    void accessWithoutToken_401() throws Exception {
        mockMvc.perform(get("/api/v1/profissionais"))
                .andExpect(status().isUnauthorized());
    }
}
