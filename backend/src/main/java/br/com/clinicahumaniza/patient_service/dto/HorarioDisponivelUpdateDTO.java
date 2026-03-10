package br.com.clinicahumaniza.patient_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HorarioDisponivelUpdateDTO {

    private DayOfWeek diaSemana;
    private LocalTime horaInicio;
    private LocalTime horaFim;
}
