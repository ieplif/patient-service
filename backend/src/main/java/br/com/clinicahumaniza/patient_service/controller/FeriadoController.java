package br.com.clinicahumaniza.patient_service.controller;

import br.com.clinicahumaniza.patient_service.dto.FeriadoRequestDTO;
import br.com.clinicahumaniza.patient_service.dto.FeriadoResponseDTO;
import br.com.clinicahumaniza.patient_service.mapper.FeriadoMapper;
import br.com.clinicahumaniza.patient_service.model.Feriado;
import br.com.clinicahumaniza.patient_service.service.FeriadoService;
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
@RequestMapping("/api/v1/feriados")
@Tag(name = "Feriados", description = "Endpoints para gestão de feriados")
public class FeriadoController {

    private final FeriadoService feriadoService;
    private final FeriadoMapper feriadoMapper;

    @Autowired
    public FeriadoController(FeriadoService feriadoService, FeriadoMapper feriadoMapper) {
        this.feriadoService = feriadoService;
        this.feriadoMapper = feriadoMapper;
    }

    @PostMapping
    @Operation(summary = "Criar feriado", description = "Cadastra um novo feriado no sistema")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Feriado criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos")
    })
    public ResponseEntity<FeriadoResponseDTO> createFeriado(@Valid @RequestBody FeriadoRequestDTO dto) {
        Feriado feriado = feriadoService.createFeriado(dto);
        return new ResponseEntity<>(feriadoMapper.toResponseDTO(feriado), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Listar feriados", description = "Retorna todos os feriados cadastrados com paginação")
    @ApiResponse(responseCode = "200", description = "Lista paginada de feriados retornada com sucesso")
    public ResponseEntity<Page<FeriadoResponseDTO>> getAllFeriados(
            @PageableDefault(size = 20, sort = "data", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(feriadoService.getAllFeriados(pageable).map(feriadoMapper::toResponseDTO));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar feriado por ID", description = "Retorna um feriado pelo seu ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Feriado encontrado"),
            @ApiResponse(responseCode = "404", description = "Feriado não encontrado")
    })
    public ResponseEntity<FeriadoResponseDTO> getFeriadoById(@PathVariable UUID id) {
        Feriado feriado = feriadoService.getFeriadoById(id);
        return ResponseEntity.ok(feriadoMapper.toResponseDTO(feriado));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar feriado", description = "Atualiza os dados de um feriado existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Feriado atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Feriado não encontrado")
    })
    public ResponseEntity<FeriadoResponseDTO> updateFeriado(
            @PathVariable UUID id,
            @Valid @RequestBody FeriadoRequestDTO dto) {
        Feriado feriado = feriadoService.updateFeriado(id, dto);
        return ResponseEntity.ok(feriadoMapper.toResponseDTO(feriado));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir feriado", description = "Remove um feriado do sistema")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Feriado excluído com sucesso"),
            @ApiResponse(responseCode = "404", description = "Feriado não encontrado")
    })
    public ResponseEntity<Void> deleteFeriado(@PathVariable UUID id) {
        feriadoService.deleteFeriado(id);
        return ResponseEntity.noContent().build();
    }
}
