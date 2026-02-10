package br.com.clinicahumaniza.patient_service.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfissionalUpdateDTO {

    @Size(min = 3, max = 255, message = "Nome deve ter entre 3 e 255 caracteres")
    private String nome;

    @Size(min = 10, max = 20, message = "Telefone deve ter entre 10 e 20 caracteres")
    private String telefone;

    private Set<UUID> atividadeIds;
}
