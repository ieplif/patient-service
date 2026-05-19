package br.com.clinicahumaniza.patient_service.dto;

import jakarta.validation.constraints.NotNull;

import br.com.clinicahumaniza.patient_service.model.StatusAssinatura;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssinaturaStatusDTO {

    @NotNull(message = "Status é obrigatório")
    private StatusAssinatura status;
}
