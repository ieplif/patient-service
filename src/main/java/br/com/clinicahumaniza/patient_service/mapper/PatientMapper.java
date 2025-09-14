package br.com.clinicahumaniza.patient_service.mapper;

import br.com.clinicahumaniza.patient_service.dto.PatientRequestDTO;
import br.com.clinicahumaniza.patient_service.dto.PatientResponseDTO;
import br.com.clinicahumaniza.patient_service.model.Patient;
import org.springframework.stereotype.Component;

@Component // Torna esta classe um Bean do Spring para que possamos injetá-la.
public class PatientMapper {

    public Patient toEntity(PatientRequestDTO dto) {
        Patient patient = new Patient();
        patient.setNomeCompleto(dto.getNomeCompleto());
        patient.setEmail(dto.getEmail());
        patient.setCpf(dto.getCpf());
        patient.setDataNascimento(dto.getDataNascimento());
        patient.setTelefone(dto.getTelefone());
        patient.setEndereco(dto.getEndereco());
        patient.setProfissao(dto.getProfissao());
        patient.setEstadoCivil(dto.getEstadoCivil());
        if (dto.getConsentimentoLgpd() != null) {
            patient.setConsentimentoLgpd(dto.getConsentimentoLgpd());
        }
        return patient;
    }

    public PatientResponseDTO toResponseDTO(Patient entity) {
        PatientResponseDTO dto = new PatientResponseDTO();
        dto.setId(entity.getId());
        dto.setNomeCompleto(entity.getNomeCompleto());
        dto.setEmail(entity.getEmail());
        dto.setDataNascimento(entity.getDataNascimento());
        dto.setTelefone(entity.getTelefone());
        dto.setStatusAtivo(entity.isStatusAtivo());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }
}
