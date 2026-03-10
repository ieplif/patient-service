package br.com.clinicahumaniza.patient_service.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServicoRequestDTO {

    @NotNull(message = "ID da atividade é obrigatório")
    private UUID atividadeId;

    @NotNull(message = "ID do plano é obrigatório")
    private UUID planoId;

    private String tipoAtendimento;
    private Integer quantidade;
    private String unidadeServico;
    private String modalidadeLocal;

    @NotNull(message = "Valor é obrigatório")
    @DecimalMin(value = "0.01", message = "Valor deve ser no mínimo R$ 0,01")
    private BigDecimal valor;
}
