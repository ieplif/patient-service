package br.com.clinicahumaniza.patient_service.service;

import br.com.clinicahumaniza.patient_service.dto.PlanoRequestDTO;
import br.com.clinicahumaniza.patient_service.dto.PlanoUpdateDTO;
import br.com.clinicahumaniza.patient_service.exception.DuplicateResourceException;
import br.com.clinicahumaniza.patient_service.exception.ResourceNotFoundException;
import br.com.clinicahumaniza.patient_service.mapper.PlanoMapper;
import br.com.clinicahumaniza.patient_service.model.Plano;
import br.com.clinicahumaniza.patient_service.repository.PlanoRepository;
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
class PlanoServiceTest {

    @Mock
    private PlanoRepository planoRepository;

    @Mock
    private PlanoMapper planoMapper;

    @InjectMocks
    private PlanoService planoService;

    private Plano plano;
    private PlanoRequestDTO requestDTO;
    private UUID planoId;

    @BeforeEach
    void setUp() {
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

        requestDTO = new PlanoRequestDTO();
        requestDTO.setNome("Mensal");
        requestDTO.setDescricao("Plano mensal com sessões semanais");
        requestDTO.setTipoPlano("mensal");
        requestDTO.setValidadeDias(30);
        requestDTO.setSessoesIncluidas(4);
    }

    @Test
    @DisplayName("Deve criar plano com sucesso")
    void createPlano_Success() {
        when(planoRepository.existsByNome(requestDTO.getNome())).thenReturn(false);
        when(planoMapper.toEntity(requestDTO)).thenReturn(plano);
        when(planoRepository.save(plano)).thenReturn(plano);

        Plano result = planoService.createPlano(requestDTO);

        assertThat(result).isNotNull();
        assertThat(result.getNome()).isEqualTo("Mensal");
        verify(planoRepository).save(plano);
    }

    @Test
    @DisplayName("Deve lançar exceção ao criar plano com nome duplicado")
    void createPlano_DuplicateName() {
        when(planoRepository.existsByNome(requestDTO.getNome())).thenReturn(true);

        assertThatThrownBy(() -> planoService.createPlano(requestDTO))
                .isInstanceOf(DuplicateResourceException.class);

        verify(planoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve buscar plano por ID com sucesso")
    void getPlanoById_Success() {
        when(planoRepository.findById(planoId)).thenReturn(Optional.of(plano));

        Plano result = planoService.getPlanoById(planoId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(planoId);
    }

    @Test
    @DisplayName("Deve lançar exceção quando plano não encontrado")
    void getPlanoById_NotFound() {
        when(planoRepository.findById(planoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> planoService.getPlanoById(planoId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Deve listar todos os planos")
    void getAllPlanos_Success() {
        when(planoRepository.findAll()).thenReturn(List.of(plano));

        List<Plano> result = planoService.getAllPlanos();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getNome()).isEqualTo("Mensal");
    }

    @Test
    @DisplayName("Deve atualizar plano com sucesso")
    void updatePlano_Success() {
        PlanoUpdateDTO updateDTO = new PlanoUpdateDTO();
        updateDTO.setNome("Mensal Premium");

        when(planoRepository.findById(planoId)).thenReturn(Optional.of(plano));
        when(planoRepository.existsByNome("Mensal Premium")).thenReturn(false);
        doNothing().when(planoMapper).updateEntityFromDto(updateDTO, plano);
        when(planoRepository.save(plano)).thenReturn(plano);

        Plano result = planoService.updatePlano(planoId, updateDTO);

        assertThat(result).isNotNull();
        verify(planoRepository).save(plano);
    }

    @Test
    @DisplayName("Deve deletar plano com sucesso (soft delete)")
    void deletePlano_Success() {
        when(planoRepository.findById(planoId)).thenReturn(Optional.of(plano));
        when(planoRepository.save(any(Plano.class))).thenReturn(plano);

        planoService.deletePlano(planoId);

        assertThat(plano.isAtivo()).isFalse();
        verify(planoRepository).save(plano);
    }
}
