package br.com.clinicahumaniza.patient_service.mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import br.com.clinicahumaniza.patient_service.dto.PagamentoRequestDTO;
import br.com.clinicahumaniza.patient_service.dto.PagamentoResponseDTO;
import br.com.clinicahumaniza.patient_service.dto.PagamentoUpdateDTO;
import br.com.clinicahumaniza.patient_service.dto.ParcelaResponseDTO;
import br.com.clinicahumaniza.patient_service.model.Agendamento;
import br.com.clinicahumaniza.patient_service.model.Assinatura;
import br.com.clinicahumaniza.patient_service.model.Pagamento;
import br.com.clinicahumaniza.patient_service.model.Parcela;
import br.com.clinicahumaniza.patient_service.model.Patient;

@Component
public class PagamentoMapper {

    public Pagamento toEntity(
            PagamentoRequestDTO dto, Patient paciente, List<Assinatura> assinaturas, Agendamento agendamento) {
        Pagamento pagamento = new Pagamento();
        pagamento.setPaciente(paciente);
        pagamento.setAssinaturas(assinaturas != null ? assinaturas : new ArrayList<>());
        pagamento.setAgendamento(agendamento);
        pagamento.setValor(dto.getValor());
        pagamento.setFormaPagamento(dto.getFormaPagamento());
        pagamento.setNumeroParcelas(dto.getNumeroParcelas() != null ? dto.getNumeroParcelas() : 1);
        pagamento.setDataVencimento(dto.getDataVencimento());
        pagamento.setObservacoes(dto.getObservacoes());
        return pagamento;
    }

    public PagamentoResponseDTO toResponseDTO(Pagamento entity) {
        PagamentoResponseDTO dto = new PagamentoResponseDTO();
        dto.setId(entity.getId());
        dto.setPacienteId(entity.getPaciente().getId());
        dto.setPacienteNome(entity.getPaciente().getNomeCompleto());

        if (entity.getAssinaturas() != null && !entity.getAssinaturas().isEmpty()) {
            dto.setAssinaturaIds(
                    entity.getAssinaturas().stream().map(Assinatura::getId).collect(Collectors.toList()));
            dto.setAssinaturaDescricoes(entity.getAssinaturas().stream()
                    .map(a -> a.getServico().getAtividade().getNome() + " - "
                            + a.getServico().getPlano().getNome())
                    .collect(Collectors.toList()));
        }

        if (entity.getAgendamento() != null) {
            dto.setAgendamentoId(entity.getAgendamento().getId());
            dto.setAgendamentoDescricao(
                    entity.getAgendamento().getServico().getAtividade().getNome() + " - "
                            + entity.getAgendamento().getDataHora().toString());
        }

        dto.setValor(entity.getValor());
        dto.setFormaPagamento(entity.getFormaPagamento());
        dto.setStatus(entity.getStatus());
        dto.setNumeroParcelas(entity.getNumeroParcelas());
        dto.setDataPagamento(entity.getDataPagamento());
        dto.setDataVencimento(entity.getDataVencimento());
        dto.setObservacoes(entity.getObservacoes());
        dto.setGatewayId(entity.getGatewayId());
        dto.setGatewayStatus(entity.getGatewayStatus());
        dto.setAtivo(entity.isAtivo());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        if (entity.getParcelas() != null) {
            List<ParcelaResponseDTO> parcelas = entity.getParcelas().stream()
                    .map(this::toParcelaResponseDTO)
                    .collect(Collectors.toList());
            dto.setParcelas(parcelas);
        }

        return dto;
    }

    public ParcelaResponseDTO toParcelaResponseDTO(Parcela parcela) {
        ParcelaResponseDTO dto = new ParcelaResponseDTO();
        dto.setId(parcela.getId());
        dto.setNumero(parcela.getNumero());
        dto.setValor(parcela.getValor());
        dto.setDataVencimento(parcela.getDataVencimento());
        dto.setDataPagamento(parcela.getDataPagamento());
        dto.setStatus(parcela.getStatus());
        return dto;
    }

    public void updateEntityFromDto(PagamentoUpdateDTO dto, Pagamento entity) {
        if (dto.getFormaPagamento() != null) {
            entity.setFormaPagamento(dto.getFormaPagamento());
        }
        if (dto.getDataVencimento() != null) {
            entity.setDataVencimento(dto.getDataVencimento());
        }
        if (dto.getObservacoes() != null) {
            entity.setObservacoes(dto.getObservacoes());
        }
    }
}
