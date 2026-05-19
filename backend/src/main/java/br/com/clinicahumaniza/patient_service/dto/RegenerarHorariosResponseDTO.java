package br.com.clinicahumaniza.patient_service.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Resultado da regeneração de horários: quantos agendamentos foram cancelados
 * e quantos foram criados, mais a lista de novas datas e datas que não puderam
 * ser agendadas (ex.: feriado, conflito).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegenerarHorariosResponseDTO {
    private int agendamentosCancelados;
    private int agendamentosCriados;
    private List<AgendamentoResponseDTO> novosAgendamentos;
    private List<DataIgnoradaDTO> datasIgnoradas;
}
