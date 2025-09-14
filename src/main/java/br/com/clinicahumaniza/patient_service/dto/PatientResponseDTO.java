package br.com.clinicahumaniza.patient_service.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

// DTO para enviar dados como resposta da API.
// Apenas os campos que o cliente pode ver. Note que não incluímos o CPF aqui por segurança.
@Data
public class PatientResponseDTO {
    private UUID id;
    private String nomeCompleto;
    private String email;
    private LocalDate dataNascimento;
    private String telefone;
    private boolean statusAtivo;
    private LocalDateTime createdAt;
}
