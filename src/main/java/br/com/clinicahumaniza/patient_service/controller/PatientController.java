package br.com.clinicahumaniza.patient_service.controller;

import br.com.clinicahumaniza.patient_service.dto.PatientRequestDTO;
import br.com.clinicahumaniza.patient_service.dto.PatientResponseDTO;
import br.com.clinicahumaniza.patient_service.mapper.PatientMapper;
import br.com.clinicahumaniza.patient_service.model.Patient;
import br.com.clinicahumaniza.patient_service.service.PatientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/patients" )
public class PatientController {

    private final PatientService patientService;
    private final PatientMapper patientMapper; // Injeta o mapper

    @Autowired
    public PatientController(PatientService patientService, PatientMapper patientMapper) {
        this.patientService = patientService;
        this.patientMapper = patientMapper;
    }

    @PostMapping
    public ResponseEntity<PatientResponseDTO> createPatient(@RequestBody PatientRequestDTO patientDTO) {
        try {
            Patient createdPatient = patientService.createPatient(patientDTO);
            // Converte a entidade salva para o DTO de resposta antes de enviar
            PatientResponseDTO responseDTO = patientMapper.toResponseDTO(createdPatient);
            return new ResponseEntity<>(responseDTO, HttpStatus.CREATED);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<PatientResponseDTO>> getAllPatients() {
        List<Patient> patients = patientService.getAllPatients();
        // Converte a lista de entidades para uma lista de DTOs
        List<PatientResponseDTO> responseDTOs = patients.stream()
                .map(patientMapper::toResponseDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PatientResponseDTO> getPatientById(@PathVariable UUID id) {
        return patientService.getPatientById(id)
                .map(patientMapper::toResponseDTO) // Converte a entidade para DTO
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
