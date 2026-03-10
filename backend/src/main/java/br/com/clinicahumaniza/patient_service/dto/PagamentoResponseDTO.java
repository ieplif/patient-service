package br.com.clinicahumaniza.patient_service.dto;

import br.com.clinicahumaniza.patient_service.model.FormaPagamento;
import br.com.clinicahumaniza.patient_service.model.StatusPagamento;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PagamentoResponseDTO {

    private UUID id;
    private UUID pacienteId;
    private String pacienteNome;
    private UUID assinaturaId;
    private String assinaturaDescricao;
    private UUID agendamentoId;
    private String agendamentoDescricao;
    private BigDecimal valor;
    private FormaPagamento formaPagamento;
    private StatusPagamento status;
    private Integer numeroParcelas;
    private LocalDateTime dataPagamento;
    private LocalDate dataVencimento;
    private String observacoes;
    private String gatewayId;
    private String gatewayStatus;
    private boolean ativo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ParcelaResponseDTO> parcelas;
}
