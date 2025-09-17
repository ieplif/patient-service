package br.com.clinicahumaniza.patient_service.dto;

import lombok.Data;

// DTO para receber dados na ATUALIZAÇÃO de um paciente.
// Campos que podem ser nulos se o cliente não quiser atualizá-los.
@Data
public class PatientUpdateDTO {
    private String nomeCompleto;
    private String telefone;
    private String endereco;
    private String profissao;
    private String estadoCivil;
    private Boolean statusAtivo;
    private Boolean consentimentoLgpd;
    // Note que não incluímos email ou cpf, pois geralmente são campos imutáveis.
    // Se a atualização de email fosse permitida, ela teria regras de negócio próprias (verificar duplicidade).
}
