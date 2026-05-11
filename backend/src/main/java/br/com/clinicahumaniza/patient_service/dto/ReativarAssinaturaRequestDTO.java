package br.com.clinicahumaniza.patient_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Reativa uma assinatura SUSPENSO. Volta o status para ATIVO, reseta a janela
 * de validade (hoje + plano.validadeDias) e limpa os campos de suspensão.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReativarAssinaturaRequestDTO {

    /** Data de início ao reativar. Default: hoje. */
    private LocalDate dataInicio;

    /**
     * Se true, recria os agendamentos com os horários fixos antigos.
     * O frontend pode também chamar /regenerar-horarios separadamente
     * informando novos horários — aí esse campo fica false.
     */
    private Boolean recriarAgendamentos;
}
