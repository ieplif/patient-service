package br.com.clinicahumaniza.patient_service.dto;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HorarioDisponivelResponseDTO {

    private UUID id;
    private UUID profissionalId;
    private String profissionalNome;
    private DayOfWeek diaSemana;
    private LocalTime horaInicio;
    private LocalTime horaFim;
    private boolean ativo;
    private LocalDateTime createdAt;
}
