package br.com.clinicahumaniza.patient_service.controller;

import br.com.clinicahumaniza.patient_service.dto.ProfissionalRequestDTO;
import br.com.clinicahumaniza.patient_service.dto.ProfissionalResponseDTO;
import br.com.clinicahumaniza.patient_service.dto.ProfissionalUpdateDTO;
import br.com.clinicahumaniza.patient_service.mapper.ProfissionalMapper;
import br.com.clinicahumaniza.patient_service.model.Profissional;
import br.com.clinicahumaniza.patient_service.service.ProfissionalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/profissionais")
@Tag(name = "Profissionais", description = "Endpoints para gestão de profissionais")
public class ProfissionalController {

    private final ProfissionalService profissionalService;
    private final ProfissionalMapper profissionalMapper;

    @Autowired
    public ProfissionalController(ProfissionalService profissionalService, ProfissionalMapper profissionalMapper) {
        this.profissionalService = profissionalService;
        this.profissionalMapper = profissionalMapper;
    }

    @PostMapping
    @Operation(summary = "Criar profissional", description = "Cria um novo profissional com login no sistema")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Profissional criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "409", description = "E-mail já cadastrado")
    })
    public ResponseEntity<ProfissionalResponseDTO> createProfissional(@Valid @RequestBody ProfissionalRequestDTO dto) {
        Profissional profissional = profissionalService.createProfissional(dto);
        return new ResponseEntity<>(profissionalMapper.toResponseDTO(profissional), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Listar profissionais", description = "Retorna todos os profissionais ativos")
    @ApiResponse(responseCode = "200", description = "Lista de profissionais retornada com sucesso")
    public ResponseEntity<List<ProfissionalResponseDTO>> getAllProfissionais() {
        List<ProfissionalResponseDTO> responseDTOs = profissionalService.getAllProfissionais().stream()
                .map(profissionalMapper::toResponseDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar profissional por ID", description = "Retorna um profissional pelo seu ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profissional encontrado"),
            @ApiResponse(responseCode = "404", description = "Profissional não encontrado")
    })
    public ResponseEntity<ProfissionalResponseDTO> getProfissionalById(@PathVariable UUID id) {
        Profissional profissional = profissionalService.getProfissionalById(id);
        return ResponseEntity.ok(profissionalMapper.toResponseDTO(profissional));
    }

    @GetMapping("/atividade/{atividadeId}")
    @Operation(summary = "Listar profissionais por atividade", description = "Retorna profissionais que atendem uma atividade específica")
    @ApiResponse(responseCode = "200", description = "Lista de profissionais retornada com sucesso")
    public ResponseEntity<List<ProfissionalResponseDTO>> getProfissionaisByAtividade(@PathVariable UUID atividadeId) {
        List<ProfissionalResponseDTO> responseDTOs = profissionalService.getProfissionaisByAtividade(atividadeId).stream()
                .map(profissionalMapper::toResponseDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseDTOs);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar profissional", description = "Atualiza os dados de um profissional existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profissional atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Profissional não encontrado")
    })
    public ResponseEntity<ProfissionalResponseDTO> updateProfissional(
            @PathVariable UUID id,
            @Valid @RequestBody ProfissionalUpdateDTO dto) {
        Profissional profissional = profissionalService.updateProfissional(id, dto);
        return ResponseEntity.ok(profissionalMapper.toResponseDTO(profissional));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desativar profissional", description = "Desativa um profissional (soft delete)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Profissional desativado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Profissional não encontrado")
    })
    public ResponseEntity<Void> deleteProfissional(@PathVariable UUID id) {
        profissionalService.deleteProfissional(id);
        return ResponseEntity.noContent().build();
    }
}
