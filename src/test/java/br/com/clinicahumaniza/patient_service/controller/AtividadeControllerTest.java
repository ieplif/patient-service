package br.com.clinicahumaniza.patient_service.controller;

import br.com.clinicahumaniza.patient_service.dto.AtividadeRequestDTO;
import br.com.clinicahumaniza.patient_service.dto.AtividadeResponseDTO;
import br.com.clinicahumaniza.patient_service.dto.AtividadeUpdateDTO;
import br.com.clinicahumaniza.patient_service.exception.ResourceNotFoundException;
import br.com.clinicahumaniza.patient_service.mapper.AtividadeMapper;
import br.com.clinicahumaniza.patient_service.model.Atividade;
import br.com.clinicahumaniza.patient_service.security.JwtAuthenticationFilter;
import br.com.clinicahumaniza.patient_service.security.JwtService;
import br.com.clinicahumaniza.patient_service.security.SecurityConfig;
import br.com.clinicahumaniza.patient_service.service.AtividadeService;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AtividadeController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AtividadeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AtividadeService atividadeService;

    @MockitoBean
    private AtividadeMapper atividadeMapper;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private ObjectMapper objectMapper;
    private Atividade atividade;
    private AtividadeResponseDTO responseDTO;
    private UUID atividadeId;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        atividadeId = UUID.randomUUID();

        atividade = new Atividade();
        atividade.setId(atividadeId);
        atividade.setNome("Pilates");
        atividade.setDescricao("Método de exercícios físicos");
        atividade.setDuracaoPadrao(50);
        atividade.setAtivo(true);

        responseDTO = new AtividadeResponseDTO();
        responseDTO.setId(atividadeId);
        responseDTO.setNome("Pilates");
        responseDTO.setDescricao("Método de exercícios físicos");
        responseDTO.setDuracaoPadrao(50);
        responseDTO.setAtivo(true);
        responseDTO.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Deve criar atividade com autenticação - 201")
    @WithMockUser
    void createAtividade_Authenticated_201() throws Exception {
        AtividadeRequestDTO requestDTO = new AtividadeRequestDTO();
        requestDTO.setNome("Pilates");
        requestDTO.setDescricao("Método de exercícios físicos");
        requestDTO.setDuracaoPadrao(50);

        when(atividadeService.createAtividade(any(AtividadeRequestDTO.class))).thenReturn(atividade);
        when(atividadeMapper.toResponseDTO(atividade)).thenReturn(responseDTO);

        mockMvc.perform(post("/api/v1/atividades")
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Pilates"));
    }

    @Test
    @DisplayName("Deve listar atividades com autenticação - 200")
    @WithMockUser
    void getAllAtividades_Authenticated_200() throws Exception {
        when(atividadeService.getAllAtividades()).thenReturn(List.of(atividade));
        when(atividadeMapper.toResponseDTO(atividade)).thenReturn(responseDTO);

        mockMvc.perform(get("/api/v1/atividades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Pilates"));
    }

    @Test
    @DisplayName("Deve buscar atividade por ID com autenticação - 200")
    @WithMockUser
    void getAtividadeById_Authenticated_200() throws Exception {
        when(atividadeService.getAtividadeById(atividadeId)).thenReturn(atividade);
        when(atividadeMapper.toResponseDTO(atividade)).thenReturn(responseDTO);

        mockMvc.perform(get("/api/v1/atividades/{id}", atividadeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Pilates"));
    }

    @Test
    @DisplayName("Deve atualizar atividade com autenticação - 200")
    @WithMockUser
    void updateAtividade_Authenticated_200() throws Exception {
        AtividadeUpdateDTO updateDTO = new AtividadeUpdateDTO();
        updateDTO.setNome("Pilates Avançado");

        when(atividadeService.updateAtividade(any(UUID.class), any(AtividadeUpdateDTO.class))).thenReturn(atividade);
        when(atividadeMapper.toResponseDTO(atividade)).thenReturn(responseDTO);

        mockMvc.perform(put("/api/v1/atividades/{id}", atividadeId)
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Deve deletar atividade com autenticação - 204")
    @WithMockUser
    void deleteAtividade_Authenticated_204() throws Exception {
        doNothing().when(atividadeService).deleteAtividade(atividadeId);

        mockMvc.perform(delete("/api/v1/atividades/{id}", atividadeId)
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Deve retornar 401 sem autenticação")
    void getAllAtividades_Unauthenticated_401() throws Exception {
        mockMvc.perform(get("/api/v1/atividades"))
                .andExpect(status().isUnauthorized());
    }
}
