package br.com.clinicahumaniza.patient_service.dto;

import lombok.Data;
import java.time.LocalDate;

// DTO para receber dados na criação/atualização de um paciente.
// Apenas os campos que o cliente deve fornecer.
@Data
public class PatientRequestDTO {
    private String nomeCompleto;
    private String email;
    private String cpf;
    private LocalDate dataNascimento;
    private String telefone;
    private String endereco;
    private String profissao;
    private String estadoCivil;
    private Boolean consentimentoLgpd;
}
