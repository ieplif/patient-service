package br.com.clinicahumaniza.patient_service.service;

import br.com.clinicahumaniza.patient_service.dto.PatientRequestDTO;
import br.com.clinicahumaniza.patient_service.dto.PatientUpdateDTO;
import br.com.clinicahumaniza.patient_service.exception.DuplicateResourceException;
import br.com.clinicahumaniza.patient_service.exception.PatientNotFoundException;
import br.com.clinicahumaniza.patient_service.mapper.PatientMapper;
import br.com.clinicahumaniza.patient_service.model.Patient;
import br.com.clinicahumaniza.patient_service.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PatientService {

    private final PatientRepository patientRepository;
    private final PatientMapper patientMapper;

    @Autowired
    public PatientService(PatientRepository patientRepository, PatientMapper patientMapper) {
        this.patientRepository = patientRepository;
        this.patientMapper = patientMapper;
    }

    @Transactional
    public Patient createPatient(PatientRequestDTO patientDTO) {
        Patient patient = patientMapper.toEntity(patientDTO);

        patientRepository.findByCpf(patient.getCpf()).ifPresent(p -> {
            throw new DuplicateResourceException("CPF", patient.getCpf());
        });
        patientRepository.findByEmail(patient.getEmail()).ifPresent(p -> {
            throw new DuplicateResourceException("E-mail", patient.getEmail());
        });

        return patientRepository.save(patient);
    }

    public Patient getPatientById(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("Patient ID cannot be null");
        }
        return patientRepository.findById(id)
                .orElseThrow(() -> new PatientNotFoundException(id));
    }

    public List<Patient> getAllPatients() {
        return patientRepository.findAll();
    }

    @Transactional
    public Patient updatePatient(UUID id, PatientUpdateDTO patientUpdateDTO) {
        if (id == null) {
            throw new IllegalArgumentException("Patient ID cannot be null");
        }
        Patient existingPatient = patientRepository.findById(id)
                .orElseThrow(() -> new PatientNotFoundException(id));

        if (existingPatient != null) {
            patientMapper.updateEntityFromDto(patientUpdateDTO, existingPatient);
            return patientRepository.save(existingPatient);
        }
        throw new PatientNotFoundException(id);
    }

    @Transactional
    public void deletePatient(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("Patient ID cannot be null");
        }
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new PatientNotFoundException(id));

        patient.setStatusAtivo(false);
        patientRepository.save(patient);
    }
}
