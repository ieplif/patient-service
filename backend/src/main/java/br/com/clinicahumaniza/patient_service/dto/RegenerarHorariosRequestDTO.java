package br.com.clinicahumaniza.patient_service.dto;

import jakarta.validation.Valid;
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

/**
 * Solicita a regeneração dos horários fixos de uma assinatura:
 * cancela agendamentos futuros (status AGENDADO/CONFIRMADO) e cria novos
 * a partir dos slots informados.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegenerarHorariosRequestDTO {

    @NotEmpty(message = "Informe ao menos um horário fixo")
    @Valid
    private List<HorarioFixoDTO> horariosFixos;

    /** Profissional opcional — quando vazio, agendamentos ficam como "Sem profissional". */
    private UUID profissionalId;

    /** Default: amanhã. Agendamentos antes dessa data não são tocados. */
    private LocalDate dataInicioRegeneracao;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HorarioFixoDTO {
        @NotNull
        private DayOfWeek diaSemana;

        @NotNull
        private LocalTime horaInicio;
    }
}
