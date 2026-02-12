package br.com.clinicahumaniza.patient_service.mapper;

import br.com.clinicahumaniza.patient_service.dto.AgendamentoRequestDTO;
import br.com.clinicahumaniza.patient_service.dto.AgendamentoResponseDTO;
import br.com.clinicahumaniza.patient_service.dto.AgendamentoUpdateDTO;
import br.com.clinicahumaniza.patient_service.model.*;
import org.springframework.stereotype.Component;

@Component
public class AgendamentoMapper {

    public Agendamento toEntity(AgendamentoRequestDTO dto, Patient paciente,
                                 Profissional profissional, Servico servico,
                                 Assinatura assinatura) {
        Agendamento agendamento = new Agendamento();
        agendamento.setPaciente(paciente);
        agendamento.setProfissional(profissional);
        agendamento.setServico(servico);
        agendamento.setAssinatura(assinatura);
        agendamento.setDataHora(dto.getDataHora());
        agendamento.setDuracaoMinutos(dto.getDuracaoMinutos());
        agendamento.setObservacoes(dto.getObservacoes());
        return agendamento;
    }

    public AgendamentoResponseDTO toResponseDTO(Agendamento entity) {
        AgendamentoResponseDTO dto = new AgendamentoResponseDTO();
        dto.setId(entity.getId());
        dto.setPacienteId(entity.getPaciente().getId());
        dto.setPacienteNome(entity.getPaciente().getNomeCompleto());
        dto.setProfissionalId(entity.getProfissional().getId());
        dto.setProfissionalNome(entity.getProfissional().getNome());
        dto.setServicoId(entity.getServico().getId());
        dto.setServicoDescricao(
                entity.getServico().getAtividade().getNome() + " - " + entity.getServico().getPlano().getNome()
        );
        if (entity.getAssinatura() != null) {
            dto.setAssinaturaId(entity.getAssinatura().getId());
        }
        dto.setDataHora(entity.getDataHora());
        dto.setDuracaoMinutos(entity.getDuracaoMinutos());
        dto.setStatus(entity.getStatus());
        dto.setObservacoes(entity.getObservacoes());
        dto.setAtivo(entity.isAtivo());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    public void updateEntityFromDto(AgendamentoUpdateDTO dto, Agendamento entity) {
        if (dto.getDataHora() != null) entity.setDataHora(dto.getDataHora());
        if (dto.getDuracaoMinutos() != null) entity.setDuracaoMinutos(dto.getDuracaoMinutos());
        if (dto.getObservacoes() != null) entity.setObservacoes(dto.getObservacoes());
    }
}
