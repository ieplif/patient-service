package br.com.clinicahumaniza.patient_service.dto;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import br.com.clinicahumaniza.patient_service.model.FrequenciaRecorrencia;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgendamentoRecorrenteRequestDTO {

    @NotNull
    private UUID pacienteId;

    // Opcional — pode ficar em branco (ex.: Pilates onde o profissional varia por dia)
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

    /**
     * Data a partir da qual começar a gerar agendamentos. Se nulo, usa a
     * dataInicio da assinatura (se vinculada) ou amanhã. Usado pela renovação
     * automática pra forçar geração no próximo período (vencimento + 1).
     */
    private LocalDate dataInicio;

    private LocalDate dataFim;

    private String observacoes;
}
