package br.com.clinicahumaniza.patient_service.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import br.com.clinicahumaniza.patient_service.model.Patient;

@Repository // 1. (Opcional, mas boa prática) Indica ao Spring que esta é uma interface de repositório.
public interface PatientRepository
        extends JpaRepository<Patient, UUID>, JpaSpecificationExecutor<Patient> { // 2. A mágica acontece aqui!

    // 3. O Spring Data JPA criará automaticamente a implementação para este método.
    //    Ele entende "findByCpf" como "SELECT * FROM patients WHERE cpf = ?".
    Optional<Patient> findByCpf(String cpf);

    Optional<Patient> findByEmail(String email);
}
