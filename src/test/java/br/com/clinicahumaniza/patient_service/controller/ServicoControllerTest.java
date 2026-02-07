package br.com.clinicahumaniza.patient_service.controller;

import br.com.clinicahumaniza.patient_service.dto.ServicoRequestDTO;
import br.com.clinicahumaniza.patient_service.dto.ServicoResponseDTO;
import br.com.clinicahumaniza.patient_service.dto.ServicoUpdateDTO;
import br.com.clinicahumaniza.patient_service.mapper.ServicoMapper;
import br.com.clinicahumaniza.patient_service.model.Atividade;
import br.com.clinicahumaniza.patient_service.model.Plano;
import br.com.clinicahumaniza.patient_service.model.Servico;
import br.com.clinicahumaniza.patient_service.security.JwtAuthenticationFilter;
import br.com.clinicahumaniza.patient_service.security.JwtService;
import br.com.clinicahumaniza.patient_service.security.SecurityConfig;
import br.com.clinicahumaniza.patient_service.service.ServicoService;
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

import java.math.BigDecimal;
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

@WebMvcTest(ServicoController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class ServicoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ServicoService servicoService;

    @MockitoBean
    private ServicoMapper servicoMapper;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private ObjectMapper objectMapper;
    private Servico servico;
    private ServicoResponseDTO responseDTO;
    private UUID servicoId;
    private UUID atividadeId;
    private UUID planoId;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        servicoId = UUID.randomUUID();
        atividadeId = UUID.randomUUID();
        planoId = UUID.randomUUID();

        Atividade atividade = new Atividade();
        atividade.setId(atividadeId);
        atividade.setNome("Pilates");

        Plano plano = new Plano();
        plano.setId(planoId);
        plano.setNome("Mensal");

        servico = new Servico();
        servico.setId(servicoId);
        servico.setAtividade(atividade);
        servico.setPlano(plano);
        servico.setTipoAtendimento("individual");
        servico.setQuantidade(4);
        servico.setUnidadeServico("sessao");
        servico.setModalidadeLocal("clinica");
        servico.setValor(new BigDecimal("350.00"));
        servico.setAtivo(true);

        responseDTO = new ServicoResponseDTO();
        responseDTO.setId(servicoId);
        responseDTO.setAtividadeId(atividadeId);
        responseDTO.setAtividadeNome("Pilates");
        responseDTO.setPlanoId(planoId);
        responseDTO.setPlanoNome("Mensal");
        responseDTO.setTipoAtendimento("individual");
        responseDTO.setQuantidade(4);
        responseDTO.setUnidadeServico("sessao");
        responseDTO.setModalidadeLocal("clinica");
        responseDTO.setValor(new BigDecimal("350.00"));
        responseDTO.setAtivo(true);
        responseDTO.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Deve criar serviço com autenticação - 201")
    @WithMockUser
    void createServico_Authenticated_201() throws Exception {
        ServicoRequestDTO requestDTO = new ServicoRequestDTO();
        requestDTO.setAtividadeId(atividadeId);
        requestDTO.setPlanoId(planoId);
        requestDTO.setTipoAtendimento("individual");
        requestDTO.setQuantidade(4);
        requestDTO.setUnidadeServico("sessao");
        requestDTO.setModalidadeLocal("clinica");
        requestDTO.setValor(new BigDecimal("350.00"));

        when(servicoService.createServico(any(ServicoRequestDTO.class))).thenReturn(servico);
        when(servicoMapper.toResponseDTO(servico)).thenReturn(responseDTO);

        mockMvc.perform(post("/api/v1/servicos")
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.atividadeNome").value("Pilates"))
                .andExpect(jsonPath("$.planoNome").value("Mensal"));
    }

    @Test
    @DisplayName("Deve listar serviços com autenticação - 200")
    @WithMockUser
    void getAllServicos_Authenticated_200() throws Exception {
        when(servicoService.getAllServicos()).thenReturn(List.of(servico));
        when(servicoMapper.toResponseDTO(servico)).thenReturn(responseDTO);

        mockMvc.perform(get("/api/v1/servicos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].atividadeNome").value("Pilates"));
    }

    @Test
    @DisplayName("Deve buscar serviço por ID com autenticação - 200")
    @WithMockUser
    void getServicoById_Authenticated_200() throws Exception {
        when(servicoService.getServicoById(servicoId)).thenReturn(servico);
        when(servicoMapper.toResponseDTO(servico)).thenReturn(responseDTO);

        mockMvc.perform(get("/api/v1/servicos/{id}", servicoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.atividadeNome").value("Pilates"));
    }

    @Test
    @DisplayName("Deve listar serviços por atividade - 200")
    @WithMockUser
    void getServicosByAtividade_200() throws Exception {
        when(servicoService.getServicosByAtividade(atividadeId)).thenReturn(List.of(servico));
        when(servicoMapper.toResponseDTO(servico)).thenReturn(responseDTO);

        mockMvc.perform(get("/api/v1/servicos/atividade/{atividadeId}", atividadeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].atividadeNome").value("Pilates"));
    }

    @Test
    @DisplayName("Deve listar serviços por plano - 200")
    @WithMockUser
    void getServicosByPlano_200() throws Exception {
        when(servicoService.getServicosByPlano(planoId)).thenReturn(List.of(servico));
        when(servicoMapper.toResponseDTO(servico)).thenReturn(responseDTO);

        mockMvc.perform(get("/api/v1/servicos/plano/{planoId}", planoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].planoNome").value("Mensal"));
    }

    @Test
    @DisplayName("Deve atualizar serviço com autenticação - 200")
    @WithMockUser
    void updateServico_Authenticated_200() throws Exception {
        ServicoUpdateDTO updateDTO = new ServicoUpdateDTO();
        updateDTO.setValor(new BigDecimal("400.00"));

        when(servicoService.updateServico(any(UUID.class), any(ServicoUpdateDTO.class))).thenReturn(servico);
        when(servicoMapper.toResponseDTO(servico)).thenReturn(responseDTO);

        mockMvc.perform(put("/api/v1/servicos/{id}", servicoId)
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Deve deletar serviço com autenticação - 204")
    @WithMockUser
    void deleteServico_Authenticated_204() throws Exception {
        doNothing().when(servicoService).deleteServico(servicoId);

        mockMvc.perform(delete("/api/v1/servicos/{id}", servicoId)
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Deve retornar 401 sem autenticação")
    void getAllServicos_Unauthenticated_401() throws Exception {
        mockMvc.perform(get("/api/v1/servicos"))
                .andExpect(status().isUnauthorized());
    }
}
