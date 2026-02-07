package br.com.clinicahumaniza.patient_service.controller;

import br.com.clinicahumaniza.patient_service.dto.AssinaturaRequestDTO;
import br.com.clinicahumaniza.patient_service.dto.AssinaturaResponseDTO;
import br.com.clinicahumaniza.patient_service.dto.AssinaturaStatusDTO;
import br.com.clinicahumaniza.patient_service.dto.AssinaturaUpdateDTO;
import br.com.clinicahumaniza.patient_service.mapper.AssinaturaMapper;
import br.com.clinicahumaniza.patient_service.model.Assinatura;
import br.com.clinicahumaniza.patient_service.service.AssinaturaService;
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
@RequestMapping("/api/v1/assinaturas")
@Tag(name = "Assinaturas", description = "Endpoints para gestão de assinaturas")
public class AssinaturaController {

    private final AssinaturaService assinaturaService;
    private final AssinaturaMapper assinaturaMapper;

    @Autowired
    public AssinaturaController(AssinaturaService assinaturaService, AssinaturaMapper assinaturaMapper) {
        this.assinaturaService = assinaturaService;
        this.assinaturaMapper = assinaturaMapper;
    }

    @PostMapping
    @Operation(summary = "Criar assinatura", description = "Cria uma nova assinatura vinculando paciente a serviço")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Assinatura criada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Paciente ou Serviço não encontrado")
    })
    public ResponseEntity<AssinaturaResponseDTO> createAssinatura(@Valid @RequestBody AssinaturaRequestDTO dto) {
        Assinatura assinatura = assinaturaService.createAssinatura(dto);
        return new ResponseEntity<>(assinaturaMapper.toResponseDTO(assinatura), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Listar assinaturas", description = "Retorna todas as assinaturas ativas")
    @ApiResponse(responseCode = "200", description = "Lista de assinaturas retornada com sucesso")
    public ResponseEntity<List<AssinaturaResponseDTO>> getAllAssinaturas() {
        List<AssinaturaResponseDTO> responseDTOs = assinaturaService.getAllAssinaturas().stream()
                .map(assinaturaMapper::toResponseDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar assinatura por ID", description = "Retorna uma assinatura pelo seu ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Assinatura encontrada"),
            @ApiResponse(responseCode = "404", description = "Assinatura não encontrada")
    })
    public ResponseEntity<AssinaturaResponseDTO> getAssinaturaById(@PathVariable UUID id) {
        Assinatura assinatura = assinaturaService.getAssinaturaById(id);
        return ResponseEntity.ok(assinaturaMapper.toResponseDTO(assinatura));
    }

    @GetMapping("/paciente/{pacienteId}")
    @Operation(summary = "Listar assinaturas por paciente", description = "Retorna todas as assinaturas de um paciente")
    @ApiResponse(responseCode = "200", description = "Lista de assinaturas retornada com sucesso")
    public ResponseEntity<List<AssinaturaResponseDTO>> getAssinaturasByPaciente(@PathVariable UUID pacienteId) {
        List<AssinaturaResponseDTO> responseDTOs = assinaturaService.getAssinaturasByPaciente(pacienteId).stream()
                .map(assinaturaMapper::toResponseDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/servico/{servicoId}")
    @Operation(summary = "Listar assinaturas por serviço", description = "Retorna todas as assinaturas de um serviço")
    @ApiResponse(responseCode = "200", description = "Lista de assinaturas retornada com sucesso")
    public ResponseEntity<List<AssinaturaResponseDTO>> getAssinaturasByServico(@PathVariable UUID servicoId) {
        List<AssinaturaResponseDTO> responseDTOs = assinaturaService.getAssinaturasByServico(servicoId).stream()
                .map(assinaturaMapper::toResponseDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseDTOs);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar assinatura", description = "Atualiza os dados de uma assinatura existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Assinatura atualizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Assinatura não encontrada")
    })
    public ResponseEntity<AssinaturaResponseDTO> updateAssinatura(
            @PathVariable UUID id,
            @Valid @RequestBody AssinaturaUpdateDTO dto) {
        Assinatura assinatura = assinaturaService.updateAssinatura(id, dto);
        return ResponseEntity.ok(assinaturaMapper.toResponseDTO(assinatura));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Alterar status da assinatura", description = "Altera o status de uma assinatura")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status alterado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Assinatura não encontrada"),
            @ApiResponse(responseCode = "422", description = "Transição de status inválida")
    })
    public ResponseEntity<AssinaturaResponseDTO> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody AssinaturaStatusDTO dto) {
        Assinatura assinatura = assinaturaService.updateStatus(id, dto);
        return ResponseEntity.ok(assinaturaMapper.toResponseDTO(assinatura));
    }

    @PatchMapping("/{id}/registrar-sessao")
    @Operation(summary = "Registrar sessão realizada", description = "Registra uma sessão realizada na assinatura")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sessão registrada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Assinatura não encontrada"),
            @ApiResponse(responseCode = "422", description = "Não é possível registrar sessão")
    })
    public ResponseEntity<AssinaturaResponseDTO> registrarSessao(@PathVariable UUID id) {
        Assinatura assinatura = assinaturaService.registrarSessao(id);
        return ResponseEntity.ok(assinaturaMapper.toResponseDTO(assinatura));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desativar assinatura", description = "Desativa uma assinatura (soft delete)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Assinatura desativada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Assinatura não encontrada")
    })
    public ResponseEntity<Void> deleteAssinatura(@PathVariable UUID id) {
        assinaturaService.deleteAssinatura(id);
        return ResponseEntity.noContent().build();
    }
}
