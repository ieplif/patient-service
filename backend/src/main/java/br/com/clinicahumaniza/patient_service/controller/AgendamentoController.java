package br.com.clinicahumaniza.patient_service.controller;

import br.com.clinicahumaniza.patient_service.dto.*;
import br.com.clinicahumaniza.patient_service.mapper.AgendamentoMapper;
import br.com.clinicahumaniza.patient_service.model.Agendamento;
import br.com.clinicahumaniza.patient_service.model.StatusAgendamento;
import br.com.clinicahumaniza.patient_service.service.AgendamentoRecorrenteService;
import br.com.clinicahumaniza.patient_service.service.AgendamentoService;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
    private final AgendamentoRecorrenteService recorrenteService;
    private final AgendamentoMapper agendamentoMapper;

    @Autowired
    public AgendamentoController(AgendamentoService agendamentoService,
                                  AgendamentoRecorrenteService recorrenteService,
                                  AgendamentoMapper agendamentoMapper) {
        this.agendamentoService = agendamentoService;
        this.recorrenteService = recorrenteService;
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
    @Operation(summary = "Listar agendamentos", description = "Retorna agendamentos ativos com paginação e filtros opcionais")
    @ApiResponse(responseCode = "200", description = "Lista paginada de agendamentos retornada com sucesso")
    public ResponseEntity<Page<AgendamentoResponseDTO>> getAllAgendamentos(
            @RequestParam(required = false) StatusAgendamento status,
            @RequestParam(required = false) UUID pacienteId,
            @RequestParam(required = false) UUID profissionalId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @PageableDefault(size = 20, sort = "dataHora", direction = Sort.Direction.DESC) Pageable pageable) {
        LocalDateTime dtInicio = dataInicio != null ? dataInicio.atStartOfDay() : null;
        LocalDateTime dtFim = dataFim != null ? dataFim.atTime(LocalTime.MAX) : null;
        return ResponseEntity.ok(agendamentoService.getAllAgendamentos(status, pacienteId, profissionalId,
                dtInicio, dtFim, pageable).map(agendamentoMapper::toResponseDTO));
    }

    @GetMapping("/export/csv")
    @Operation(summary = "Exportar agendamentos em CSV")
    @ApiResponse(responseCode = "200", description = "CSV de agendamentos gerado com sucesso")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) StatusAgendamento status) {
        byte[] csv = agendamentoService.exportCsv(inicio, fim, status);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=agendamentos.csv")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
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

    @PostMapping("/reposicao")
    @Operation(summary = "Criar reposição", description = "Cria um agendamento de reposição a partir de um agendamento cancelado com direito")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Reposição criada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Recurso não encontrado"),
            @ApiResponse(responseCode = "422", description = "Erro de regra de negócio")
    })
    public ResponseEntity<AgendamentoResponseDTO> criarReposicao(
            @Valid @RequestBody ReposicaoRequestDTO dto) {
        Agendamento agendamento = agendamentoService.criarReposicao(dto);
        return new ResponseEntity<>(agendamentoMapper.toResponseDTO(agendamento), HttpStatus.CREATED);
    }

    @GetMapping("/paciente/{pacienteId}/reposicoes-info")
    @Operation(summary = "Info de reposições do paciente", description = "Retorna informações sobre reposições usadas e disponíveis no mês")
    @ApiResponse(responseCode = "200", description = "Informações de reposição retornadas com sucesso")
    public ResponseEntity<ReposicaoInfoDTO> getReposicoesInfo(@PathVariable UUID pacienteId) {
        ReposicaoInfoDTO info = agendamentoService.getReposicoesInfo(pacienteId);
        return ResponseEntity.ok(info);
    }

    @PostMapping("/recorrente")
    @Operation(summary = "Criar agendamentos recorrentes", description = "Cria uma série de agendamentos com base em um padrão de recorrência")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Recorrência criada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Recurso não encontrado"),
            @ApiResponse(responseCode = "422", description = "Erro de regra de negócio")
    })
    public ResponseEntity<AgendamentoRecorrenteResponseDTO> createRecorrente(
            @Valid @RequestBody AgendamentoRecorrenteRequestDTO dto) {
        AgendamentoRecorrenteResponseDTO response = recorrenteService.createRecorrente(dto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}/recorrencia")
    @Operation(summary = "Cancelar agendamento da recorrência", description = "Cancela um agendamento ou todos os futuros da mesma recorrência")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Agendamento(s) cancelado(s) com sucesso"),
            @ApiResponse(responseCode = "404", description = "Agendamento não encontrado"),
            @ApiResponse(responseCode = "422", description = "Erro de regra de negócio")
    })
    public ResponseEntity<List<AgendamentoResponseDTO>> cancelarRecorrencia(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "false") boolean cancelarFuturos) {
        List<AgendamentoResponseDTO> cancelados = recorrenteService.cancelarRecorrencia(id, cancelarFuturos);
        return ResponseEntity.ok(cancelados);
    }
}
