package br.com.clinicahumaniza.patient_service.controller;

import br.com.clinicahumaniza.patient_service.dto.AtividadeRequestDTO;
import br.com.clinicahumaniza.patient_service.dto.AtividadeResponseDTO;
import br.com.clinicahumaniza.patient_service.dto.AtividadeUpdateDTO;
import br.com.clinicahumaniza.patient_service.mapper.AtividadeMapper;
import br.com.clinicahumaniza.patient_service.model.Atividade;
import br.com.clinicahumaniza.patient_service.service.AtividadeService;
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
@RequestMapping("/api/v1/atividades")
@Tag(name = "Atividades", description = "Endpoints para gestão de atividades")
public class AtividadeController {

    private final AtividadeService atividadeService;
    private final AtividadeMapper atividadeMapper;

    @Autowired
    public AtividadeController(AtividadeService atividadeService, AtividadeMapper atividadeMapper) {
        this.atividadeService = atividadeService;
        this.atividadeMapper = atividadeMapper;
    }

    @PostMapping
    @Operation(summary = "Criar atividade", description = "Cria uma nova atividade no sistema")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Atividade criada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "409", description = "Nome já cadastrado")
    })
    public ResponseEntity<AtividadeResponseDTO> createAtividade(@Valid @RequestBody AtividadeRequestDTO dto) {
        Atividade atividade = atividadeService.createAtividade(dto);
        return new ResponseEntity<>(atividadeMapper.toResponseDTO(atividade), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Listar atividades", description = "Retorna todas as atividades ativas")
    @ApiResponse(responseCode = "200", description = "Lista de atividades retornada com sucesso")
    public ResponseEntity<List<AtividadeResponseDTO>> getAllAtividades() {
        List<AtividadeResponseDTO> responseDTOs = atividadeService.getAllAtividades().stream()
                .map(atividadeMapper::toResponseDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar atividade por ID", description = "Retorna uma atividade pelo seu ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Atividade encontrada"),
            @ApiResponse(responseCode = "404", description = "Atividade não encontrada")
    })
    public ResponseEntity<AtividadeResponseDTO> getAtividadeById(@PathVariable UUID id) {
        Atividade atividade = atividadeService.getAtividadeById(id);
        return ResponseEntity.ok(atividadeMapper.toResponseDTO(atividade));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar atividade", description = "Atualiza os dados de uma atividade existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Atividade atualizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Atividade não encontrada")
    })
    public ResponseEntity<AtividadeResponseDTO> updateAtividade(
            @PathVariable UUID id,
            @Valid @RequestBody AtividadeUpdateDTO dto) {
        Atividade atividade = atividadeService.updateAtividade(id, dto);
        return ResponseEntity.ok(atividadeMapper.toResponseDTO(atividade));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desativar atividade", description = "Desativa uma atividade (soft delete)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Atividade desativada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Atividade não encontrada")
    })
    public ResponseEntity<Void> deleteAtividade(@PathVariable UUID id) {
        atividadeService.deleteAtividade(id);
        return ResponseEntity.noContent().build();
    }
}
