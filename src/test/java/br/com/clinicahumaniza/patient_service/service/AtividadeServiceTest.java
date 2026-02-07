package br.com.clinicahumaniza.patient_service.service;

import br.com.clinicahumaniza.patient_service.dto.AtividadeRequestDTO;
import br.com.clinicahumaniza.patient_service.dto.AtividadeUpdateDTO;
import br.com.clinicahumaniza.patient_service.exception.DuplicateResourceException;
import br.com.clinicahumaniza.patient_service.exception.ResourceNotFoundException;
import br.com.clinicahumaniza.patient_service.mapper.AtividadeMapper;
import br.com.clinicahumaniza.patient_service.model.Atividade;
import br.com.clinicahumaniza.patient_service.repository.AtividadeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AtividadeServiceTest {

    @Mock
    private AtividadeRepository atividadeRepository;

    @Mock
    private AtividadeMapper atividadeMapper;

    @InjectMocks
    private AtividadeService atividadeService;

    private Atividade atividade;
    private AtividadeRequestDTO requestDTO;
    private UUID atividadeId;

    @BeforeEach
    void setUp() {
        atividadeId = UUID.randomUUID();

        atividade = new Atividade();
        atividade.setId(atividadeId);
        atividade.setNome("Pilates");
        atividade.setDescricao("Método de exercícios físicos");
        atividade.setDuracaoPadrao(50);
        atividade.setAtivo(true);

        requestDTO = new AtividadeRequestDTO();
        requestDTO.setNome("Pilates");
        requestDTO.setDescricao("Método de exercícios físicos");
        requestDTO.setDuracaoPadrao(50);
    }

    @Test
    @DisplayName("Deve criar atividade com sucesso")
    void createAtividade_Success() {
        when(atividadeRepository.existsByNome(requestDTO.getNome())).thenReturn(false);
        when(atividadeMapper.toEntity(requestDTO)).thenReturn(atividade);
        when(atividadeRepository.save(atividade)).thenReturn(atividade);

        Atividade result = atividadeService.createAtividade(requestDTO);

        assertThat(result).isNotNull();
        assertThat(result.getNome()).isEqualTo("Pilates");
        verify(atividadeRepository).save(atividade);
    }

    @Test
    @DisplayName("Deve lançar exceção ao criar atividade com nome duplicado")
    void createAtividade_DuplicateName() {
        when(atividadeRepository.existsByNome(requestDTO.getNome())).thenReturn(true);

        assertThatThrownBy(() -> atividadeService.createAtividade(requestDTO))
                .isInstanceOf(DuplicateResourceException.class);

        verify(atividadeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve buscar atividade por ID com sucesso")
    void getAtividadeById_Success() {
        when(atividadeRepository.findById(atividadeId)).thenReturn(Optional.of(atividade));

        Atividade result = atividadeService.getAtividadeById(atividadeId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(atividadeId);
    }

    @Test
    @DisplayName("Deve lançar exceção quando atividade não encontrada")
    void getAtividadeById_NotFound() {
        when(atividadeRepository.findById(atividadeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> atividadeService.getAtividadeById(atividadeId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Deve listar todas as atividades")
    void getAllAtividades_Success() {
        when(atividadeRepository.findAll()).thenReturn(List.of(atividade));

        List<Atividade> result = atividadeService.getAllAtividades();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getNome()).isEqualTo("Pilates");
    }

    @Test
    @DisplayName("Deve atualizar atividade com sucesso")
    void updateAtividade_Success() {
        AtividadeUpdateDTO updateDTO = new AtividadeUpdateDTO();
        updateDTO.setNome("Pilates Avançado");

        when(atividadeRepository.findById(atividadeId)).thenReturn(Optional.of(atividade));
        when(atividadeRepository.existsByNome("Pilates Avançado")).thenReturn(false);
        doNothing().when(atividadeMapper).updateEntityFromDto(updateDTO, atividade);
        when(atividadeRepository.save(atividade)).thenReturn(atividade);

        Atividade result = atividadeService.updateAtividade(atividadeId, updateDTO);

        assertThat(result).isNotNull();
        verify(atividadeRepository).save(atividade);
    }

    @Test
    @DisplayName("Deve deletar atividade com sucesso (soft delete)")
    void deleteAtividade_Success() {
        when(atividadeRepository.findById(atividadeId)).thenReturn(Optional.of(atividade));
        when(atividadeRepository.save(any(Atividade.class))).thenReturn(atividade);

        atividadeService.deleteAtividade(atividadeId);

        assertThat(atividade.isAtivo()).isFalse();
        verify(atividadeRepository).save(atividade);
    }
}
