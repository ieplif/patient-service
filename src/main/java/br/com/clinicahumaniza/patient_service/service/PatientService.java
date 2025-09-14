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
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getAllPatients'");
    }
}
