package br.com.clinicahumaniza.patient_service.integration;

import br.com.clinicahumaniza.patient_service.dto.LoginRequestDTO;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        LoginRequestDTO loginRequest = new LoginRequestDTO("admin@test.com", "senha123");
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        adminToken = objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    @Test
    @DisplayName("Admin registra novo usuário → novo usuário faz login → obtém token")
    void fullAuthFlow() throws Exception {
        RegisterRequestDTO registerRequest = new RegisterRequestDTO("Ana Costa", "ana@email.com", "senha123");

        mockMvc.perform(post("/api/auth/registrar")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.nome").value("Ana Costa"))
                .andExpect(jsonPath("$.email").value("ana@email.com"))
                .andExpect(jsonPath("$.tipo").value("Bearer"));

        LoginRequestDTO loginRequest = new LoginRequestDTO("ana@email.com", "senha123");
        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.nome").value("Ana Costa"));
    }

    @Test
    @DisplayName("Admin tenta registrar e-mail duplicado → 409 Conflict")
    void register_DuplicateEmail() throws Exception {
        RegisterRequestDTO request = new RegisterRequestDTO("Ana Costa", "duplicado@email.com", "senha123");
        String body = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/auth/registrar")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/registrar")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Tentativa de registro sem autenticação → 401 Unauthorized")
    void register_WithoutAuth_401() throws Exception {
        RegisterRequestDTO request = new RegisterRequestDTO("Sem Auth", "semauth@email.com", "senha123");
        mockMvc.perform(post("/api/auth/registrar")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Login com senha incorreta → 401 Unauthorized")
    void login_WrongPassword() throws Exception {
        RegisterRequestDTO registerRequest = new RegisterRequestDTO("Ana Costa", "ana2@email.com", "senha123");
        mockMvc.perform(post("/api/auth/registrar")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        LoginRequestDTO loginRequest = new LoginRequestDTO("ana2@email.com", "senhaerrada");
        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }
}
