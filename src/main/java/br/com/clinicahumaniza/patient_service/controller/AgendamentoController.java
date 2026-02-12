package br.com.clinicahumaniza.patient_service.controller;

import br.com.clinicahumaniza.patient_service.dto.AgendamentoRequestDTO;
import br.com.clinicahumaniza.patient_service.dto.AgendamentoResponseDTO;
import br.com.clinicahumaniza.patient_service.dto.AgendamentoStatusDTO;
import br.com.clinicahumaniza.patient_service.dto.AgendamentoUpdateDTO;
import br.com.clinicahumaniza.patient_service.mapper.AgendamentoMapper;
import br.com.clinicahumaniza.patient_service.model.Agendamento;
import br.com.clinicahumaniza.patient_service.service.AgendamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/agendamentos")
@Tag(name = "Agendamentos", description = "Endpoints para gestão de agendamentos de consultas")
public class AgendamentoController {

    private final AgendamentoService agendamentoService;
    private final AgendamentoMapper agendamentoMapper;

    @Autowired
    public AgendamentoController(AgendamentoService agendamentoService,
                                  AgendamentoMapper agendamentoMapper) {
        this.agendamentoService = agendamentoService;
        this.agendamentoMapper = agendamentoMapper;
    }

    @PostMapping
    @Operation(summary = "Criar agendamento", description = "Cria um novo agendamento de consulta")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Agendamento criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Recurso não encontrado"),
            @ApiResponse(responseCode = "422", description = "Erro de regra de negócio")
    })
    public ResponseEntity<AgendamentoResponseDTO> createAgendamento(
            @Valid @RequestBody AgendamentoRequestDTO dto) {
        Agendamento agendamento = agendamentoService.createAgendamento(dto);
        return new ResponseEntity<>(agendamentoMapper.toResponseDTO(agendamento), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Listar agendamentos", description = "Retorna todos os agendamentos ativos")
    @ApiResponse(responseCode = "200", description = "Lista de agendamentos retornada com sucesso")
    public ResponseEntity<List<AgendamentoResponseDTO>> getAllAgendamentos() {
        List<AgendamentoResponseDTO> responseDTOs = agendamentoService.getAllAgendamentos().stream()
                .map(agendamentoMapper::toResponseDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar agendamento por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Agendamento encontrado"),
            @ApiResponse(responseCode = "404", description = "Agendamento não encontrado")
    })
    public ResponseEntity<AgendamentoResponseDTO> getAgendamentoById(@PathVariable UUID id) {
        Agendamento agendamento = agendamentoService.getAgendamentoById(id);
        return ResponseEntity.ok(agendamentoMapper.toResponseDTO(agendamento));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar agendamento")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Agendamento atualizado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Agendamento não encontrado"),
            @ApiResponse(responseCode = "422", description = "Erro de regra de negócio")
    })
    public ResponseEntity<AgendamentoResponseDTO> updateAgendamento(
            @PathVariable UUID id,
            @Valid @RequestBody AgendamentoUpdateDTO dto) {
        Agendamento agendamento = agendamentoService.updateAgendamento(id, dto);
        return ResponseEntity.ok(agendamentoMapper.toResponseDTO(agendamento));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Alterar status do agendamento", description = "Transição de status com validação de fluxo")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status atualizado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Agendamento não encontrado"),
            @ApiResponse(responseCode = "422", description = "Transição de status inválida")
    })
    public ResponseEntity<AgendamentoResponseDTO> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody AgendamentoStatusDTO dto) {
        Agendamento agendamento = agendamentoService.updateStatus(id, dto);
        return ResponseEntity.ok(agendamentoMapper.toResponseDTO(agendamento));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir agendamento", description = "Desativa o agendamento (soft delete)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Agendamento excluído com sucesso"),
            @ApiResponse(responseCode = "404", description = "Agendamento não encontrado")
    })
    public ResponseEntity<Void> deleteAgendamento(@PathVariable UUID id) {
        agendamentoService.deleteAgendamento(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/paciente/{pacienteId}")
    @Operation(summary = "Listar agendamentos por paciente")
    @ApiResponse(responseCode = "200", description = "Lista de agendamentos retornada com sucesso")
    public ResponseEntity<List<AgendamentoResponseDTO>> getAgendamentosByPaciente(
            @PathVariable UUID pacienteId) {
        List<AgendamentoResponseDTO> responseDTOs = agendamentoService
                .getAgendamentosByPaciente(pacienteId).stream()
                .map(agendamentoMapper::toResponseDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/profissional/{profissionalId}")
    @Operation(summary = "Listar agendamentos por profissional")
    @ApiResponse(responseCode = "200", description = "Lista de agendamentos retornada com sucesso")
    public ResponseEntity<List<AgendamentoResponseDTO>> getAgendamentosByProfissional(
            @PathVariable UUID profissionalId) {
        List<AgendamentoResponseDTO> responseDTOs = agendamentoService
                .getAgendamentosByProfissional(profissionalId).stream()
                .map(agendamentoMapper::toResponseDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/data")
    @Operation(summary = "Listar agendamentos por período", description = "Filtra agendamentos entre datas de início e fim")
    @ApiResponse(responseCode = "200", description = "Lista de agendamentos retornada com sucesso")
    public ResponseEntity<List<AgendamentoResponseDTO>> getAgendamentosByPeriodo(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        List<AgendamentoResponseDTO> responseDTOs = agendamentoService
                .getAgendamentosByPeriodo(inicio.atStartOfDay(), fim.atTime(LocalTime.MAX)).stream()
                .map(agendamentoMapper::toResponseDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/profissional/{profissionalId}/slots-disponiveis")
    @Operation(summary = "Consultar slots disponíveis", description = "Retorna horários livres do profissional para uma data")
    @ApiResponse(responseCode = "200", description = "Slots disponíveis retornados com sucesso")
    public ResponseEntity<List<LocalDateTime>> getAvailableSlots(
            @PathVariable UUID profissionalId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam(defaultValue = "50") Integer duracaoMinutos,
            @RequestParam(defaultValue = "1") Integer capacidadeMaxima) {
        List<LocalDateTime> slots = agendamentoService.getAvailableSlots(profissionalId, data,
                duracaoMinutos, capacidadeMaxima);
        return ResponseEntity.ok(slots);
    }
}
