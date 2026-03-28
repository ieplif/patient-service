package br.com.clinicahumaniza.patient_service.controller;

import br.com.clinicahumaniza.patient_service.dto.PatientExportDTO;
import br.com.clinicahumaniza.patient_service.dto.PatientRequestDTO;
import br.com.clinicahumaniza.patient_service.dto.PatientResponseDTO;
import br.com.clinicahumaniza.patient_service.dto.PatientUpdateDTO;
import br.com.clinicahumaniza.patient_service.mapper.PatientMapper;
import br.com.clinicahumaniza.patient_service.model.Patient;
import br.com.clinicahumaniza.patient_service.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/patients")
@Tag(name = "Pacientes", description = "Endpoints para gestão de pacientes")
public class PatientController {

    private final PatientService patientService;
    private final PatientMapper patientMapper;

    @Autowired
    public PatientController(PatientService patientService, PatientMapper patientMapper) {
        this.patientService = patientService;
        this.patientMapper = patientMapper;
    }

    @PostMapping
    @Operation(summary = "Cadastrar paciente", description = "Cria um novo paciente no sistema")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Paciente criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "409", description = "CPF ou e-mail já cadastrado")
    })
    public ResponseEntity<PatientResponseDTO> createPatient(@Valid @RequestBody PatientRequestDTO patientDTO) {
        Patient createdPatient = patientService.createPatient(patientDTO);
        PatientResponseDTO responseDTO = patientMapper.toResponseDTO(createdPatient);
        return new ResponseEntity<>(responseDTO, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Listar pacientes", description = "Retorna pacientes ativos com paginação e filtros opcionais")
    @ApiResponse(responseCode = "200", description = "Lista paginada de pacientes retornada com sucesso")
    public ResponseEntity<Page<PatientResponseDTO>> getAllPatients(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String cpf,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(patientService.getAllPatients(nome, email, cpf, pageable)
                .map(patientMapper::toResponseDTO));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar paciente por ID", description = "Retorna um paciente pelo seu ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paciente encontrado"),
            @ApiResponse(responseCode = "404", description = "Paciente não encontrado")
    })
    public ResponseEntity<PatientResponseDTO> getPatientById(@PathVariable UUID id) {
        Patient patient = patientService.getPatientById(id);
        return ResponseEntity.ok(patientMapper.toResponseDTO(patient));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar paciente", description = "Atualiza os dados de um paciente existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paciente atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Paciente não encontrado")
    })
    public ResponseEntity<PatientResponseDTO> updatePatient(
            @PathVariable UUID id,
            @Valid @RequestBody PatientUpdateDTO patientUpdateDTO) {
        Patient updatedPatient = patientService.updatePatient(id, patientUpdateDTO);
        return ResponseEntity.ok(patientMapper.toResponseDTO(updatedPatient));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desativar paciente", description = "Desativa um paciente (soft delete)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Paciente desativado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Paciente não encontrado")
    })
    public ResponseEntity<Void> deletePatient(@PathVariable UUID id) {
        patientService.deletePatient(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/permanente")
    @Operation(summary = "Exclusão permanente do paciente (LGPD - Direito ao Esquecimento)",
               description = "Remove permanentemente todos os dados do paciente. Apenas ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Paciente removido permanentemente"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Paciente não encontrado")
    })
    public ResponseEntity<Void> deletePatientPermanente(@PathVariable UUID id) {
        patientService.deletePermanente(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/exportar")
    @Operation(summary = "Exportar todos os dados do paciente (LGPD - Portabilidade)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dados exportados com sucesso"),
            @ApiResponse(responseCode = "404", description = "Paciente não encontrado")
    })
    public ResponseEntity<PatientExportDTO> exportarDados(@PathVariable UUID id) {
        return ResponseEntity.ok(patientService.exportarDados(id));
    }
}
