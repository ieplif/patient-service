package br.com.clinicahumaniza.patient_service.service;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.EntityNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import br.com.clinicahumaniza.patient_service.dto.ProntuarioResponseDTO;
import br.com.clinicahumaniza.patient_service.model.Patient;
import br.com.clinicahumaniza.patient_service.model.Prontuario;
import br.com.clinicahumaniza.patient_service.model.TipoDocumento;
import br.com.clinicahumaniza.patient_service.repository.PatientRepository;
import br.com.clinicahumaniza.patient_service.repository.ProntuarioRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProntuarioService {

    private static final Logger log = LoggerFactory.getLogger(ProntuarioService.class);

    private final ProntuarioRepository prontuarioRepository;
    private final PatientRepository patientRepository;
    private final SupabaseStorageService storageService;

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    private static final long MAX_FILE_SIZE = 10_485_760L; // 10MB

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Arquivo não pode ser vazio");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Arquivo muito grande (máx 10MB)");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Tipo de arquivo não permitido. Use: PDF, JPEG, PNG, DOC ou DOCX");
        }
        // Sanitize filename
        String originalName = file.getOriginalFilename();
        if (originalName != null && (originalName.contains("..") || originalName.contains("/"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome de arquivo inválido");
        }
    }

    public ProntuarioResponseDTO upload(
            UUID pacienteId, TipoDocumento tipo, String titulo, String descricao, MultipartFile file)
            throws IOException {
        validateFile(file);
        Patient paciente = patientRepository
                .findById(pacienteId)
                .orElseThrow(() -> new EntityNotFoundException("Paciente não encontrado"));

        // Sobe primeiro no Storage (chamada externa, fora da transação JPA)
        String storagePath = storageService.upload(pacienteId, file);
        String storageUrl = storageService.getSignedUrl(storagePath);

        String uploadedBy =
                SecurityContextHolder.getContext().getAuthentication().getName();

        Prontuario prontuario = Prontuario.builder()
                .paciente(paciente)
                .tipo(tipo != null ? tipo : TipoDocumento.PRONTUARIO)
                .titulo(titulo)
                .descricao(descricao)
                .nomeArquivo(file.getOriginalFilename())
                .tipoArquivo(file.getContentType())
                .tamanhoBytes(file.getSize())
                .storagePath(storagePath)
                .storageUrl(storageUrl)
                .uploadedBy(uploadedBy)
                .build();

        // Rollback manual: se o save no banco falhar, remove o arquivo do Storage
        // pra evitar arquivos órfãos consumindo espaço e expondo dados de pacientes.
        try {
            prontuario = prontuarioRepository.save(prontuario);
        } catch (RuntimeException e) {
            log.error("Falha ao salvar Prontuario no banco. Removendo arquivo orfao do Storage: {}", storagePath, e);
            try {
                storageService.delete(storagePath);
            } catch (Exception cleanupEx) {
                log.error("Cleanup do arquivo orfao falhou (path={}): {}", storagePath, cleanupEx.getMessage());
            }
            throw e;
        }

        return toDTO(prontuario);
    }

    public Page<ProntuarioResponseDTO> getByPaciente(UUID pacienteId, TipoDocumento tipo, Pageable pageable) {
        Page<Prontuario> page = (tipo != null)
                ? prontuarioRepository.findByPacienteIdAndTipo(pacienteId, tipo, pageable)
                : prontuarioRepository.findByPacienteId(pacienteId, pageable);
        // Regenera a URL assinada na hora de servir: a guardada no banco expira em 1h
        // após o upload e causaria InvalidJWT ("exp" claim) ao abrir depois.
        return page.map(this::toDTOComUrlFresca);
    }

    public ProntuarioResponseDTO getById(UUID id) {
        Prontuario p = prontuarioRepository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Prontuário não encontrado"));
        return toDTOComUrlFresca(p);
    }

    public void delete(UUID id) {
        Prontuario p = prontuarioRepository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Prontuário não encontrado"));
        storageService.delete(p.getStoragePath());
        prontuarioRepository.delete(p);
    }

    /** DTO com URL assinada gerada na hora (evita servir URL expirada do banco). */
    private ProntuarioResponseDTO toDTOComUrlFresca(Prontuario p) {
        ProntuarioResponseDTO dto = toDTO(p);
        dto.setStorageUrl(storageService.getSignedUrl(p.getStoragePath()));
        return dto;
    }

    private ProntuarioResponseDTO toDTO(Prontuario p) {
        ProntuarioResponseDTO dto = new ProntuarioResponseDTO();
        dto.setId(p.getId());
        dto.setPacienteId(p.getPaciente().getId());
        dto.setPacienteNome(p.getPaciente().getNomeCompleto());
        dto.setTipo(p.getTipo() != null ? p.getTipo() : TipoDocumento.PRONTUARIO);
        dto.setTitulo(p.getTitulo());
        dto.setDescricao(p.getDescricao());
        dto.setNomeArquivo(p.getNomeArquivo());
        dto.setTipoArquivo(p.getTipoArquivo());
        dto.setTamanhoBytes(p.getTamanhoBytes());
        dto.setStoragePath(p.getStoragePath());
        dto.setStorageUrl(p.getStorageUrl());
        dto.setUploadedBy(p.getUploadedBy());
        dto.setCreatedAt(p.getCreatedAt());
        return dto;
    }
}
