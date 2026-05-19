package br.com.clinicahumaniza.patient_service.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Suspende uma assinatura ATIVO. Cancela agendamentos futuros pendentes
 * (AGENDADO/CONFIRMADO) e desativa templates recorrentes. Saldo de sessões
 * preservado para retomada futura.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SuspenderAssinaturaRequestDTO {

    @NotBlank(message = "Motivo da suspensão é obrigatório")
    private String motivo;

    /** Data prevista para retomada (opcional, só pra controle). */
    private LocalDate dataPrevistaRetomada;
}
