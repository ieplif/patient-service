package br.com.clinicahumaniza.patient_service.dto;

import br.com.clinicahumaniza.patient_service.model.StatusAgendamento;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgendamentoResponseDTO {

    private UUID id;
    private UUID pacienteId;
    private String pacienteNome;
    private UUID profissionalId;
    private String profissionalNome;
    private UUID servicoId;
    private String servicoDescricao;
    private UUID assinaturaId;
    private LocalDateTime dataHora;
    private Integer duracaoMinutos;
    private StatusAgendamento status;
    private String observacoes;
    private boolean ativo;
    private LocalDateTime createdAt;
}
