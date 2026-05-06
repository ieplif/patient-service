package br.com.clinicahumaniza.patient_service.dto;

import br.com.clinicahumaniza.patient_service.model.StatusPagamento;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

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
