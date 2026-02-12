package br.com.clinicahumaniza.patient_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AtividadeRequestDTO {

    @NotBlank(message = "Nome é obrigatório")
    private String nome;

    private String descricao;

    @Min(value = 1, message = "Duração padrão deve ser no mínimo 1 minuto")
    private Integer duracaoPadrao;

    @Min(value = 1, message = "Capacidade máxima deve ser no mínimo 1")
    private Integer capacidadeMaxima = 1;
}
