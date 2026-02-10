package br.com.clinicahumaniza.patient_service.controller;

import br.com.clinicahumaniza.patient_service.dto.ProfissionalRequestDTO;
import br.com.clinicahumaniza.patient_service.dto.ProfissionalResponseDTO;
import br.com.clinicahumaniza.patient_service.dto.ProfissionalUpdateDTO;
import br.com.clinicahumaniza.patient_service.mapper.ProfissionalMapper;
import br.com.clinicahumaniza.patient_service.model.Profissional;
import br.com.clinicahumaniza.patient_service.security.JwtAuthenticationFilter;
import br.com.clinicahumaniza.patient_service.security.JwtService;
import br.com.clinicahumaniza.patient_service.security.SecurityConfig;
import br.com.clinicahumaniza.patient_service.service.ProfissionalService;
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
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProfissionalController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class ProfissionalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProfissionalService profissionalService;

    @MockitoBean
    private ProfissionalMapper profissionalMapper;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private ObjectMapper objectMapper;
    private Profissional profissional;
    private ProfissionalResponseDTO responseDTO;
    private UUID profissionalId;
    private UUID atividadeId;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        profissionalId = UUID.randomUUID();
        atividadeId = UUID.randomUUID();

        profissional = new Profissional();
        profissional.setId(profissionalId);
        profissional.setNome("Maria Silva");
        profissional.setTelefone("11999999999");
        profissional.setAtivo(true);

        ProfissionalResponseDTO.AtividadeSimpleDTO atividadeSimple =
                new ProfissionalResponseDTO.AtividadeSimpleDTO(atividadeId, "Fisioterapia Pélvica");

        responseDTO = new ProfissionalResponseDTO();
        responseDTO.setId(profissionalId);
        responseDTO.setNome("Maria Silva");
        responseDTO.setTelefone("11999999999");
        responseDTO.setEmail("maria@email.com");
        responseDTO.setAtividades(List.of(atividadeSimple));
        responseDTO.setAtivo(true);
        responseDTO.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Deve criar profissional com autenticação - 201")
    @WithMockUser
    void createProfissional_Authenticated_201() throws Exception {
        ProfissionalRequestDTO requestDTO = new ProfissionalRequestDTO();
        requestDTO.setNome("Maria Silva");
        requestDTO.setTelefone("11999999999");
        requestDTO.setEmail("maria@email.com");
        requestDTO.setSenha("senha123");
        requestDTO.setAtividadeIds(Set.of(atividadeId));

        when(profissionalService.createProfissional(any(ProfissionalRequestDTO.class))).thenReturn(profissional);
        when(profissionalMapper.toResponseDTO(profissional)).thenReturn(responseDTO);

        mockMvc.perform(post("/api/v1/profissionais")
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Maria Silva"))
                .andExpect(jsonPath("$.email").value("maria@email.com"))
                .andExpect(jsonPath("$.atividades[0].nome").value("Fisioterapia Pélvica"));
    }

    @Test
    @DisplayName("Deve listar profissionais com autenticação - 200")
    @WithMockUser
    void getAllProfissionais_Authenticated_200() throws Exception {
        when(profissionalService.getAllProfissionais()).thenReturn(List.of(profissional));
        when(profissionalMapper.toResponseDTO(profissional)).thenReturn(responseDTO);

        mockMvc.perform(get("/api/v1/profissionais"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Maria Silva"));
    }

    @Test
    @DisplayName("Deve buscar profissional por ID com autenticação - 200")
    @WithMockUser
    void getProfissionalById_Authenticated_200() throws Exception {
        when(profissionalService.getProfissionalById(profissionalId)).thenReturn(profissional);
        when(profissionalMapper.toResponseDTO(profissional)).thenReturn(responseDTO);

        mockMvc.perform(get("/api/v1/profissionais/{id}", profissionalId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Maria Silva"));
    }

    @Test
    @DisplayName("Deve listar profissionais por atividade - 200")
    @WithMockUser
    void getProfissionaisByAtividade_Authenticated_200() throws Exception {
        when(profissionalService.getProfissionaisByAtividade(atividadeId)).thenReturn(List.of(profissional));
        when(profissionalMapper.toResponseDTO(profissional)).thenReturn(responseDTO);

        mockMvc.perform(get("/api/v1/profissionais/atividade/{atividadeId}", atividadeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Maria Silva"));
    }

    @Test
    @DisplayName("Deve atualizar profissional com autenticação - 200")
    @WithMockUser
    void updateProfissional_Authenticated_200() throws Exception {
        ProfissionalUpdateDTO updateDTO = new ProfissionalUpdateDTO();
        updateDTO.setNome("Maria Silva Santos");

        when(profissionalService.updateProfissional(any(UUID.class), any(ProfissionalUpdateDTO.class))).thenReturn(profissional);
        when(profissionalMapper.toResponseDTO(profissional)).thenReturn(responseDTO);

        mockMvc.perform(put("/api/v1/profissionais/{id}", profissionalId)
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Deve deletar profissional com autenticação - 204")
    @WithMockUser
    void deleteProfissional_Authenticated_204() throws Exception {
        doNothing().when(profissionalService).deleteProfissional(profissionalId);

        mockMvc.perform(delete("/api/v1/profissionais/{id}", profissionalId)
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Deve retornar 401 sem autenticação")
    void getAllProfissionais_Unauthenticated_401() throws Exception {
        mockMvc.perform(get("/api/v1/profissionais"))
                .andExpect(status().isUnauthorized());
    }
}
