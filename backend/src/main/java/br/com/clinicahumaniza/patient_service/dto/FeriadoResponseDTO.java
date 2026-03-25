package br.com.clinicahumaniza.patient_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeriadoResponseDTO {

    private UUID id;
    private LocalDate data;
    private String descricao;
    private boolean recorrente;
    private LocalDateTime createdAt;
}
