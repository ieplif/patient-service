package br.com.clinicahumaniza.patient_service.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssinaturaUpdateDTO {

    private LocalDate dataVencimento;

    @Min(value = 1, message = "Sessões contratadas deve ser no mínimo 1")
    private Integer sessoesContratadas;

    @DecimalMin(value = "0.01", message = "Valor deve ser no mínimo R$ 0,01")
    private BigDecimal valor;

    private String observacoes;
}
