package br.com.clinicahumaniza.patient_service.mapper;

import br.com.clinicahumaniza.patient_service.dto.AssinaturaRequestDTO;
import br.com.clinicahumaniza.patient_service.dto.AssinaturaResponseDTO;
import br.com.clinicahumaniza.patient_service.dto.AssinaturaUpdateDTO;
import br.com.clinicahumaniza.patient_service.model.Assinatura;
import br.com.clinicahumaniza.patient_service.model.Patient;
import br.com.clinicahumaniza.patient_service.model.Servico;
import org.springframework.stereotype.Component;

@Component
public class AssinaturaMapper {

    public Assinatura toEntity(AssinaturaRequestDTO dto, Patient paciente, Servico servico) {
        Assinatura assinatura = new Assinatura();
        assinatura.setPaciente(paciente);
        assinatura.setServico(servico);
        assinatura.setDataInicio(dto.getDataInicio());
        assinatura.setDataVencimento(dto.getDataVencimento());
        assinatura.setSessoesContratadas(dto.getSessoesContratadas());
        assinatura.setValor(dto.getValor());
        assinatura.setObservacoes(dto.getObservacoes());
        return assinatura;
    }

    public AssinaturaResponseDTO toResponseDTO(Assinatura entity) {
        AssinaturaResponseDTO dto = new AssinaturaResponseDTO();
        dto.setId(entity.getId());
        dto.setPacienteId(entity.getPaciente().getId());
        dto.setPacienteNome(entity.getPaciente().getNomeCompleto());
        dto.setServicoId(entity.getServico().getId());
        dto.setServicoDescricao(
                entity.getServico().getAtividade().getNome() + " - " + entity.getServico().getPlano().getNome()
        );
        dto.setDataInicio(entity.getDataInicio());
        dto.setDataVencimento(entity.getDataVencimento());
        dto.setSessoesContratadas(entity.getSessoesContratadas());
        dto.setSessoesRealizadas(entity.getSessoesRealizadas());
        dto.setSessoesRestantes(entity.getSessoesContratadas() - entity.getSessoesRealizadas());
        dto.setStatus(entity.getStatus());
        dto.setValor(entity.getValor());
        dto.setObservacoes(entity.getObservacoes());
        dto.setAtivo(entity.isAtivo());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    public void updateEntityFromDto(AssinaturaUpdateDTO dto, Assinatura entity) {
        if (dto.getDataVencimento() != null) {
            entity.setDataVencimento(dto.getDataVencimento());
        }
        if (dto.getSessoesContratadas() != null) {
            entity.setSessoesContratadas(dto.getSessoesContratadas());
        }
        if (dto.getValor() != null) {
            entity.setValor(dto.getValor());
        }
        if (dto.getObservacoes() != null) {
            entity.setObservacoes(dto.getObservacoes());
        }
    }
}
