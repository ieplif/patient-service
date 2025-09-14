package br.com.clinicahumaniza.patient_service.service;

import br.com.clinicahumaniza.patient_service.model.Patient;
import br.com.clinicahumaniza.patient_service.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service // 1. Anotação que marca esta classe como um componente de serviço do Spring.
public class PatientService {

    private final PatientRepository patientRepository;

    @Autowired // 2. Injeção de dependência: O Spring fornecerá uma instância de PatientRepository.
    public PatientService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    // --- Lógica de Negócio ---

    @Transactional // 3. Garante que a operação inteira seja uma única transação no banco de dados.
    public Patient createPatient(Patient patient) {
        // Regra de Negócio 1: Verificar se o CPF já existe.
        if (patientRepository.findByCpf(patient.getCpf()) != null) {
            throw new IllegalStateException("CPF já cadastrado.");
        }

        // Regra de Negócio 2: Verificar se o E-mail já existe.
        if (patientRepository.findByEmail(patient.getEmail()) != null) {
            throw new IllegalStateException("E-mail já cadastrado.");
        }

        // Se todas as regras passarem, salva o paciente no banco.
        return patientRepository.save(patient);
    }

    @Transactional(readOnly = true) // 4. Otimização para operações que apenas leem dados.
    public List<Patient> getAllPatients() {
        return patientRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Patient> getPatientById(UUID id) {
        return patientRepository.findById(id);
    }

}
