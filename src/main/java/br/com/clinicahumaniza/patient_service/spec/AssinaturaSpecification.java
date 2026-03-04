package br.com.clinicahumaniza.patient_service.spec;

import br.com.clinicahumaniza.patient_service.model.Assinatura;
import br.com.clinicahumaniza.patient_service.model.StatusAssinatura;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

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
}
