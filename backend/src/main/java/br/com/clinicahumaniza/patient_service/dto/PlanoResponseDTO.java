package br.com.clinicahumaniza.patient_service.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlanoResponseDTO {

    private UUID id;
    private String nome;
    private String descricao;
    private String tipoPlano;
    private Integer validadeDias;
    private Integer sessoesIncluidas;
    private boolean permiteTransferencia;
    private boolean ativo;
    private LocalDateTime createdAt;
}
