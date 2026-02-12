package br.com.clinicahumaniza.patient_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgendamentoUpdateDTO {

    private LocalDateTime dataHora;
    private Integer duracaoMinutos;
    private String observacoes;
}
