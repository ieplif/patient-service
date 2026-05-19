package br.com.clinicahumaniza.patient_service.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

import br.com.clinicahumaniza.patient_service.model.StatusPagamento;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PagamentoStatusDTO {

    @NotNull(message = "Status é obrigatório")
    private StatusPagamento status;

    /**
     * Data em que o pagamento foi efetivamente realizado.
     * Quando informada e o status novo for PAGO, sobrescreve a data padrão (hoje).
     * Útil para lançamentos retroativos (ex.: registrar uma mensalidade que foi
     * paga semana passada).
     */
    private LocalDate dataPagamento;
}
