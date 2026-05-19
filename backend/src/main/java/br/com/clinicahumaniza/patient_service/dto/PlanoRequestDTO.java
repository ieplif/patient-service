package br.com.clinicahumaniza.patient_service.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlanoRequestDTO {

    @NotBlank(message = "Nome é obrigatório")
    private String nome;

    private String descricao;

    @NotBlank(message = "Tipo do plano é obrigatório")
    private String tipoPlano;

    private Integer validadeDias;
    private Integer sessoesIncluidas;
    private Boolean permiteTransferencia;
}
