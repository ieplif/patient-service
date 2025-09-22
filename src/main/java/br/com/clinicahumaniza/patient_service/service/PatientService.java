package br.com.clinicahumaniza.patient_service.service;

import br.com.clinicahumaniza.patient_service.model.Patient;
import br.com.clinicahumaniza.patient_service.repository.PatientRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import br.com.clinicahumaniza.patient_service.dto.PatientRequestDTO;
import br.com.clinicahumaniza.patient_service.dto.PatientUpdateDTO;
import br.com.clinicahumaniza.patient_service.mapper.PatientMapper;



@Service
public class PatientService {

    private final PatientRepository patientRepository;
    private final PatientMapper patientMapper; // Injeta o mapper

    @Autowired
    public PatientService(PatientRepository patientRepository, PatientMapper patientMapper) {
        this.patientRepository = patientRepository;
        this.patientMapper = patientMapper;
    }

    @Transactional
    public Patient createPatient(PatientRequestDTO patientDTO) { // Recebe o DTO
        // Converte DTO para Entidade
        Patient patient = patientMapper.toEntity(patientDTO);

        // Regras de negócio continuam usando a entidade
        if (patientRepository.findByCpf(patient.getCpf()) != null) {
            throw new IllegalStateException("CPF já cadastrado.");
        }
        if (patientRepository.findByEmail(patient.getEmail()) != null) {
            throw new IllegalStateException("E-mail já cadastrado.");
        }

        return patientRepository.save(patient);
    }
    // ... outros métodos

    public Optional<Patient> getPatientById(UUID id) {
        return patientRepository.findById(id);
    }

    public List<Patient> getAllPatients() {
        return patientRepository.findAll();
    }

    @Transactional
    public Optional<Patient> updatePatient(UUID id, PatientUpdateDTO patientUpdateDTO) {
        // 1. Encontrar o paciente existente pelo ID.
        Optional<Patient> optionalPatient = patientRepository.findById(id);

        if (optionalPatient.isEmpty()) {
            return Optional.empty(); // Retorna um Optional vazio se o paciente não for encontrado.
        }

        // 2. Obter a entidade do Optional.
        Patient existingPatient = optionalPatient.get();

        // 3. Usar o mapper para aplicar as atualizações do DTO na entidade.
        patientMapper.updateEntityFromDto(patientUpdateDTO, existingPatient);

        // 4. Salvar a entidade atualizada. O JPA/Hibernate é inteligente e executará um UPDATE.
        // A anotação @PreUpdate na entidade Patient cuidará de atualizar o campo `updatedAt`.
        Patient updatedPatient = patientRepository.save(existingPatient);

        return Optional.of(updatedPatient);
    }


    @Transactional
    public boolean deletePatient(UUID id) {
    // 1. Encontrar o paciente. Note que findById já vai respeitar a cláusula @Where,
    // então ele só encontrará um paciente se ele já estiver ativo.
    Optional<Patient> optionalPatient = patientRepository.findById(id);

    if (optionalPatient.isEmpty()) {
        // O paciente não existe ou já está inativo.
        return false;
    }

    // 2. Obter a entidade e alterar o status.
    Patient patient = optionalPatient.get();
    patient.setStatusAtivo(false);

    // 3. Salvar a entidade. O JPA executará um UPDATE.
    patientRepository.save(patient);

    return true;
    }
}