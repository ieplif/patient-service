package br.com.clinicahumaniza.patient_service.controller;

import br.com.clinicahumaniza.patient_service.dto.AuthResponseDTO;
import br.com.clinicahumaniza.patient_service.dto.LoginRequestDTO;
import br.com.clinicahumaniza.patient_service.dto.RegisterRequestDTO;
import br.com.clinicahumaniza.patient_service.exception.DuplicateResourceException;
import br.com.clinicahumaniza.patient_service.security.JwtAuthenticationFilter;
import br.com.clinicahumaniza.patient_service.security.JwtService;
import br.com.clinicahumaniza.patient_service.security.SecurityConfig;
import br.com.clinicahumaniza.patient_service.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("Deve registrar usuário com sucesso - 201")
    void registrar_Success_201() throws Exception {
        RegisterRequestDTO request = new RegisterRequestDTO("Maria Silva", "maria@email.com", "senha123");
        AuthResponseDTO response = new AuthResponseDTO("jwt-token", "Maria Silva", "maria@email.com", "ROLE_USER");

        when(authService.registrar(any(RegisterRequestDTO.class))).thenReturn(response);

        String body = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/auth/registrar")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.nome").value("Maria Silva"));
    }

    @Test
    @DisplayName("Deve fazer login com sucesso - 200")
    void login_Success_200() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO("maria@email.com", "senha123");
        AuthResponseDTO response = new AuthResponseDTO("jwt-token", "Maria Silva", "maria@email.com", "ROLE_USER");

        when(authService.login(any(LoginRequestDTO.class))).thenReturn(response);

        String body = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    @DisplayName("Deve retornar 400 com dados inválidos no registro")
    void registrar_InvalidData_400() throws Exception {
        RegisterRequestDTO invalidRequest = new RegisterRequestDTO("", "", "");

        String body = objectMapper.writeValueAsString(invalidRequest);
        mockMvc.perform(post("/api/auth/registrar")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve retornar 409 com e-mail duplicado")
    void registrar_DuplicateEmail_409() throws Exception {
        RegisterRequestDTO request = new RegisterRequestDTO("Maria Silva", "maria@email.com", "senha123");

        when(authService.registrar(any(RegisterRequestDTO.class)))
                .thenThrow(new DuplicateResourceException("E-mail", "maria@email.com"));

        String body = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/auth/registrar")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Deve retornar 401 com credenciais inválidas no login")
    void login_BadCredentials_401() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO("maria@email.com", "senhaerrada");

        when(authService.login(any(LoginRequestDTO.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        String body = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isUnauthorized());
    }
}
