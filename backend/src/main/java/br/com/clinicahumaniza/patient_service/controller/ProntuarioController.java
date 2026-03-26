package br.com.clinicahumaniza.patient_service.controller;

import br.com.clinicahumaniza.patient_service.dto.ProntuarioResponseDTO;
import br.com.clinicahumaniza.patient_service.service.ProntuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/prontuarios")
@Tag(name = "Prontuários", description = "Upload e gestão de prontuários (Supabase Storage)")
@RequiredArgsConstructor
public class ProntuarioController {

    private final ProntuarioService prontuarioService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload de prontuário")
    public ResponseEntity<ProntuarioResponseDTO> upload(
            @RequestParam UUID pacienteId,
            @RequestParam String titulo,
            @RequestParam(required = false) String descricao,
            @RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(prontuarioService.upload(pacienteId, titulo, descricao, file));
    }

    @GetMapping("/paciente/{pacienteId}")
    @Operation(summary = "Listar prontuários de um paciente")
    public ResponseEntity<Page<ProntuarioResponseDTO>> getByPaciente(
            @PathVariable UUID pacienteId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(prontuarioService.getByPaciente(pacienteId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obter prontuário por ID (com URL assinada)")
    public ResponseEntity<ProntuarioResponseDTO> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(prontuarioService.getById(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir prontuário")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        prontuarioService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
