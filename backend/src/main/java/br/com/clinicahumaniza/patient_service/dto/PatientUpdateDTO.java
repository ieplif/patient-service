package br.com.clinicahumaniza.patient_service.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PatientUpdateDTO {

    @Size(min = 3, max = 255, message = "Nome completo deve ter entre 3 e 255 caracteres")
    private String nomeCompleto;

    @Size(min = 10, max = 20, message = "Telefone deve ter entre 10 e 20 caracteres")
    private String telefone;

    private String endereco;

    @Size(max = 100, message = "Profissão deve ter no máximo 100 caracteres")
    private String profissao;

    @Size(max = 50, message = "Estado civil deve ter no máximo 50 caracteres")
    private String estadoCivil;

    private Boolean statusAtivo;
    private Boolean consentimentoLgpd;
}
