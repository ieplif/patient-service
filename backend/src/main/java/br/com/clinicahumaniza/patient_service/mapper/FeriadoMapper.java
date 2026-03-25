package br.com.clinicahumaniza.patient_service.mapper;

import br.com.clinicahumaniza.patient_service.dto.FeriadoRequestDTO;
import br.com.clinicahumaniza.patient_service.dto.FeriadoResponseDTO;
import br.com.clinicahumaniza.patient_service.model.Feriado;
import org.springframework.stereotype.Component;

@Component
public class FeriadoMapper {

    public Feriado toEntity(FeriadoRequestDTO dto) {
        Feriado feriado = new Feriado();
        feriado.setData(dto.getData());
        feriado.setDescricao(dto.getDescricao());
        feriado.setRecorrente(dto.isRecorrente());
        return feriado;
    }

    public FeriadoResponseDTO toResponseDTO(Feriado entity) {
        FeriadoResponseDTO dto = new FeriadoResponseDTO();
        dto.setId(entity.getId());
        dto.setData(entity.getData());
        dto.setDescricao(entity.getDescricao());
        dto.setRecorrente(entity.isRecorrente());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    public void updateEntityFromDto(FeriadoRequestDTO dto, Feriado entity) {
        if (dto.getData() != null) entity.setData(dto.getData());
        if (dto.getDescricao() != null) entity.setDescricao(dto.getDescricao());
        entity.setRecorrente(dto.isRecorrente());
    }
}
