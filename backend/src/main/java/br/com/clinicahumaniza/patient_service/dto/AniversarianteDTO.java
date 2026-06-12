package br.com.clinicahumaniza.patient_service.dto;

import java.time.LocalDate;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Paciente aniversariante (sem idade — só nome e data, para o card do Dashboard). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AniversarianteDTO {

    private UUID id;
    private String nomeCompleto;
    private LocalDate dataNascimento;
}
