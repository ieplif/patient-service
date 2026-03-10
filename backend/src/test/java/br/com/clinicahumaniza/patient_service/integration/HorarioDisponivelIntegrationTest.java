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

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class HorarioDisponivelIntegrationTest {

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
    @DisplayName("Fluxo completo: criar atividade → criar profissional → adicionar disponibilidade → listar por profissional")
    void fullHorarioDisponivelFlow() throws Exception {
        // Criar atividade
        AtividadeRequestDTO atividadeDTO = new AtividadeRequestDTO();
        atividadeDTO.setNome("Pilates");
        atividadeDTO.setDescricao("Exercícios de Pilates");
        atividadeDTO.setDuracaoPadrao(50);

        MvcResult atividadeResult = mockMvc.perform(post("/api/v1/atividades")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(atividadeDTO)))
                .andExpect(status().isCreated())
                .andReturn();

        String atividadeId = objectMapper.readTree(atividadeResult.getResponse().getContentAsString()).get("id").asText();

        // Criar profissional
        ProfissionalRequestDTO profissionalDTO = new ProfissionalRequestDTO();
        profissionalDTO.setNome("Dr. Ana");
        profissionalDTO.setTelefone("11999990000");
        profissionalDTO.setEmail("ana@email.com");
        profissionalDTO.setSenha("senha123");
        profissionalDTO.setAtividadeIds(Set.of(UUID.fromString(atividadeId)));

        MvcResult profissionalResult = mockMvc.perform(post("/api/v1/profissionais")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(profissionalDTO)))
                .andExpect(status().isCreated())
                .andReturn();

        String profissionalId = objectMapper.readTree(profissionalResult.getResponse().getContentAsString()).get("id").asText();

        // Criar horário disponível - Segunda 08:00-12:00
        HorarioDisponivelRequestDTO horarioDTO = new HorarioDisponivelRequestDTO();
        horarioDTO.setProfissionalId(UUID.fromString(profissionalId));
        horarioDTO.setDiaSemana(DayOfWeek.MONDAY);
        horarioDTO.setHoraInicio(LocalTime.of(8, 0));
        horarioDTO.setHoraFim(LocalTime.of(12, 0));

        mockMvc.perform(post("/api/v1/disponibilidades")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(horarioDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.profissionalNome").value("Dr. Ana"))
                .andExpect(jsonPath("$.diaSemana").value("MONDAY"))
                .andExpect(jsonPath("$.horaInicio").value("08:00:00"))
                .andExpect(jsonPath("$.horaFim").value("12:00:00"));

        // Criar horário disponível - Segunda 14:00-18:00
        HorarioDisponivelRequestDTO horarioDTO2 = new HorarioDisponivelRequestDTO();
        horarioDTO2.setProfissionalId(UUID.fromString(profissionalId));
        horarioDTO2.setDiaSemana(DayOfWeek.MONDAY);
        horarioDTO2.setHoraInicio(LocalTime.of(14, 0));
        horarioDTO2.setHoraFim(LocalTime.of(18, 0));

        mockMvc.perform(post("/api/v1/disponibilidades")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(horarioDTO2)))
                .andExpect(status().isCreated());

        // Listar por profissional
        mockMvc.perform(get("/api/v1/disponibilidades/profissional/{profissionalId}", profissionalId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("Deve retornar 401 sem token")
    void accessWithoutToken_401() throws Exception {
        mockMvc.perform(get("/api/v1/disponibilidades"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Deve retornar 422 quando hora início >= hora fim")
    void createHorario_InvalidHours_422() throws Exception {
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

        // Criar profissional
        ProfissionalRequestDTO profissionalDTO = new ProfissionalRequestDTO();
        profissionalDTO.setNome("Dr. Carlos");
        profissionalDTO.setTelefone("11888880000");
        profissionalDTO.setEmail("carlos@email.com");
        profissionalDTO.setSenha("senha123");
        profissionalDTO.setAtividadeIds(Set.of(UUID.fromString(atividadeId)));

        MvcResult profissionalResult = mockMvc.perform(post("/api/v1/profissionais")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(profissionalDTO)))
                .andExpect(status().isCreated())
                .andReturn();

        String profissionalId = objectMapper.readTree(profissionalResult.getResponse().getContentAsString()).get("id").asText();

        // Tentar criar horário com hora início > hora fim
        HorarioDisponivelRequestDTO horarioDTO = new HorarioDisponivelRequestDTO();
        horarioDTO.setProfissionalId(UUID.fromString(profissionalId));
        horarioDTO.setDiaSemana(DayOfWeek.TUESDAY);
        horarioDTO.setHoraInicio(LocalTime.of(18, 0));
        horarioDTO.setHoraFim(LocalTime.of(8, 0));

        mockMvc.perform(post("/api/v1/disponibilidades")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(horarioDTO)))
                .andExpect(status().isUnprocessableEntity());
    }
}
