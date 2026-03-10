package br.com.clinicahumaniza.patient_service.dto;

import br.com.clinicahumaniza.patient_service.model.StatusParcela;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParcelaResponseDTO {

    private UUID id;
    private Integer numero;
    private BigDecimal valor;
    private LocalDate dataVencimento;
    private LocalDateTime dataPagamento;
    private StatusParcela status;
}
