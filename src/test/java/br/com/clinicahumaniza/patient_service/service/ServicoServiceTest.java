package br.com.clinicahumaniza.patient_service.service;

import br.com.clinicahumaniza.patient_service.dto.ServicoRequestDTO;
import br.com.clinicahumaniza.patient_service.dto.ServicoUpdateDTO;
import br.com.clinicahumaniza.patient_service.exception.ResourceNotFoundException;
import br.com.clinicahumaniza.patient_service.mapper.ServicoMapper;
import br.com.clinicahumaniza.patient_service.model.Atividade;
import br.com.clinicahumaniza.patient_service.model.Plano;
import br.com.clinicahumaniza.patient_service.model.Servico;
import br.com.clinicahumaniza.patient_service.repository.AtividadeRepository;
import br.com.clinicahumaniza.patient_service.repository.PlanoRepository;
import br.com.clinicahumaniza.patient_service.repository.ServicoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServicoServiceTest {

    @Mock
    private ServicoRepository servicoRepository;

    @Mock
    private AtividadeRepository atividadeRepository;

    @Mock
    private PlanoRepository planoRepository;

    @Mock
    private ServicoMapper servicoMapper;

    @InjectMocks
    private ServicoService servicoService;

    private Servico servico;
    private Atividade atividade;
    private Plano plano;
    private ServicoRequestDTO requestDTO;
    private UUID servicoId;
    private UUID atividadeId;
    private UUID planoId;

    @BeforeEach
    void setUp() {
        servicoId = UUID.randomUUID();
        atividadeId = UUID.randomUUID();
        planoId = UUID.randomUUID();

        atividade = new Atividade();
        atividade.setId(atividadeId);
        atividade.setNome("Pilates");
        atividade.setAtivo(true);

        plano = new Plano();
        plano.setId(planoId);
        plano.setNome("Mensal");
        plano.setAtivo(true);

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

        requestDTO = new ServicoRequestDTO();
        requestDTO.setAtividadeId(atividadeId);
        requestDTO.setPlanoId(planoId);
        requestDTO.setTipoAtendimento("individual");
        requestDTO.setQuantidade(4);
        requestDTO.setUnidadeServico("sessao");
        requestDTO.setModalidadeLocal("clinica");
        requestDTO.setValor(new BigDecimal("350.00"));
    }

    @Test
    @DisplayName("Deve criar serviço com sucesso")
    void createServico_Success() {
        when(atividadeRepository.findById(atividadeId)).thenReturn(Optional.of(atividade));
        when(planoRepository.findById(planoId)).thenReturn(Optional.of(plano));
        when(servicoMapper.toEntity(requestDTO, atividade, plano)).thenReturn(servico);
        when(servicoRepository.save(servico)).thenReturn(servico);

        Servico result = servicoService.createServico(requestDTO);

        assertThat(result).isNotNull();
        assertThat(result.getAtividade().getNome()).isEqualTo("Pilates");
        assertThat(result.getPlano().getNome()).isEqualTo("Mensal");
        verify(servicoRepository).save(servico);
    }

    @Test
    @DisplayName("Deve lançar exceção ao criar serviço com atividade inexistente")
    void createServico_AtividadeNotFound() {
        when(atividadeRepository.findById(atividadeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicoService.createServico(requestDTO))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(servicoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar exceção ao criar serviço com plano inexistente")
    void createServico_PlanoNotFound() {
        when(atividadeRepository.findById(atividadeId)).thenReturn(Optional.of(atividade));
        when(planoRepository.findById(planoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicoService.createServico(requestDTO))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(servicoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve buscar serviço por ID com sucesso")
    void getServicoById_Success() {
        when(servicoRepository.findById(servicoId)).thenReturn(Optional.of(servico));

        Servico result = servicoService.getServicoById(servicoId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(servicoId);
    }

    @Test
    @DisplayName("Deve lançar exceção quando serviço não encontrado")
    void getServicoById_NotFound() {
        when(servicoRepository.findById(servicoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicoService.getServicoById(servicoId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Deve listar todos os serviços")
    void getAllServicos_Success() {
        when(servicoRepository.findAll()).thenReturn(List.of(servico));

        List<Servico> result = servicoService.getAllServicos();

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("Deve listar serviços por atividade")
    void getServicosByAtividade_Success() {
        when(servicoRepository.findByAtividadeId(atividadeId)).thenReturn(List.of(servico));

        List<Servico> result = servicoService.getServicosByAtividade(atividadeId);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("Deve listar serviços por plano")
    void getServicosByPlano_Success() {
        when(servicoRepository.findByPlanoId(planoId)).thenReturn(List.of(servico));

        List<Servico> result = servicoService.getServicosByPlano(planoId);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("Deve atualizar serviço com sucesso")
    void updateServico_Success() {
        ServicoUpdateDTO updateDTO = new ServicoUpdateDTO();
        updateDTO.setValor(new BigDecimal("400.00"));

        when(servicoRepository.findById(servicoId)).thenReturn(Optional.of(servico));
        doNothing().when(servicoMapper).updateEntityFromDto(updateDTO, servico, null, null);
        when(servicoRepository.save(servico)).thenReturn(servico);

        Servico result = servicoService.updateServico(servicoId, updateDTO);

        assertThat(result).isNotNull();
        verify(servicoRepository).save(servico);
    }

    @Test
    @DisplayName("Deve deletar serviço com sucesso (soft delete)")
    void deleteServico_Success() {
        when(servicoRepository.findById(servicoId)).thenReturn(Optional.of(servico));
        when(servicoRepository.save(any(Servico.class))).thenReturn(servico);

        servicoService.deleteServico(servicoId);

        assertThat(servico.isAtivo()).isFalse();
        verify(servicoRepository).save(servico);
    }
}
