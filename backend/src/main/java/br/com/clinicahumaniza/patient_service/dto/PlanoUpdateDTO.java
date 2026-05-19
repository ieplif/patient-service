package br.com.clinicahumaniza.patient_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlanoUpdateDTO {

    private String nome;
    private String descricao;
    private String tipoPlano;
    private Integer validadeDias;
    private Integer sessoesIncluidas;
    private Boolean permiteTransferencia;
}
