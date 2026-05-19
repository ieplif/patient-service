package br.com.clinicahumaniza.patient_service.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import br.com.clinicahumaniza.patient_service.model.StatusAssinatura;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssinaturaResponseDTO {

    private UUID id;
    private UUID pacienteId;
    private String pacienteNome;
    private UUID servicoId;
    private String servicoDescricao;
    private LocalDate dataInicio;
    private LocalDate dataVencimento;
    private Integer sessoesContratadas;
    private Integer sessoesRealizadas;
    private Integer sessoesRestantes;
    private StatusAssinatura status;
    private BigDecimal valor;
    private String observacoes;
    private boolean renovacaoAutomatica;
    private LocalDate dataSuspensao;
    private String motivoSuspensao;
    private LocalDate dataPrevistaRetomada;
    private boolean ativo;
    private LocalDateTime createdAt;
}
