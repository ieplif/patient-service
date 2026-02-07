package br.com.clinicahumaniza.patient_service.controller;

import br.com.clinicahumaniza.patient_service.dto.ServicoRequestDTO;
import br.com.clinicahumaniza.patient_service.dto.ServicoResponseDTO;
import br.com.clinicahumaniza.patient_service.dto.ServicoUpdateDTO;
import br.com.clinicahumaniza.patient_service.mapper.ServicoMapper;
import br.com.clinicahumaniza.patient_service.model.Servico;
import br.com.clinicahumaniza.patient_service.service.ServicoService;
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
@RequestMapping("/api/v1/servicos")
@Tag(name = "Serviços", description = "Endpoints para gestão de serviços")
public class ServicoController {

    private final ServicoService servicoService;
    private final ServicoMapper servicoMapper;

    @Autowired
    public ServicoController(ServicoService servicoService, ServicoMapper servicoMapper) {
        this.servicoService = servicoService;
        this.servicoMapper = servicoMapper;
    }

    @PostMapping
    @Operation(summary = "Criar serviço", description = "Cria um novo serviço no sistema")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Serviço criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Atividade ou Plano não encontrado")
    })
    public ResponseEntity<ServicoResponseDTO> createServico(@Valid @RequestBody ServicoRequestDTO dto) {
        Servico servico = servicoService.createServico(dto);
        return new ResponseEntity<>(servicoMapper.toResponseDTO(servico), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Listar serviços", description = "Retorna todos os serviços ativos")
    @ApiResponse(responseCode = "200", description = "Lista de serviços retornada com sucesso")
    public ResponseEntity<List<ServicoResponseDTO>> getAllServicos() {
        List<ServicoResponseDTO> responseDTOs = servicoService.getAllServicos().stream()
                .map(servicoMapper::toResponseDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar serviço por ID", description = "Retorna um serviço pelo seu ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Serviço encontrado"),
            @ApiResponse(responseCode = "404", description = "Serviço não encontrado")
    })
    public ResponseEntity<ServicoResponseDTO> getServicoById(@PathVariable UUID id) {
        Servico servico = servicoService.getServicoById(id);
        return ResponseEntity.ok(servicoMapper.toResponseDTO(servico));
    }

    @GetMapping("/atividade/{atividadeId}")
    @Operation(summary = "Listar serviços por atividade", description = "Retorna todos os serviços de uma atividade")
    @ApiResponse(responseCode = "200", description = "Lista de serviços retornada com sucesso")
    public ResponseEntity<List<ServicoResponseDTO>> getServicosByAtividade(@PathVariable UUID atividadeId) {
        List<ServicoResponseDTO> responseDTOs = servicoService.getServicosByAtividade(atividadeId).stream()
                .map(servicoMapper::toResponseDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/plano/{planoId}")
    @Operation(summary = "Listar serviços por plano", description = "Retorna todos os serviços de um plano")
    @ApiResponse(responseCode = "200", description = "Lista de serviços retornada com sucesso")
    public ResponseEntity<List<ServicoResponseDTO>> getServicosByPlano(@PathVariable UUID planoId) {
        List<ServicoResponseDTO> responseDTOs = servicoService.getServicosByPlano(planoId).stream()
                .map(servicoMapper::toResponseDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseDTOs);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar serviço", description = "Atualiza os dados de um serviço existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Serviço atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Serviço, Atividade ou Plano não encontrado")
    })
    public ResponseEntity<ServicoResponseDTO> updateServico(
            @PathVariable UUID id,
            @Valid @RequestBody ServicoUpdateDTO dto) {
        Servico servico = servicoService.updateServico(id, dto);
        return ResponseEntity.ok(servicoMapper.toResponseDTO(servico));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desativar serviço", description = "Desativa um serviço (soft delete)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Serviço desativado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Serviço não encontrado")
    })
    public ResponseEntity<Void> deleteServico(@PathVariable UUID id) {
        servicoService.deleteServico(id);
        return ResponseEntity.noContent().build();
    }
}
