package br.com.clinicahumaniza.patient_service.dto;

import br.com.clinicahumaniza.patient_service.model.FrequenciaRecorrencia;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgendamentoRecorrenteRequestDTO {

    @NotNull
    private UUID pacienteId;

    @NotNull
    private UUID profissionalId;

    @NotNull
    private UUID servicoId;

    private UUID assinaturaId;

    @NotNull
    private FrequenciaRecorrencia frequencia;

    @NotEmpty
    private List<DayOfWeek> diasSemana;

    @NotNull
    private LocalTime horaInicio;

    private Integer duracaoMinutos;

    @Min(1)
    private Integer totalSessoes;

    private LocalDate dataFim;

    private String observacoes;
}
