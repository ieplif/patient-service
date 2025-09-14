package br.com.clinicahumaniza.patient_service.repository;

import br.com.clinicahumaniza.patient_service.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository // 1. (Opcional, mas boa prática) Indica ao Spring que esta é uma interface de repositório.
public interface PatientRepository extends JpaRepository<Patient, UUID> { // 2. A mágica acontece aqui!

    // 3. O Spring Data JPA criará automaticamente a implementação para este método.
    //    Ele entende "findByCpf" como "SELECT * FROM patients WHERE cpf = ?".
    Patient findByCpf(String cpf);

    //    Da mesma forma, ele entende "findByEmail".
    Patient findByEmail(String email);

}
