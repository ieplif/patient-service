package br.com.clinicahumaniza.patient_service.mapper;

import br.com.clinicahumaniza.patient_service.dto.ProfissionalRequestDTO;
import br.com.clinicahumaniza.patient_service.dto.ProfissionalResponseDTO;
import br.com.clinicahumaniza.patient_service.dto.ProfissionalUpdateDTO;
import br.com.clinicahumaniza.patient_service.model.Atividade;
import br.com.clinicahumaniza.patient_service.model.Profissional;
import br.com.clinicahumaniza.patient_service.model.User;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ProfissionalMapper {

    public Profissional toEntity(ProfissionalRequestDTO dto, User user, Set<Atividade> atividades) {
        Profissional profissional = new Profissional();
        profissional.setNome(dto.getNome());
        profissional.setTelefone(dto.getTelefone());
        profissional.setUser(user);
        profissional.setAtividades(atividades);
        return profissional;
    }

    public ProfissionalResponseDTO toResponseDTO(Profissional entity) {
        ProfissionalResponseDTO dto = new ProfissionalResponseDTO();
        dto.setId(entity.getId());
        dto.setNome(entity.getNome());
        dto.setTelefone(entity.getTelefone());
        dto.setEmail(entity.getUser().getEmail());
        dto.setAtividades(
                entity.getAtividades().stream()
                        .map(a -> new ProfissionalResponseDTO.AtividadeSimpleDTO(a.getId(), a.getNome()))
                        .collect(Collectors.toList())
        );
        dto.setAtivo(entity.isAtivo());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    public void updateEntityFromDto(ProfissionalUpdateDTO dto, Profissional entity, Set<Atividade> atividades) {
        if (dto.getNome() != null) {
            entity.setNome(dto.getNome());
        }
        if (dto.getTelefone() != null) {
            entity.setTelefone(dto.getTelefone());
        }
        if (atividades != null) {
            entity.setAtividades(atividades);
        }
    }
}
