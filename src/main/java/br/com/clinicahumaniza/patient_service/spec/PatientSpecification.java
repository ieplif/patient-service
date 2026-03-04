package br.com.clinicahumaniza.patient_service.spec;

import br.com.clinicahumaniza.patient_service.model.Patient;
import org.springframework.data.jpa.domain.Specification;

public class PatientSpecification {

    public static Specification<Patient> hasNome(String nome) {
        return (root, query, cb) -> {
            if (nome == null || nome.isBlank()) return null;
            return cb.like(cb.lower(root.get("nomeCompleto")), "%" + nome.toLowerCase() + "%");
        };
    }

    public static Specification<Patient> hasEmail(String email) {
        return (root, query, cb) -> {
            if (email == null || email.isBlank()) return null;
            return cb.like(cb.lower(root.get("email")), "%" + email.toLowerCase() + "%");
        };
    }

    public static Specification<Patient> hasCpf(String cpf) {
        return (root, query, cb) -> {
            if (cpf == null || cpf.isBlank()) return null;
            return cb.equal(root.get("cpf"), cpf);
        };
    }
}
