package br.com.clinicahumaniza.patient_service.mapper;

import br.com.clinicahumaniza.patient_service.dto.ServicoRequestDTO;
import br.com.clinicahumaniza.patient_service.dto.ServicoResponseDTO;
import br.com.clinicahumaniza.patient_service.dto.ServicoUpdateDTO;
import br.com.clinicahumaniza.patient_service.model.Atividade;
import br.com.clinicahumaniza.patient_service.model.Plano;
import br.com.clinicahumaniza.patient_service.model.Servico;
import org.springframework.stereotype.Component;

@Component
public class ServicoMapper {

    public Servico toEntity(ServicoRequestDTO dto, Atividade atividade, Plano plano) {
        Servico servico = new Servico();
        servico.setAtividade(atividade);
        servico.setPlano(plano);
        servico.setTipoAtendimento(dto.getTipoAtendimento());
        servico.setQuantidade(dto.getQuantidade());
        servico.setUnidadeServico(dto.getUnidadeServico());
        servico.setModalidadeLocal(dto.getModalidadeLocal());
        servico.setValor(dto.getValor());
        return servico;
    }

    public ServicoResponseDTO toResponseDTO(Servico entity) {
        ServicoResponseDTO dto = new ServicoResponseDTO();
        dto.setId(entity.getId());
        dto.setAtividadeId(entity.getAtividade().getId());
        dto.setAtividadeNome(entity.getAtividade().getNome());
        dto.setPlanoId(entity.getPlano().getId());
        dto.setPlanoNome(entity.getPlano().getNome());
        dto.setTipoAtendimento(entity.getTipoAtendimento());
        dto.setQuantidade(entity.getQuantidade());
        dto.setUnidadeServico(entity.getUnidadeServico());
        dto.setModalidadeLocal(entity.getModalidadeLocal());
        dto.setValor(entity.getValor());
        dto.setAtivo(entity.isAtivo());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    public void updateEntityFromDto(ServicoUpdateDTO dto, Servico entity, Atividade atividade, Plano plano) {
        if (atividade != null) {
            entity.setAtividade(atividade);
        }
        if (plano != null) {
            entity.setPlano(plano);
        }
        if (dto.getTipoAtendimento() != null) {
            entity.setTipoAtendimento(dto.getTipoAtendimento());
        }
        if (dto.getQuantidade() != null) {
            entity.setQuantidade(dto.getQuantidade());
        }
        if (dto.getUnidadeServico() != null) {
            entity.setUnidadeServico(dto.getUnidadeServico());
        }
        if (dto.getModalidadeLocal() != null) {
            entity.setModalidadeLocal(dto.getModalidadeLocal());
        }
        if (dto.getValor() != null) {
            entity.setValor(dto.getValor());
        }
    }
}
