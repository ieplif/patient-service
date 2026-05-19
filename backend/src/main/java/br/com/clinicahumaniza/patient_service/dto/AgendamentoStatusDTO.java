package br.com.clinicahumaniza.patient_service.dto;

import jakarta.validation.constraints.NotNull;

import br.com.clinicahumaniza.patient_service.model.StatusAgendamento;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgendamentoStatusDTO {

    @NotNull(message = "Status é obrigatório")
    private StatusAgendamento status;

    private String motivoCancelamento;

    /**
     * Override opcional para o cálculo automático de direito a reposição
     * quando o status novo é CANCELADO. Quando informado, força o valor.
     * Quando null, usa a regra padrão (Pilates + não-feriado + não-reposição).
     */
    private Boolean gerarReposicao;
}
