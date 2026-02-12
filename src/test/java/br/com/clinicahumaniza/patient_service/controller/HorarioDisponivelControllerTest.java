package br.com.clinicahumaniza.patient_service.controller;

import br.com.clinicahumaniza.patient_service.dto.HorarioDisponivelRequestDTO;
import br.com.clinicahumaniza.patient_service.dto.HorarioDisponivelResponseDTO;
import br.com.clinicahumaniza.patient_service.dto.HorarioDisponivelUpdateDTO;
import br.com.clinicahumaniza.patient_service.mapper.HorarioDisponivelMapper;
import br.com.clinicahumaniza.patient_service.model.HorarioDisponivel;
import br.com.clinicahumaniza.patient_service.model.Profissional;
import br.com.clinicahumaniza.patient_service.security.JwtAuthenticationFilter;
import br.com.clinicahumaniza.patient_service.security.JwtService;
import br.com.clinicahumaniza.patient_service.security.SecurityConfig;
import br.com.clinicahumaniza.patient_service.service.HorarioDisponivelService;
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

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HorarioDisponivelController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class HorarioDisponivelControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HorarioDisponivelService horarioDisponivelService;

    @MockitoBean
    private HorarioDisponivelMapper horarioDisponivelMapper;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private ObjectMapper objectMapper;
    private HorarioDisponivel horario;
    private HorarioDisponivelResponseDTO responseDTO;
    private UUID horarioId;
    private UUID profissionalId;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        horarioId = UUID.randomUUID();
        profissionalId = UUID.randomUUID();

        Profissional profissional = new Profissional();
        profissional.setId(profissionalId);
        profissional.setNome("Dr. Ana");

        horario = new HorarioDisponivel();
        horario.setId(horarioId);
        horario.setProfissional(profissional);
        horario.setDiaSemana(DayOfWeek.MONDAY);
        horario.setHoraInicio(LocalTime.of(8, 0));
        horario.setHoraFim(LocalTime.of(12, 0));
        horario.setAtivo(true);

        responseDTO = new HorarioDisponivelResponseDTO();
        responseDTO.setId(horarioId);
        responseDTO.setProfissionalId(profissionalId);
        responseDTO.setProfissionalNome("Dr. Ana");
        responseDTO.setDiaSemana(DayOfWeek.MONDAY);
        responseDTO.setHoraInicio(LocalTime.of(8, 0));
        responseDTO.setHoraFim(LocalTime.of(12, 0));
        responseDTO.setAtivo(true);
        responseDTO.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Deve criar horário disponível com autenticação - 201")
    @WithMockUser
    void createHorarioDisponivel_Authenticated_201() throws Exception {
        HorarioDisponivelRequestDTO requestDTO = new HorarioDisponivelRequestDTO();
        requestDTO.setProfissionalId(profissionalId);
        requestDTO.setDiaSemana(DayOfWeek.MONDAY);
        requestDTO.setHoraInicio(LocalTime.of(8, 0));
        requestDTO.setHoraFim(LocalTime.of(12, 0));

        when(horarioDisponivelService.createHorarioDisponivel(any(HorarioDisponivelRequestDTO.class)))
                .thenReturn(horario);
        when(horarioDisponivelMapper.toResponseDTO(horario)).thenReturn(responseDTO);

        mockMvc.perform(post("/api/v1/disponibilidades")
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.profissionalNome").value("Dr. Ana"))
                .andExpect(jsonPath("$.diaSemana").value("MONDAY"));
    }

    @Test
    @DisplayName("Deve listar horários com autenticação - 200")
    @WithMockUser
    void getAllHorariosDisponiveis_Authenticated_200() throws Exception {
        when(horarioDisponivelService.getAllHorariosDisponiveis()).thenReturn(List.of(horario));
        when(horarioDisponivelMapper.toResponseDTO(horario)).thenReturn(responseDTO);

        mockMvc.perform(get("/api/v1/disponibilidades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].profissionalNome").value("Dr. Ana"));
    }

    @Test
    @DisplayName("Deve buscar horário por ID com autenticação - 200")
    @WithMockUser
    void getHorarioDisponivelById_Authenticated_200() throws Exception {
        when(horarioDisponivelService.getHorarioDisponivelById(horarioId)).thenReturn(horario);
        when(horarioDisponivelMapper.toResponseDTO(horario)).thenReturn(responseDTO);

        mockMvc.perform(get("/api/v1/disponibilidades/{id}", horarioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profissionalNome").value("Dr. Ana"));
    }

    @Test
    @DisplayName("Deve listar horários por profissional - 200")
    @WithMockUser
    void getHorariosByProfissional_200() throws Exception {
        when(horarioDisponivelService.getHorariosByProfissional(profissionalId)).thenReturn(List.of(horario));
        when(horarioDisponivelMapper.toResponseDTO(horario)).thenReturn(responseDTO);

        mockMvc.perform(get("/api/v1/disponibilidades/profissional/{profissionalId}", profissionalId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].diaSemana").value("MONDAY"));
    }

    @Test
    @DisplayName("Deve atualizar horário com autenticação - 200")
    @WithMockUser
    void updateHorarioDisponivel_Authenticated_200() throws Exception {
        HorarioDisponivelUpdateDTO updateDTO = new HorarioDisponivelUpdateDTO();
        updateDTO.setHoraFim(LocalTime.of(13, 0));

        when(horarioDisponivelService.updateHorarioDisponivel(any(UUID.class), any(HorarioDisponivelUpdateDTO.class)))
                .thenReturn(horario);
        when(horarioDisponivelMapper.toResponseDTO(horario)).thenReturn(responseDTO);

        mockMvc.perform(put("/api/v1/disponibilidades/{id}", horarioId)
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Deve deletar horário com autenticação - 204")
    @WithMockUser
    void deleteHorarioDisponivel_Authenticated_204() throws Exception {
        doNothing().when(horarioDisponivelService).deleteHorarioDisponivel(horarioId);

        mockMvc.perform(delete("/api/v1/disponibilidades/{id}", horarioId)
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Deve retornar 401 sem autenticação")
    void getAllHorariosDisponiveis_Unauthenticated_401() throws Exception {
        mockMvc.perform(get("/api/v1/disponibilidades"))
                .andExpect(status().isUnauthorized());
    }
}
