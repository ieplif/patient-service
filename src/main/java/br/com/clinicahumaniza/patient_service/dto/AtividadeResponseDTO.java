package br.com.clinicahumaniza.patient_service.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AtividadeResponseDTO {

    private UUID id;
    private String nome;
    private String descricao;
    private Integer duracaoPadrao;
    private boolean ativo;
    private LocalDateTime createdAt;
}
