package br.com.clinicahumaniza.patient_service.dto;

import br.com.clinicahumaniza.patient_service.model.FrequenciaRecorrencia;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgendamentoRecorrenteResponseDTO {

    private UUID id;
    private UUID pacienteId;
    private String pacienteNome;
    private UUID profissionalId;
    private String profissionalNome;
    private UUID servicoId;
    private String servicoDescricao;
    private UUID assinaturaId;
    private FrequenciaRecorrencia frequencia;
    private List<DayOfWeek> diasSemana;
    private LocalTime horaInicio;
    private Integer duracaoMinutos;
    private Integer totalSessoes;
    private LocalDate dataFim;
    private String observacoes;
    private List<AgendamentoResponseDTO> agendamentosCriados;
    private List<DataIgnoradaDTO> datasIgnoradas;
    private LocalDateTime createdAt;
}
