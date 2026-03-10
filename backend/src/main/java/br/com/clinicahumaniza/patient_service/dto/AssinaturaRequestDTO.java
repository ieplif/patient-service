package br.com.clinicahumaniza.patient_service.dto;

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
public class AssinaturaRequestDTO {

    @NotNull(message = "ID do paciente é obrigatório")
    private UUID pacienteId;

    @NotNull(message = "ID do serviço é obrigatório")
    private UUID servicoId;

    @NotNull(message = "Data de início é obrigatória")
    private LocalDate dataInicio;

    private LocalDate dataVencimento;

    @NotNull(message = "Quantidade de sessões contratadas é obrigatória")
    @Min(value = 1, message = "Sessões contratadas deve ser no mínimo 1")
    private Integer sessoesContratadas;

    @NotNull(message = "Valor é obrigatório")
    @DecimalMin(value = "0.01", message = "Valor deve ser no mínimo R$ 0,01")
    private BigDecimal valor;

    private String observacoes;
}
