package br.com.clinicahumaniza.patient_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientExportDTO {

    private UUID id;
    private String nomeCompleto;
    private String cpf;
    private String email;
    private String telefone;
    private LocalDate dataNascimento;
    private String endereco;
    private String estadoCivil;
    private String profissao;
    private String nomeMedicoResponsavel;
    private boolean consentimentoLgpd;
    private LocalDateTime dataConsentimentoLgpd;
    private LocalDateTime createdAt;

    private List<AssinaturaResumoDTO> assinaturas;
    private List<AgendamentoResumoDTO> agendamentos;
    private List<PagamentoResumoDTO> pagamentos;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssinaturaResumoDTO {
        private UUID id;
        private String servico;
        private String status;
        private LocalDate dataInicio;
        private LocalDate dataVencimento;
        private BigDecimal valor;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgendamentoResumoDTO {
        private UUID id;
        private String servico;
        private String profissional;
        private LocalDateTime dataHora;
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PagamentoResumoDTO {
        private UUID id;
        private BigDecimal valor;
        private String status;
        private String formaPagamento;
        private LocalDate dataVencimento;
        private LocalDateTime dataPagamento;
    }
}
