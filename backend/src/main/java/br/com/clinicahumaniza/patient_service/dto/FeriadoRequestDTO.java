package br.com.clinicahumaniza.patient_service.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeriadoRequestDTO {

    @NotNull(message = "Data é obrigatória")
    private LocalDate data;

    private String descricao;

    private boolean recorrente;
}
