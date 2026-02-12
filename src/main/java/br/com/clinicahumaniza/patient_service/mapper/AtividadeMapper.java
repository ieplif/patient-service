package br.com.clinicahumaniza.patient_service.mapper;

import br.com.clinicahumaniza.patient_service.dto.AtividadeRequestDTO;
import br.com.clinicahumaniza.patient_service.dto.AtividadeResponseDTO;
import br.com.clinicahumaniza.patient_service.dto.AtividadeUpdateDTO;
import br.com.clinicahumaniza.patient_service.model.Atividade;
import org.springframework.stereotype.Component;

@Component
public class AtividadeMapper {

    public Atividade toEntity(AtividadeRequestDTO dto) {
        Atividade atividade = new Atividade();
        atividade.setNome(dto.getNome());
        atividade.setDescricao(dto.getDescricao());
        atividade.setDuracaoPadrao(dto.getDuracaoPadrao());
        if (dto.getCapacidadeMaxima() != null) {
            atividade.setCapacidadeMaxima(dto.getCapacidadeMaxima());
        }
        return atividade;
    }

    public AtividadeResponseDTO toResponseDTO(Atividade entity) {
        AtividadeResponseDTO dto = new AtividadeResponseDTO();
        dto.setId(entity.getId());
        dto.setNome(entity.getNome());
        dto.setDescricao(entity.getDescricao());
        dto.setDuracaoPadrao(entity.getDuracaoPadrao());
        dto.setCapacidadeMaxima(entity.getCapacidadeMaxima());
        dto.setAtivo(entity.isAtivo());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    public void updateEntityFromDto(AtividadeUpdateDTO dto, Atividade entity) {
        if (dto.getNome() != null) {
            entity.setNome(dto.getNome());
        }
        if (dto.getDescricao() != null) {
            entity.setDescricao(dto.getDescricao());
        }
        if (dto.getDuracaoPadrao() != null) {
            entity.setDuracaoPadrao(dto.getDuracaoPadrao());
        }
        if (dto.getCapacidadeMaxima() != null) {
            entity.setCapacidadeMaxima(dto.getCapacidadeMaxima());
        }
    }
}
