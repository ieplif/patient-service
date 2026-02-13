package br.com.clinicahumaniza.patient_service.controller;

import br.com.clinicahumaniza.patient_service.dto.*;
import br.com.clinicahumaniza.patient_service.mapper.PagamentoMapper;
import br.com.clinicahumaniza.patient_service.model.Pagamento;
import br.com.clinicahumaniza.patient_service.service.PagamentoService;
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
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/pagamentos")
@Tag(name = "Pagamentos", description = "Endpoints para gestão de pagamentos")
public class PagamentoController {

    private final PagamentoService pagamentoService;
    private final PagamentoMapper pagamentoMapper;

    @Autowired
    public PagamentoController(PagamentoService pagamentoService, PagamentoMapper pagamentoMapper) {
        this.pagamentoService = pagamentoService;
        this.pagamentoMapper = pagamentoMapper;
    }

    @PostMapping
    @Operation(summary = "Criar pagamento", description = "Cria um novo pagamento vinculado a uma assinatura e/ou agendamento")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pagamento criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Paciente, Assinatura ou Agendamento não encontrado"),
            @ApiResponse(responseCode = "422", description = "Erro de regra de negócio")
    })
    public ResponseEntity<PagamentoResponseDTO> createPagamento(@Valid @RequestBody PagamentoRequestDTO dto) {
        Pagamento pagamento = pagamentoService.createPagamento(dto);
        return new ResponseEntity<>(pagamentoMapper.toResponseDTO(pagamento), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Listar pagamentos", description = "Retorna todos os pagamentos ativos")
    @ApiResponse(responseCode = "200", description = "Lista de pagamentos retornada com sucesso")
    public ResponseEntity<List<PagamentoResponseDTO>> getAllPagamentos() {
        List<PagamentoResponseDTO> responseDTOs = pagamentoService.getAllPagamentos().stream()
                .map(pagamentoMapper::toResponseDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar pagamento por ID", description = "Retorna um pagamento pelo seu ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pagamento encontrado"),
            @ApiResponse(responseCode = "404", description = "Pagamento não encontrado")
    })
    public ResponseEntity<PagamentoResponseDTO> getPagamentoById(@PathVariable UUID id) {
        Pagamento pagamento = pagamentoService.getPagamentoById(id);
        return ResponseEntity.ok(pagamentoMapper.toResponseDTO(pagamento));
    }

    @GetMapping("/paciente/{pacienteId}")
    @Operation(summary = "Listar pagamentos por paciente", description = "Retorna todos os pagamentos de um paciente")
    @ApiResponse(responseCode = "200", description = "Lista de pagamentos retornada com sucesso")
    public ResponseEntity<List<PagamentoResponseDTO>> getPagamentosByPaciente(@PathVariable UUID pacienteId) {
        List<PagamentoResponseDTO> responseDTOs = pagamentoService.getPagamentosByPaciente(pacienteId).stream()
                .map(pagamentoMapper::toResponseDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/assinatura/{assinaturaId}")
    @Operation(summary = "Listar pagamentos por assinatura", description = "Retorna todos os pagamentos de uma assinatura")
    @ApiResponse(responseCode = "200", description = "Lista de pagamentos retornada com sucesso")
    public ResponseEntity<List<PagamentoResponseDTO>> getPagamentosByAssinatura(@PathVariable UUID assinaturaId) {
        List<PagamentoResponseDTO> responseDTOs = pagamentoService.getPagamentosByAssinatura(assinaturaId).stream()
                .map(pagamentoMapper::toResponseDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/agendamento/{agendamentoId}")
    @Operation(summary = "Listar pagamentos por agendamento", description = "Retorna todos os pagamentos de um agendamento")
    @ApiResponse(responseCode = "200", description = "Lista de pagamentos retornada com sucesso")
    public ResponseEntity<List<PagamentoResponseDTO>> getPagamentosByAgendamento(@PathVariable UUID agendamentoId) {
        List<PagamentoResponseDTO> responseDTOs = pagamentoService.getPagamentosByAgendamento(agendamentoId).stream()
                .map(pagamentoMapper::toResponseDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/periodo")
    @Operation(summary = "Listar pagamentos por período", description = "Retorna pagamentos com vencimento no período informado")
    @ApiResponse(responseCode = "200", description = "Lista de pagamentos retornada com sucesso")
    public ResponseEntity<List<PagamentoResponseDTO>> getPagamentosByPeriodo(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        List<PagamentoResponseDTO> responseDTOs = pagamentoService.getPagamentosByPeriodo(inicio, fim).stream()
                .map(pagamentoMapper::toResponseDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseDTOs);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar pagamento", description = "Atualiza os dados de um pagamento existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pagamento atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Pagamento não encontrado")
    })
    public ResponseEntity<PagamentoResponseDTO> updatePagamento(
            @PathVariable UUID id,
            @Valid @RequestBody PagamentoUpdateDTO dto) {
        Pagamento pagamento = pagamentoService.updatePagamento(id, dto);
        return ResponseEntity.ok(pagamentoMapper.toResponseDTO(pagamento));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Alterar status do pagamento", description = "Altera o status de um pagamento")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status alterado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Pagamento não encontrado"),
            @ApiResponse(responseCode = "422", description = "Transição de status inválida")
    })
    public ResponseEntity<PagamentoResponseDTO> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody PagamentoStatusDTO dto) {
        Pagamento pagamento = pagamentoService.updateStatus(id, dto);
        return ResponseEntity.ok(pagamentoMapper.toResponseDTO(pagamento));
    }

    @PatchMapping("/{id}/parcelas/{parcelaId}/status")
    @Operation(summary = "Alterar status da parcela", description = "Altera o status de uma parcela e auto-atualiza o pagamento")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status da parcela alterado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Pagamento ou Parcela não encontrado"),
            @ApiResponse(responseCode = "422", description = "Erro de regra de negócio")
    })
    public ResponseEntity<PagamentoResponseDTO> updateParcelaStatus(
            @PathVariable UUID id,
            @PathVariable UUID parcelaId,
            @Valid @RequestBody ParcelaStatusDTO dto) {
        Pagamento pagamento = pagamentoService.updateParcelaStatus(id, parcelaId, dto);
        return ResponseEntity.ok(pagamentoMapper.toResponseDTO(pagamento));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desativar pagamento", description = "Desativa um pagamento (soft delete)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Pagamento desativado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Pagamento não encontrado")
    })
    public ResponseEntity<Void> deletePagamento(@PathVariable UUID id) {
        pagamentoService.deletePagamento(id);
        return ResponseEntity.noContent().build();
    }
}
