package br.com.clinicahumaniza.patient_service.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReposicaoRequestDTO {

    @NotNull(message = "ID do agendamento de origem é obrigatório")
    private UUID agendamentoOrigemId;

    private UUID profissionalId; // opcional — Pilates não tem profissional fixo

    @NotNull(message = "Data e hora são obrigatórios")
    private LocalDateTime dataHora;

    private Integer duracaoMinutos;

    private String observacoes;
}
