package br.com.clinicahumaniza.patient_service.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfissionalResponseDTO {

    private UUID id;
    private String nome;
    private String telefone;
    private String email;
    private List<AtividadeSimpleDTO> atividades;
    private String googleCalendarId;
    private boolean ativo;
    private LocalDateTime createdAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AtividadeSimpleDTO {
        private UUID id;
        private String nome;
    }
}
