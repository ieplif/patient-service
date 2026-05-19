package br.com.clinicahumaniza.patient_service.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import br.com.clinicahumaniza.patient_service.validation.ValidCpf;
import lombok.Data;

@Data
public class PatientUpdateDTO {

    @Size(min = 3, max = 255, message = "Nome completo deve ter entre 3 e 255 caracteres")
    private String nomeCompleto;

    @Email(message = "E-mail deve ser válido")
    private String email;

    @ValidCpf
    private String cpf;

    @Past(message = "Data de nascimento deve ser uma data no passado")
    private LocalDate dataNascimento;

    @Size(min = 10, max = 20, message = "Telefone deve ter entre 10 e 20 caracteres")
    private String telefone;

    private String endereco;

    @Size(max = 100, message = "Profissão deve ter no máximo 100 caracteres")
    private String profissao;

    @Size(max = 50, message = "Estado civil deve ter no máximo 50 caracteres")
    private String estadoCivil;

    @Size(max = 150, message = "Médico(a) responsável deve ter no máximo 150 caracteres")
    private String medicoResponsavel;

    private Boolean statusAtivo;
    private Boolean consentimentoLgpd;
}
