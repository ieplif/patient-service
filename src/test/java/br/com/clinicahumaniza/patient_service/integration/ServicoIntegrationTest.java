package br.com.clinicahumaniza.patient_service.integration;

import br.com.clinicahumaniza.patient_service.dto.AtividadeRequestDTO;
import br.com.clinicahumaniza.patient_service.dto.PlanoRequestDTO;
import br.com.clinicahumaniza.patient_service.dto.RegisterRequestDTO;
import br.com.clinicahumaniza.patient_service.dto.ServicoRequestDTO;
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

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ServicoIntegrationTest {

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
    @DisplayName("Fluxo completo: criar atividade → criar plano → criar serviço → listar")
    void fullServicoCrudFlow() throws Exception {
        // Criar atividade
        AtividadeRequestDTO atividadeDTO = new AtividadeRequestDTO();
        atividadeDTO.setNome("Pilates");
        atividadeDTO.setDescricao("Método de exercícios físicos");
        atividadeDTO.setDuracaoPadrao(50);

        MvcResult atividadeResult = mockMvc.perform(post("/api/v1/atividades")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(atividadeDTO)))
                .andExpect(status().isCreated())
                .andReturn();

        String atividadeId = objectMapper.readTree(atividadeResult.getResponse().getContentAsString()).get("id").asText();

        // Criar plano
        PlanoRequestDTO planoDTO = new PlanoRequestDTO();
        planoDTO.setNome("Mensal");
        planoDTO.setDescricao("Plano mensal");
        planoDTO.setTipoPlano("mensal");
        planoDTO.setValidadeDias(30);
        planoDTO.setSessoesIncluidas(4);

        MvcResult planoResult = mockMvc.perform(post("/api/v1/planos")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(planoDTO)))
                .andExpect(status().isCreated())
                .andReturn();

        String planoId = objectMapper.readTree(planoResult.getResponse().getContentAsString()).get("id").asText();

        // Criar serviço
        ServicoRequestDTO servicoDTO = new ServicoRequestDTO();
        servicoDTO.setAtividadeId(UUID.fromString(atividadeId));
        servicoDTO.setPlanoId(UUID.fromString(planoId));
        servicoDTO.setTipoAtendimento("individual");
        servicoDTO.setQuantidade(4);
        servicoDTO.setUnidadeServico("sessao");
        servicoDTO.setModalidadeLocal("clinica");
        servicoDTO.setValor(new BigDecimal("350.00"));

        mockMvc.perform(post("/api/v1/servicos")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(servicoDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.atividadeNome").value("Pilates"))
                .andExpect(jsonPath("$.planoNome").value("Mensal"))
                .andExpect(jsonPath("$.valor").value(350.00));

        // Listar serviços
        mockMvc.perform(get("/api/v1/servicos")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].atividadeNome").value("Pilates"));
    }

    @Test
    @DisplayName("Deve listar serviços por atividade")
    void getServicosByAtividade() throws Exception {
        // Criar atividade
        AtividadeRequestDTO atividadeDTO = new AtividadeRequestDTO();
        atividadeDTO.setNome("Fisioterapia");
        atividadeDTO.setDescricao("Tratamento e reabilitação");
        atividadeDTO.setDuracaoPadrao(50);

        MvcResult atividadeResult = mockMvc.perform(post("/api/v1/atividades")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(atividadeDTO)))
                .andExpect(status().isCreated())
                .andReturn();

        String atividadeId = objectMapper.readTree(atividadeResult.getResponse().getContentAsString()).get("id").asText();

        // Criar plano
        PlanoRequestDTO planoDTO = new PlanoRequestDTO();
        planoDTO.setNome("Avulso");
        planoDTO.setDescricao("Sessão avulsa");
        planoDTO.setTipoPlano("avulso");

        MvcResult planoResult = mockMvc.perform(post("/api/v1/planos")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(planoDTO)))
                .andExpect(status().isCreated())
                .andReturn();

        String planoId = objectMapper.readTree(planoResult.getResponse().getContentAsString()).get("id").asText();

        // Criar serviço
        ServicoRequestDTO servicoDTO = new ServicoRequestDTO();
        servicoDTO.setAtividadeId(UUID.fromString(atividadeId));
        servicoDTO.setPlanoId(UUID.fromString(planoId));
        servicoDTO.setTipoAtendimento("individual");
        servicoDTO.setQuantidade(1);
        servicoDTO.setUnidadeServico("sessao");
        servicoDTO.setModalidadeLocal("clinica");
        servicoDTO.setValor(new BigDecimal("150.00"));

        mockMvc.perform(post("/api/v1/servicos")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(servicoDTO)))
                .andExpect(status().isCreated());

        // Buscar serviços por atividade
        mockMvc.perform(get("/api/v1/servicos/atividade/{atividadeId}", atividadeId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].atividadeNome").value("Fisioterapia"));
    }

    @Test
    @DisplayName("Deve retornar 401 sem token")
    void accessWithoutToken_401() throws Exception {
        mockMvc.perform(get("/api/v1/servicos"))
                .andExpect(status().isUnauthorized());
    }
}
