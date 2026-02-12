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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.util.List;
import java.util.UUID;

@Service
public class HorarioDisponivelService {

    private final HorarioDisponivelRepository horarioDisponivelRepository;
    private final ProfissionalRepository profissionalRepository;
    private final HorarioDisponivelMapper horarioDisponivelMapper;

    @Autowired
    public HorarioDisponivelService(HorarioDisponivelRepository horarioDisponivelRepository,
                                     ProfissionalRepository profissionalRepository,
                                     HorarioDisponivelMapper horarioDisponivelMapper) {
        this.horarioDisponivelRepository = horarioDisponivelRepository;
        this.profissionalRepository = profissionalRepository;
        this.horarioDisponivelMapper = horarioDisponivelMapper;
    }

    @Transactional
    public HorarioDisponivel createHorarioDisponivel(HorarioDisponivelRequestDTO dto) {
        Profissional profissional = profissionalRepository.findById(dto.getProfissionalId())
                .orElseThrow(() -> new ResourceNotFoundException("Profissional", dto.getProfissionalId()));

        validarHorario(dto.getHoraInicio(), dto.getHoraFim());

        HorarioDisponivel horario = horarioDisponivelMapper.toEntity(dto, profissional);
        return horarioDisponivelRepository.save(horario);
    }

    public HorarioDisponivel getHorarioDisponivelById(UUID id) {
        return horarioDisponivelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Horário disponível", id));
    }

    public List<HorarioDisponivel> getAllHorariosDisponiveis() {
        return horarioDisponivelRepository.findAll();
    }

    public List<HorarioDisponivel> getHorariosByProfissional(UUID profissionalId) {
        return horarioDisponivelRepository.findByProfissionalId(profissionalId);
    }

    public List<HorarioDisponivel> getHorariosByProfissionalAndDia(UUID profissionalId, DayOfWeek diaSemana) {
        return horarioDisponivelRepository.findByProfissionalIdAndDiaSemana(profissionalId, diaSemana);
    }

    @Transactional
    public HorarioDisponivel updateHorarioDisponivel(UUID id, HorarioDisponivelUpdateDTO dto) {
        HorarioDisponivel horario = horarioDisponivelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Horário disponível", id));

        horarioDisponivelMapper.updateEntityFromDto(dto, horario);

        validarHorario(horario.getHoraInicio(), horario.getHoraFim());

        return horarioDisponivelRepository.save(horario);
    }

    @Transactional
    public void deleteHorarioDisponivel(UUID id) {
        HorarioDisponivel horario = horarioDisponivelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Horário disponível", id));
        horario.setAtivo(false);
        horarioDisponivelRepository.save(horario);
    }

    private void validarHorario(java.time.LocalTime horaInicio, java.time.LocalTime horaFim) {
        if (!horaInicio.isBefore(horaFim)) {
            throw new BusinessException("Hora de início deve ser anterior à hora de fim");
        }
    }
}
