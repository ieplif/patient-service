package br.com.clinicahumaniza.patient_service.integration;

import br.com.clinicahumaniza.patient_service.dto.LoginRequestDTO;
import br.com.clinicahumaniza.patient_service.dto.RegisterRequestDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

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

    @Test
    @DisplayName("Fluxo completo: registrar → login → obter token")
    void fullAuthFlow() throws Exception {
        RegisterRequestDTO registerRequest = new RegisterRequestDTO("Ana Costa", "ana@email.com", "senha123");

        // Registrar
        String registerBody = objectMapper.writeValueAsString(registerRequest);
        mockMvc.perform(post("/api/auth/registrar")
                        .contentType("application/json")
                        .content(registerBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.nome").value("Ana Costa"))
                .andExpect(jsonPath("$.email").value("ana@email.com"))
                .andExpect(jsonPath("$.tipo").value("Bearer"));

        // Login
        LoginRequestDTO loginRequest = new LoginRequestDTO("ana@email.com", "senha123");

        String loginBody = objectMapper.writeValueAsString(loginRequest);
        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.nome").value("Ana Costa"));
    }

    @Test
    @DisplayName("Deve rejeitar registro com e-mail duplicado")
    void register_DuplicateEmail() throws Exception {
        RegisterRequestDTO request = new RegisterRequestDTO("Ana Costa", "duplicado@email.com", "senha123");

        String body = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/auth/registrar")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated());

        // Tentar registrar com o mesmo e-mail
        mockMvc.perform(post("/api/auth/registrar")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Deve rejeitar login com senha incorreta")
    void login_WrongPassword() throws Exception {
        RegisterRequestDTO registerRequest = new RegisterRequestDTO("Ana Costa", "ana2@email.com", "senha123");

        String registerBody = objectMapper.writeValueAsString(registerRequest);
        mockMvc.perform(post("/api/auth/registrar")
                        .contentType("application/json")
                        .content(registerBody))
                .andExpect(status().isCreated());

        LoginRequestDTO loginRequest = new LoginRequestDTO("ana2@email.com", "senhaerrada");

        String loginBody = objectMapper.writeValueAsString(loginRequest);
        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(loginBody))
                .andExpect(status().isUnauthorized());
    }
}
