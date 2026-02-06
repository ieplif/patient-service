package br.com.clinicahumaniza.patient_service.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", "dGVzdHNlY3JldGtleWZvcmp3dHRva2VudmFsaWRhdGlvbjEyMzQ1Njc4OQ==");
        ReflectionTestUtils.setField(jwtService, "expiration", 86400000L);

        userDetails = new User("maria@email.com", "password", Collections.emptyList());
    }

    @Test
    @DisplayName("Deve gerar token JWT")
    void generateToken_Success() {
        String token = jwtService.generateToken(userDetails);

        assertThat(token).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("Deve extrair e-mail do token")
    void extractEmail_Success() {
        String token = jwtService.generateToken(userDetails);

        String email = jwtService.extractEmail(token);

        assertThat(email).isEqualTo("maria@email.com");
    }

    @Test
    @DisplayName("Deve validar token com sucesso")
    void isTokenValid_Success() {
        String token = jwtService.generateToken(userDetails);

        boolean valid = jwtService.isTokenValid(token, userDetails);

        assertThat(valid).isTrue();
    }

    @Test
    @DisplayName("Deve invalidar token quando usuário é diferente")
    void isTokenValid_WrongUser() {
        String token = jwtService.generateToken(userDetails);
        UserDetails otherUser = new User("outro@email.com", "password", Collections.emptyList());

        boolean valid = jwtService.isTokenValid(token, otherUser);

        assertThat(valid).isFalse();
    }

    @Test
    @DisplayName("Deve invalidar token expirado")
    void isTokenValid_Expired() {
        ReflectionTestUtils.setField(jwtService, "expiration", -1000L);

        String token = jwtService.generateToken(userDetails);

        assertThatThrownBy(() -> jwtService.isTokenValid(token, userDetails))
                .isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
    }
}
