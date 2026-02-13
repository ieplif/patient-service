package br.com.clinicahumaniza.patient_service.dto;

import br.com.clinicahumaniza.patient_service.model.FormaPagamento;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PagamentoRequestDTO {

    @NotNull(message = "ID do paciente é obrigatório")
    private UUID pacienteId;

    private UUID assinaturaId;

    private UUID agendamentoId;

    @NotNull(message = "Valor é obrigatório")
    @DecimalMin(value = "0.01", message = "Valor deve ser no mínimo R$ 0,01")
    private BigDecimal valor;

    @NotNull(message = "Forma de pagamento é obrigatória")
    private FormaPagamento formaPagamento;

    @Min(value = 1, message = "Número de parcelas deve ser no mínimo 1")
    private Integer numeroParcelas = 1;

    @NotNull(message = "Data de vencimento é obrigatória")
    private LocalDate dataVencimento;

    private String observacoes;
}
