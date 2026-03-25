package br.com.clinicahumaniza.patient_service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeriadoRequestDTO {

    @NotNull(message = "Data é obrigatória")
    private LocalDate data;

    private String descricao;

    private boolean recorrente;
}
