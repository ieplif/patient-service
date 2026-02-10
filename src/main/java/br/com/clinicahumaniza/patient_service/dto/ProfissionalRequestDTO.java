package br.com.clinicahumaniza.patient_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfissionalRequestDTO {

    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 3, max = 255, message = "Nome deve ter entre 3 e 255 caracteres")
    private String nome;

    @NotBlank(message = "Telefone é obrigatório")
    @Size(min = 10, max = 20, message = "Telefone deve ter entre 10 e 20 caracteres")
    private String telefone;

    @NotBlank(message = "E-mail é obrigatório")
    @Email(message = "E-mail deve ser válido")
    private String email;

    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres")
    private String senha;

    @NotEmpty(message = "Pelo menos uma atividade é obrigatória")
    private Set<UUID> atividadeIds;
}
