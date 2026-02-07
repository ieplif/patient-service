package br.com.clinicahumaniza.patient_service.controller;

import br.com.clinicahumaniza.patient_service.dto.PlanoRequestDTO;
import br.com.clinicahumaniza.patient_service.dto.PlanoResponseDTO;
import br.com.clinicahumaniza.patient_service.dto.PlanoUpdateDTO;
import br.com.clinicahumaniza.patient_service.mapper.PlanoMapper;
import br.com.clinicahumaniza.patient_service.model.Plano;
import br.com.clinicahumaniza.patient_service.service.PlanoService;
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
@RequestMapping("/api/v1/planos")
@Tag(name = "Planos", description = "Endpoints para gestão de planos")
public class PlanoController {

    private final PlanoService planoService;
    private final PlanoMapper planoMapper;

    @Autowired
    public PlanoController(PlanoService planoService, PlanoMapper planoMapper) {
        this.planoService = planoService;
        this.planoMapper = planoMapper;
    }

    @PostMapping
    @Operation(summary = "Criar plano", description = "Cria um novo plano no sistema")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Plano criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "409", description = "Nome já cadastrado")
    })
    public ResponseEntity<PlanoResponseDTO> createPlano(@Valid @RequestBody PlanoRequestDTO dto) {
        Plano plano = planoService.createPlano(dto);
        return new ResponseEntity<>(planoMapper.toResponseDTO(plano), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Listar planos", description = "Retorna todos os planos ativos")
    @ApiResponse(responseCode = "200", description = "Lista de planos retornada com sucesso")
    public ResponseEntity<List<PlanoResponseDTO>> getAllPlanos() {
        List<PlanoResponseDTO> responseDTOs = planoService.getAllPlanos().stream()
                .map(planoMapper::toResponseDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar plano por ID", description = "Retorna um plano pelo seu ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Plano encontrado"),
            @ApiResponse(responseCode = "404", description = "Plano não encontrado")
    })
    public ResponseEntity<PlanoResponseDTO> getPlanoById(@PathVariable UUID id) {
        Plano plano = planoService.getPlanoById(id);
        return ResponseEntity.ok(planoMapper.toResponseDTO(plano));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar plano", description = "Atualiza os dados de um plano existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Plano atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Plano não encontrado")
    })
    public ResponseEntity<PlanoResponseDTO> updatePlano(
            @PathVariable UUID id,
            @Valid @RequestBody PlanoUpdateDTO dto) {
        Plano plano = planoService.updatePlano(id, dto);
        return ResponseEntity.ok(planoMapper.toResponseDTO(plano));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desativar plano", description = "Desativa um plano (soft delete)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Plano desativado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Plano não encontrado")
    })
    public ResponseEntity<Void> deletePlano(@PathVariable UUID id) {
        planoService.deletePlano(id);
        return ResponseEntity.noContent().build();
    }
}
