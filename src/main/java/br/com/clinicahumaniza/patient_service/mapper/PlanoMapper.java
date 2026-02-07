package br.com.clinicahumaniza.patient_service.mapper;

import br.com.clinicahumaniza.patient_service.dto.PlanoRequestDTO;
import br.com.clinicahumaniza.patient_service.dto.PlanoResponseDTO;
import br.com.clinicahumaniza.patient_service.dto.PlanoUpdateDTO;
import br.com.clinicahumaniza.patient_service.model.Plano;
import org.springframework.stereotype.Component;

@Component
public class PlanoMapper {

    public Plano toEntity(PlanoRequestDTO dto) {
        Plano plano = new Plano();
        plano.setNome(dto.getNome());
        plano.setDescricao(dto.getDescricao());
        plano.setTipoPlano(dto.getTipoPlano());
        plano.setValidadeDias(dto.getValidadeDias());
        plano.setSessoesIncluidas(dto.getSessoesIncluidas());
        if (dto.getPermiteTransferencia() != null) {
            plano.setPermiteTransferencia(dto.getPermiteTransferencia());
        }
        return plano;
    }

    public PlanoResponseDTO toResponseDTO(Plano entity) {
        PlanoResponseDTO dto = new PlanoResponseDTO();
        dto.setId(entity.getId());
        dto.setNome(entity.getNome());
        dto.setDescricao(entity.getDescricao());
        dto.setTipoPlano(entity.getTipoPlano());
        dto.setValidadeDias(entity.getValidadeDias());
        dto.setSessoesIncluidas(entity.getSessoesIncluidas());
        dto.setPermiteTransferencia(entity.isPermiteTransferencia());
        dto.setAtivo(entity.isAtivo());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    public void updateEntityFromDto(PlanoUpdateDTO dto, Plano entity) {
        if (dto.getNome() != null) {
            entity.setNome(dto.getNome());
        }
        if (dto.getDescricao() != null) {
            entity.setDescricao(dto.getDescricao());
        }
        if (dto.getTipoPlano() != null) {
            entity.setTipoPlano(dto.getTipoPlano());
        }
        if (dto.getValidadeDias() != null) {
            entity.setValidadeDias(dto.getValidadeDias());
        }
        if (dto.getSessoesIncluidas() != null) {
            entity.setSessoesIncluidas(dto.getSessoesIncluidas());
        }
        if (dto.getPermiteTransferencia() != null) {
            entity.setPermiteTransferencia(dto.getPermiteTransferencia());
        }
    }
}
