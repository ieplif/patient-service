package br.com.clinicahumaniza.patient_service.integration;

import br.com.clinicahumaniza.patient_service.dto.AtividadeRequestDTO;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AtividadeIntegrationTest {

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
    @DisplayName("Fluxo completo: autenticar → CRUD de atividades")
    void fullAtividadeCrudFlow() throws Exception {
        AtividadeRequestDTO requestDTO = new AtividadeRequestDTO();
        requestDTO.setNome("Pilates");
        requestDTO.setDescricao("Método de exercícios físicos");
        requestDTO.setDuracaoPadrao(50);

        // Criar atividade
        MvcResult createResult = mockMvc.perform(post("/api/v1/atividades")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Pilates"))
                .andReturn();

        String atividadeId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        // Listar atividades
        mockMvc.perform(get("/api/v1/atividades")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Pilates"));

        // Buscar por ID
        mockMvc.perform(get("/api/v1/atividades/{id}", atividadeId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Pilates"));

        // Deletar (soft delete)
        mockMvc.perform(delete("/api/v1/atividades/{id}", atividadeId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Deve retornar 401 sem token")
    void accessWithoutToken_401() throws Exception {
        mockMvc.perform(get("/api/v1/atividades"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Deve retornar 409 ao criar atividade com nome duplicado")
    void createAtividade_DuplicateName_409() throws Exception {
        AtividadeRequestDTO requestDTO = new AtividadeRequestDTO();
        requestDTO.setNome("Pilates");
        requestDTO.setDescricao("Método de exercícios físicos");
        requestDTO.setDuracaoPadrao(50);

        String body = objectMapper.writeValueAsString(requestDTO);

        // Criar primeira vez
        mockMvc.perform(post("/api/v1/atividades")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated());

        // Criar segunda vez com mesmo nome
        mockMvc.perform(post("/api/v1/atividades")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isConflict());
    }
}
