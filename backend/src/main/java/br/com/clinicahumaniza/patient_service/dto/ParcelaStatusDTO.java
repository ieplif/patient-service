package br.com.clinicahumaniza.patient_service.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;

import br.com.clinicahumaniza.patient_service.model.StatusParcela;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParcelaStatusDTO {

    @NotNull(message = "Status é obrigatório")
    private StatusParcela status;

    private LocalDateTime dataPagamento;
}
