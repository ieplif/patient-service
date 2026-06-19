package br.com.clinicahumaniza.patient_service.spec;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.criteria.JoinType;

import org.springframework.data.jpa.domain.Specification;

import br.com.clinicahumaniza.patient_service.model.Agendamento;
import br.com.clinicahumaniza.patient_service.model.StatusAgendamento;

public class AgendamentoSpecification {

    public static Specification<Agendamento> hasStatus(StatusAgendamento status) {
        return (root, query, cb) -> {
            if (status == null) return null;
            return cb.equal(root.get("status"), status);
        };
    }

    public static Specification<Agendamento> hasPaciente(UUID pacienteId) {
        return (root, query, cb) -> {
            if (pacienteId == null) return null;
            return cb.equal(root.get("paciente").get("id"), pacienteId);
        };
    }

    public static Specification<Agendamento> hasPacienteNome(String nome) {
        return (root, query, cb) -> {
            if (nome == null || nome.isBlank()) return null;
            return cb.like(cb.lower(root.get("paciente").get("nomeCompleto")), "%" + nome.toLowerCase() + "%");
        };
    }

    public static Specification<Agendamento> hasProfissional(UUID profissionalId) {
        return (root, query, cb) -> {
            if (profissionalId == null) return null;
            return cb.equal(root.get("profissional").get("id"), profissionalId);
        };
    }

    public static Specification<Agendamento> hasAssinatura(UUID assinaturaId) {
        return (root, query, cb) -> {
            if (assinaturaId == null) return null;
            return cb.equal(root.get("assinatura").get("id"), assinaturaId);
        };
    }

    public static Specification<Agendamento> betweenDatas(LocalDateTime inicio, LocalDateTime fim) {
        return (root, query, cb) -> {
            if (inicio == null && fim == null) return null;
            if (inicio == null) return cb.lessThanOrEqualTo(root.get("dataHora"), fim);
            if (fim == null) return cb.greaterThanOrEqualTo(root.get("dataHora"), inicio);
            return cb.between(root.get("dataHora"), inicio, fim);
        };
    }

    /**
     * Oculta agendamentos confidenciais: os da atividade {@code atividadeNome} atendidos
     * pelo profissional cujo usuário tem e-mail {@code profissionalEmail}. Aplicado apenas
     * quando {@code aplicar} é true (ou seja, para perfis que não devem ver esses registros).
     *
     * Mantém a linha quando NÃO for (atividade confidencial E profissional confidencial).
     * O {@code isNull} no profissional preserva agendamentos sem profissional vinculado, e
     * o LEFT JOIN evita descartar essas linhas.
     */
    public static Specification<Agendamento> ocultarConfidencial(
            boolean aplicar, String atividadeNome, String profissionalEmail) {
        return (root, query, cb) -> {
            if (!aplicar
                    || atividadeNome == null
                    || atividadeNome.isBlank()
                    || profissionalEmail == null
                    || profissionalEmail.isBlank()) {
                return null;
            }
            var profJoin = root.join("profissional", JoinType.LEFT);
            var userJoin = profJoin.join("user", JoinType.LEFT);
            return cb.or(
                    cb.notEqual(
                            cb.lower(root.get("servico").get("atividade").get("nome")), atividadeNome.toLowerCase()),
                    profJoin.get("id").isNull(),
                    cb.notEqual(cb.lower(userJoin.get("email")), profissionalEmail.toLowerCase()));
        };
    }
}
