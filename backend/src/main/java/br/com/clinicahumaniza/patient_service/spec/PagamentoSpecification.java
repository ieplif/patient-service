package br.com.clinicahumaniza.patient_service.spec;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import br.com.clinicahumaniza.patient_service.model.FormaPagamento;
import br.com.clinicahumaniza.patient_service.model.Pagamento;
import br.com.clinicahumaniza.patient_service.model.StatusPagamento;

public class PagamentoSpecification {

    public static Specification<Pagamento> hasStatus(StatusPagamento status) {
        return (root, query, cb) -> {
            if (status == null) return null;
            return cb.equal(root.get("status"), status);
        };
    }

    public static Specification<Pagamento> hasFormaPagamento(FormaPagamento forma) {
        return (root, query, cb) -> {
            if (forma == null) return null;
            return cb.equal(root.get("formaPagamento"), forma);
        };
    }

    public static Specification<Pagamento> hasPaciente(UUID pacienteId) {
        return (root, query, cb) -> {
            if (pacienteId == null) return null;
            return cb.equal(root.get("paciente").get("id"), pacienteId);
        };
    }

    public static Specification<Pagamento> hasPacienteNome(String nome) {
        return (root, query, cb) -> {
            if (nome == null || nome.isBlank()) return null;
            return cb.like(cb.lower(root.get("paciente").get("nomeCompleto")), "%" + nome.toLowerCase() + "%");
        };
    }

    public static Specification<Pagamento> betweenVencimento(LocalDate inicio, LocalDate fim) {
        return (root, query, cb) -> {
            if (inicio == null && fim == null) return null;
            if (inicio == null) return cb.lessThanOrEqualTo(root.get("dataVencimento"), fim);
            if (fim == null) return cb.greaterThanOrEqualTo(root.get("dataVencimento"), inicio);
            return cb.between(root.get("dataVencimento"), inicio, fim);
        };
    }

    /**
     * Filtro por data efetiva do pagamento (dataPagamento) — útil para o card
     * "Receita do mês", relatórios contábeis, etc. Compara com LocalDateTime
     * usando o início do dia para inicio e o fim do dia para fim.
     */
    public static Specification<Pagamento> betweenDataPagamento(LocalDate inicio, LocalDate fim) {
        return (root, query, cb) -> {
            if (inicio == null && fim == null) return null;
            if (inicio == null) {
                return cb.lessThanOrEqualTo(root.get("dataPagamento"), fim.atTime(LocalTime.MAX));
            }
            if (fim == null) {
                return cb.greaterThanOrEqualTo(root.get("dataPagamento"), inicio.atStartOfDay());
            }
            return cb.between(root.get("dataPagamento"), inicio.atStartOfDay(), fim.atTime(LocalTime.MAX));
        };
    }
}
