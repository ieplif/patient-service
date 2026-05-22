package br.com.clinicahumaniza.patient_service.spec;

import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import br.com.clinicahumaniza.patient_service.model.Assinatura;
import br.com.clinicahumaniza.patient_service.model.StatusAssinatura;

public class AssinaturaSpecification {

    public static Specification<Assinatura> hasStatus(StatusAssinatura status) {
        return (root, query, cb) -> {
            if (status == null) return null;
            return cb.equal(root.get("status"), status);
        };
    }

    public static Specification<Assinatura> hasPaciente(UUID pacienteId) {
        return (root, query, cb) -> {
            if (pacienteId == null) return null;
            return cb.equal(root.get("paciente").get("id"), pacienteId);
        };
    }

    public static Specification<Assinatura> hasPacienteNome(String nome) {
        return (root, query, cb) -> {
            if (nome == null || nome.isBlank()) return null;
            return cb.like(cb.lower(root.get("paciente").get("nomeCompleto")), "%" + nome.toLowerCase() + "%");
        };
    }
}
