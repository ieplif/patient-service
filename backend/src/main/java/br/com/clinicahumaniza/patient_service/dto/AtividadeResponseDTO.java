package br.com.clinicahumaniza.patient_service.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AtividadeResponseDTO {

    private UUID id;
    private String nome;
    private String descricao;
    private Integer duracaoPadrao;
    private Integer capacidadeMaxima;
    private boolean ativo;
    private LocalDateTime createdAt;
}
