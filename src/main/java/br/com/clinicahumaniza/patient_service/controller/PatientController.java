package br.com.clinicahumaniza.patient_service.controller;

import br.com.clinicahumaniza.patient_service.dto.PatientRequestDTO;
import br.com.clinicahumaniza.patient_service.dto.PatientResponseDTO;
import br.com.clinicahumaniza.patient_service.dto.PatientUpdateDTO;
import br.com.clinicahumaniza.patient_service.mapper.PatientMapper;
import br.com.clinicahumaniza.patient_service.model.Patient;
import br.com.clinicahumaniza.patient_service.service.PatientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
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

    @PutMapping("/{id}") // 1. Mapeia requisições HTTP PUT para este método.
    public ResponseEntity<PatientResponseDTO> updatePatient(
            @PathVariable UUID id, // 2. Pega o ID da URL.
            @RequestBody PatientUpdateDTO patientUpdateDTO) { // 3. Pega os dados de atualização do corpo da requisição.

        Optional<Patient> updatedPatientOptional = patientService.updatePatient(id, patientUpdateDTO);

        return updatedPatientOptional
                .map(patient -> ResponseEntity.ok(patientMapper.toResponseDTO(patient))) // 4. Se a atualização foi bem-sucedida, retorna 200 OK com o DTO de resposta.
                .orElse(ResponseEntity.notFound().build()); // 5. Se o paciente não foi encontrado, retorna 404 Not Found.
    }

    @DeleteMapping("/{id}") // 1. Mapeia requisições HTTP DELETE.
    public ResponseEntity<Void> deletePatient(@PathVariable UUID id) { // 2. Retorna ResponseEntity<Void> para indicar um corpo vazio.
        boolean wasDeleted = patientService.deletePatient(id);

        if (wasDeleted) {
            // 3. Se a exclusão foi bem-sucedida, retorna 204 No Content.
            // Este é o status padrão para indicar sucesso em uma operação de DELETE sem retorno de conteúdo.
            return ResponseEntity.noContent().build();
        } else {
            // 4. Se o recurso não foi encontrado para ser deletado, retorna 404 Not Found.
            return ResponseEntity.notFound().build();
        }
    }
}
