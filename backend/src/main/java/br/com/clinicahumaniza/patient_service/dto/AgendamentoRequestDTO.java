package br.com.clinicahumaniza.patient_service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgendamentoRequestDTO {

    @NotNull(message = "ID do paciente é obrigatório")
    private UUID pacienteId;

    @NotNull(message = "ID do profissional é obrigatório")
    private UUID profissionalId;

    @NotNull(message = "ID do serviço é obrigatório")
    private UUID servicoId;

    private UUID assinaturaId;

    @NotNull(message = "Data e hora são obrigatórios")
    private LocalDateTime dataHora;

    private Integer duracaoMinutos;

    private String observacoes;
}
