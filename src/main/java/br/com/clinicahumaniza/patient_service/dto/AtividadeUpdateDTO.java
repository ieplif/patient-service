package br.com.clinicahumaniza.patient_service.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AtividadeUpdateDTO {

    private String nome;
    private String descricao;

    @Min(value = 1, message = "Duração padrão deve ser no mínimo 1 minuto")
    private Integer duracaoPadrao;
}
