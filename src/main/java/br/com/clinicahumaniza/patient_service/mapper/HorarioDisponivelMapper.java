package br.com.clinicahumaniza.patient_service.mapper;

import br.com.clinicahumaniza.patient_service.dto.HorarioDisponivelRequestDTO;
import br.com.clinicahumaniza.patient_service.dto.HorarioDisponivelResponseDTO;
import br.com.clinicahumaniza.patient_service.dto.HorarioDisponivelUpdateDTO;
import br.com.clinicahumaniza.patient_service.model.HorarioDisponivel;
import br.com.clinicahumaniza.patient_service.model.Profissional;
import org.springframework.stereotype.Component;

@Component
public class HorarioDisponivelMapper {

    public HorarioDisponivel toEntity(HorarioDisponivelRequestDTO dto, Profissional profissional) {
        HorarioDisponivel horario = new HorarioDisponivel();
        horario.setProfissional(profissional);
        horario.setDiaSemana(dto.getDiaSemana());
        horario.setHoraInicio(dto.getHoraInicio());
        horario.setHoraFim(dto.getHoraFim());
        return horario;
    }

    public HorarioDisponivelResponseDTO toResponseDTO(HorarioDisponivel entity) {
        HorarioDisponivelResponseDTO dto = new HorarioDisponivelResponseDTO();
        dto.setId(entity.getId());
        dto.setProfissionalId(entity.getProfissional().getId());
        dto.setProfissionalNome(entity.getProfissional().getNome());
        dto.setDiaSemana(entity.getDiaSemana());
        dto.setHoraInicio(entity.getHoraInicio());
        dto.setHoraFim(entity.getHoraFim());
        dto.setAtivo(entity.isAtivo());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    public void updateEntityFromDto(HorarioDisponivelUpdateDTO dto, HorarioDisponivel entity) {
        if (dto.getDiaSemana() != null) entity.setDiaSemana(dto.getDiaSemana());
        if (dto.getHoraInicio() != null) entity.setHoraInicio(dto.getHoraInicio());
        if (dto.getHoraFim() != null) entity.setHoraFim(dto.getHoraFim());
    }
}
