package br.com.clinicahumaniza.patient_service.spec;

import br.com.clinicahumaniza.patient_service.model.Agendamento;
import br.com.clinicahumaniza.patient_service.model.StatusAgendamento;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.UUID;

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

    public static Specification<Agendamento> hasProfissional(UUID profissionalId) {
        return (root, query, cb) -> {
            if (profissionalId == null) return null;
            return cb.equal(root.get("profissional").get("id"), profissionalId);
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
}
