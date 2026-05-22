package br.com.clinicahumaniza.patient_service.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import br.com.clinicahumaniza.patient_service.model.FormaPagamento;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PagamentoUpdateDTO {

    private UUID pacienteId;

    private List<UUID> assinaturaIds;

    private UUID agendamentoId;

    private BigDecimal valor;

    private FormaPagamento formaPagamento;

    private Integer numeroParcelas;

    private LocalDate dataVencimento;

    private String observacoes;
}
