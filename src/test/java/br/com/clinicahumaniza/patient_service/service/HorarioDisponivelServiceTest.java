package br.com.clinicahumaniza.patient_service.service;

import br.com.clinicahumaniza.patient_service.dto.HorarioDisponivelRequestDTO;
import br.com.clinicahumaniza.patient_service.dto.HorarioDisponivelUpdateDTO;
import br.com.clinicahumaniza.patient_service.exception.BusinessException;
import br.com.clinicahumaniza.patient_service.exception.ResourceNotFoundException;
import br.com.clinicahumaniza.patient_service.mapper.HorarioDisponivelMapper;
import br.com.clinicahumaniza.patient_service.model.HorarioDisponivel;
import br.com.clinicahumaniza.patient_service.model.Profissional;
import br.com.clinicahumaniza.patient_service.repository.HorarioDisponivelRepository;
import br.com.clinicahumaniza.patient_service.repository.ProfissionalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HorarioDisponivelServiceTest {

    @Mock
    private HorarioDisponivelRepository horarioDisponivelRepository;

    @Mock
    private ProfissionalRepository profissionalRepository;

    @Mock
    private HorarioDisponivelMapper horarioDisponivelMapper;

    @InjectMocks
    private HorarioDisponivelService horarioDisponivelService;

    private HorarioDisponivel horario;
    private Profissional profissional;
    private HorarioDisponivelRequestDTO requestDTO;
    private UUID horarioId;
    private UUID profissionalId;

    @BeforeEach
    void setUp() {
        horarioId = UUID.randomUUID();
        profissionalId = UUID.randomUUID();

        profissional = new Profissional();
        profissional.setId(profissionalId);
        profissional.setNome("Dr. Ana");

        horario = new HorarioDisponivel();
        horario.setId(horarioId);
        horario.setProfissional(profissional);
        horario.setDiaSemana(DayOfWeek.MONDAY);
        horario.setHoraInicio(LocalTime.of(8, 0));
        horario.setHoraFim(LocalTime.of(12, 0));
        horario.setAtivo(true);

        requestDTO = new HorarioDisponivelRequestDTO();
        requestDTO.setProfissionalId(profissionalId);
        requestDTO.setDiaSemana(DayOfWeek.MONDAY);
        requestDTO.setHoraInicio(LocalTime.of(8, 0));
        requestDTO.setHoraFim(LocalTime.of(12, 0));
    }

    @Test
    @DisplayName("Deve criar horário disponível com sucesso")
    void createHorarioDisponivel_Success() {
        when(profissionalRepository.findById(profissionalId)).thenReturn(Optional.of(profissional));
        when(horarioDisponivelMapper.toEntity(requestDTO, profissional)).thenReturn(horario);
        when(horarioDisponivelRepository.save(horario)).thenReturn(horario);

        HorarioDisponivel result = horarioDisponivelService.createHorarioDisponivel(requestDTO);

        assertThat(result).isNotNull();
        assertThat(result.getDiaSemana()).isEqualTo(DayOfWeek.MONDAY);
        verify(horarioDisponivelRepository).save(horario);
    }

    @Test
    @DisplayName("Deve lançar exceção ao criar horário com profissional inexistente")
    void createHorarioDisponivel_ProfissionalNotFound() {
        when(profissionalRepository.findById(profissionalId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> horarioDisponivelService.createHorarioDisponivel(requestDTO))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(horarioDisponivelRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar exceção quando hora início >= hora fim")
    void createHorarioDisponivel_HoraInicioMaiorQueFim() {
        requestDTO.setHoraInicio(LocalTime.of(14, 0));
        requestDTO.setHoraFim(LocalTime.of(8, 0));

        when(profissionalRepository.findById(profissionalId)).thenReturn(Optional.of(profissional));

        assertThatThrownBy(() -> horarioDisponivelService.createHorarioDisponivel(requestDTO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Hora de início deve ser anterior à hora de fim");

        verify(horarioDisponivelRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar exceção quando hora início igual hora fim")
    void createHorarioDisponivel_HoraInicioIgualFim() {
        requestDTO.setHoraInicio(LocalTime.of(10, 0));
        requestDTO.setHoraFim(LocalTime.of(10, 0));

        when(profissionalRepository.findById(profissionalId)).thenReturn(Optional.of(profissional));

        assertThatThrownBy(() -> horarioDisponivelService.createHorarioDisponivel(requestDTO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Hora de início deve ser anterior à hora de fim");
    }

    @Test
    @DisplayName("Deve buscar horário por ID com sucesso")
    void getHorarioDisponivelById_Success() {
        when(horarioDisponivelRepository.findById(horarioId)).thenReturn(Optional.of(horario));

        HorarioDisponivel result = horarioDisponivelService.getHorarioDisponivelById(horarioId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(horarioId);
    }

    @Test
    @DisplayName("Deve lançar exceção quando horário não encontrado")
    void getHorarioDisponivelById_NotFound() {
        when(horarioDisponivelRepository.findById(horarioId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> horarioDisponivelService.getHorarioDisponivelById(horarioId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Deve listar todos os horários")
    void getAllHorariosDisponiveis_Success() {
        when(horarioDisponivelRepository.findAll()).thenReturn(List.of(horario));

        List<HorarioDisponivel> result = horarioDisponivelService.getAllHorariosDisponiveis();

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("Deve listar horários por profissional")
    void getHorariosByProfissional_Success() {
        when(horarioDisponivelRepository.findByProfissionalId(profissionalId)).thenReturn(List.of(horario));

        List<HorarioDisponivel> result = horarioDisponivelService.getHorariosByProfissional(profissionalId);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("Deve atualizar horário com sucesso")
    void updateHorarioDisponivel_Success() {
        HorarioDisponivelUpdateDTO updateDTO = new HorarioDisponivelUpdateDTO();
        updateDTO.setHoraFim(LocalTime.of(13, 0));

        when(horarioDisponivelRepository.findById(horarioId)).thenReturn(Optional.of(horario));
        doNothing().when(horarioDisponivelMapper).updateEntityFromDto(updateDTO, horario);
        when(horarioDisponivelRepository.save(horario)).thenReturn(horario);

        HorarioDisponivel result = horarioDisponivelService.updateHorarioDisponivel(horarioId, updateDTO);

        assertThat(result).isNotNull();
        verify(horarioDisponivelRepository).save(horario);
    }

    @Test
    @DisplayName("Deve deletar horário com sucesso (soft delete)")
    void deleteHorarioDisponivel_Success() {
        when(horarioDisponivelRepository.findById(horarioId)).thenReturn(Optional.of(horario));
        when(horarioDisponivelRepository.save(any(HorarioDisponivel.class))).thenReturn(horario);

        horarioDisponivelService.deleteHorarioDisponivel(horarioId);

        assertThat(horario.isAtivo()).isFalse();
        verify(horarioDisponivelRepository).save(horario);
    }
}
