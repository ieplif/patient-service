package br.com.clinicahumaniza.patient_service.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServicoResponseDTO {

    private UUID id;
    private UUID atividadeId;
    private String atividadeNome;
    private UUID planoId;
    private String planoNome;
    private String tipoAtendimento;
    private Integer quantidade;
    private String unidadeServico;
    private String modalidadeLocal;
    private BigDecimal valor;
    private boolean ativo;
    private LocalDateTime createdAt;
}
