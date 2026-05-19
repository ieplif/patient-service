package br.com.clinicahumaniza.patient_service.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DataIgnoradaDTO {

    private LocalDate data;
    private String motivo;
}
