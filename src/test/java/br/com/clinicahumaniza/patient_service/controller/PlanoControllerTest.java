package br.com.clinicahumaniza.patient_service.controller;

import br.com.clinicahumaniza.patient_service.dto.PlanoRequestDTO;
import br.com.clinicahumaniza.patient_service.dto.PlanoResponseDTO;
import br.com.clinicahumaniza.patient_service.dto.PlanoUpdateDTO;
import br.com.clinicahumaniza.patient_service.mapper.PlanoMapper;
import br.com.clinicahumaniza.patient_service.model.Plano;
import br.com.clinicahumaniza.patient_service.security.JwtAuthenticationFilter;
import br.com.clinicahumaniza.patient_service.security.JwtService;
import br.com.clinicahumaniza.patient_service.security.SecurityConfig;
import br.com.clinicahumaniza.patient_service.service.PlanoService;
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

@WebMvcTest(PlanoController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class PlanoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PlanoService planoService;

    @MockitoBean
    private PlanoMapper planoMapper;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private ObjectMapper objectMapper;
    private Plano plano;
    private PlanoResponseDTO responseDTO;
    private UUID planoId;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        planoId = UUID.randomUUID();

        plano = new Plano();
        plano.setId(planoId);
        plano.setNome("Mensal");
        plano.setDescricao("Plano mensal com sessões semanais");
        plano.setTipoPlano("mensal");
        plano.setValidadeDias(30);
        plano.setSessoesIncluidas(4);
        plano.setPermiteTransferencia(false);
        plano.setAtivo(true);

        responseDTO = new PlanoResponseDTO();
        responseDTO.setId(planoId);
        responseDTO.setNome("Mensal");
        responseDTO.setDescricao("Plano mensal com sessões semanais");
        responseDTO.setTipoPlano("mensal");
        responseDTO.setValidadeDias(30);
        responseDTO.setSessoesIncluidas(4);
        responseDTO.setPermiteTransferencia(false);
        responseDTO.setAtivo(true);
        responseDTO.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Deve criar plano com autenticação - 201")
    @WithMockUser
    void createPlano_Authenticated_201() throws Exception {
        PlanoRequestDTO requestDTO = new PlanoRequestDTO();
        requestDTO.setNome("Mensal");
        requestDTO.setDescricao("Plano mensal");
        requestDTO.setTipoPlano("mensal");

        when(planoService.createPlano(any(PlanoRequestDTO.class))).thenReturn(plano);
        when(planoMapper.toResponseDTO(plano)).thenReturn(responseDTO);

        mockMvc.perform(post("/api/v1/planos")
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Mensal"));
    }

    @Test
    @DisplayName("Deve listar planos com autenticação - 200")
    @WithMockUser
    void getAllPlanos_Authenticated_200() throws Exception {
        when(planoService.getAllPlanos()).thenReturn(List.of(plano));
        when(planoMapper.toResponseDTO(plano)).thenReturn(responseDTO);

        mockMvc.perform(get("/api/v1/planos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Mensal"));
    }

    @Test
    @DisplayName("Deve buscar plano por ID com autenticação - 200")
    @WithMockUser
    void getPlanoById_Authenticated_200() throws Exception {
        when(planoService.getPlanoById(planoId)).thenReturn(plano);
        when(planoMapper.toResponseDTO(plano)).thenReturn(responseDTO);

        mockMvc.perform(get("/api/v1/planos/{id}", planoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Mensal"));
    }

    @Test
    @DisplayName("Deve atualizar plano com autenticação - 200")
    @WithMockUser
    void updatePlano_Authenticated_200() throws Exception {
        PlanoUpdateDTO updateDTO = new PlanoUpdateDTO();
        updateDTO.setNome("Mensal Premium");

        when(planoService.updatePlano(any(UUID.class), any(PlanoUpdateDTO.class))).thenReturn(plano);
        when(planoMapper.toResponseDTO(plano)).thenReturn(responseDTO);

        mockMvc.perform(put("/api/v1/planos/{id}", planoId)
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Deve deletar plano com autenticação - 204")
    @WithMockUser
    void deletePlano_Authenticated_204() throws Exception {
        doNothing().when(planoService).deletePlano(planoId);

        mockMvc.perform(delete("/api/v1/planos/{id}", planoId)
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Deve retornar 401 sem autenticação")
    void getAllPlanos_Unauthenticated_401() throws Exception {
        mockMvc.perform(get("/api/v1/planos"))
                .andExpect(status().isUnauthorized());
    }
}
