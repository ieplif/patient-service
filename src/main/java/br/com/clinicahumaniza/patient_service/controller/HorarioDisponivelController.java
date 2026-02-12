package br.com.clinicahumaniza.patient_service.controller;

import br.com.clinicahumaniza.patient_service.dto.HorarioDisponivelRequestDTO;
import br.com.clinicahumaniza.patient_service.dto.HorarioDisponivelResponseDTO;
import br.com.clinicahumaniza.patient_service.dto.HorarioDisponivelUpdateDTO;
import br.com.clinicahumaniza.patient_service.mapper.HorarioDisponivelMapper;
import br.com.clinicahumaniza.patient_service.model.HorarioDisponivel;
import br.com.clinicahumaniza.patient_service.service.HorarioDisponivelService;
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
@RequestMapping("/api/v1/disponibilidades")
@Tag(name = "Disponibilidades", description = "Endpoints para gestão de horários disponíveis dos profissionais")
public class HorarioDisponivelController {

    private final HorarioDisponivelService horarioDisponivelService;
    private final HorarioDisponivelMapper horarioDisponivelMapper;

    @Autowired
    public HorarioDisponivelController(HorarioDisponivelService horarioDisponivelService,
                                        HorarioDisponivelMapper horarioDisponivelMapper) {
        this.horarioDisponivelService = horarioDisponivelService;
        this.horarioDisponivelMapper = horarioDisponivelMapper;
    }

    @PostMapping
    @Operation(summary = "Criar horário disponível", description = "Adiciona um horário à grade semanal do profissional")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Horário criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Profissional não encontrado")
    })
    public ResponseEntity<HorarioDisponivelResponseDTO> createHorarioDisponivel(
            @Valid @RequestBody HorarioDisponivelRequestDTO dto) {
        HorarioDisponivel horario = horarioDisponivelService.createHorarioDisponivel(dto);
        return new ResponseEntity<>(horarioDisponivelMapper.toResponseDTO(horario), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Listar horários disponíveis", description = "Retorna todos os horários disponíveis ativos")
    @ApiResponse(responseCode = "200", description = "Lista de horários retornada com sucesso")
    public ResponseEntity<List<HorarioDisponivelResponseDTO>> getAllHorariosDisponiveis() {
        List<HorarioDisponivelResponseDTO> responseDTOs = horarioDisponivelService.getAllHorariosDisponiveis().stream()
                .map(horarioDisponivelMapper::toResponseDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar horário disponível por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Horário encontrado"),
            @ApiResponse(responseCode = "404", description = "Horário não encontrado")
    })
    public ResponseEntity<HorarioDisponivelResponseDTO> getHorarioDisponivelById(@PathVariable UUID id) {
        HorarioDisponivel horario = horarioDisponivelService.getHorarioDisponivelById(id);
        return ResponseEntity.ok(horarioDisponivelMapper.toResponseDTO(horario));
    }

    @GetMapping("/profissional/{profissionalId}")
    @Operation(summary = "Listar horários por profissional", description = "Retorna a grade semanal do profissional")
    @ApiResponse(responseCode = "200", description = "Lista de horários retornada com sucesso")
    public ResponseEntity<List<HorarioDisponivelResponseDTO>> getHorariosByProfissional(
            @PathVariable UUID profissionalId) {
        List<HorarioDisponivelResponseDTO> responseDTOs = horarioDisponivelService
                .getHorariosByProfissional(profissionalId).stream()
                .map(horarioDisponivelMapper::toResponseDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseDTOs);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar horário disponível")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Horário atualizado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Horário não encontrado")
    })
    public ResponseEntity<HorarioDisponivelResponseDTO> updateHorarioDisponivel(
            @PathVariable UUID id,
            @Valid @RequestBody HorarioDisponivelUpdateDTO dto) {
        HorarioDisponivel horario = horarioDisponivelService.updateHorarioDisponivel(id, dto);
        return ResponseEntity.ok(horarioDisponivelMapper.toResponseDTO(horario));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir horário disponível", description = "Desativa o horário (soft delete)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Horário excluído com sucesso"),
            @ApiResponse(responseCode = "404", description = "Horário não encontrado")
    })
    public ResponseEntity<Void> deleteHorarioDisponivel(@PathVariable UUID id) {
        horarioDisponivelService.deleteHorarioDisponivel(id);
        return ResponseEntity.noContent().build();
    }
}
