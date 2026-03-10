package br.com.clinicahumaniza.patient_service.dto;

import jakarta.validation.constraints.DecimalMin;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServicoUpdateDTO {

    private UUID atividadeId;
    private UUID planoId;
    private String tipoAtendimento;
    private Integer quantidade;
    private String unidadeServico;
    private String modalidadeLocal;

    @DecimalMin(value = "0.01", message = "Valor deve ser no mínimo R$ 0,01")
    private BigDecimal valor;
}
