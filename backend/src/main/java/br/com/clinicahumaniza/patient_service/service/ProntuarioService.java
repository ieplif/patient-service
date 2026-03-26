package br.com.clinicahumaniza.patient_service.service;

import br.com.clinicahumaniza.patient_service.dto.ProntuarioResponseDTO;
import br.com.clinicahumaniza.patient_service.model.Patient;
import br.com.clinicahumaniza.patient_service.model.Prontuario;
import br.com.clinicahumaniza.patient_service.repository.PatientRepository;
import br.com.clinicahumaniza.patient_service.repository.ProntuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProntuarioService {

    private final ProntuarioRepository prontuarioRepository;
    private final PatientRepository patientRepository;
    private final SupabaseStorageService storageService;

    public ProntuarioResponseDTO upload(UUID pacienteId, String titulo, String descricao, MultipartFile file) throws IOException {
        Patient paciente = patientRepository.findById(pacienteId)
                .orElseThrow(() -> new EntityNotFoundException("Paciente não encontrado"));

        String storagePath = storageService.upload(pacienteId, file);
        String storageUrl = storageService.getSignedUrl(storagePath);

        String uploadedBy = SecurityContextHolder.getContext().getAuthentication().getName();

        Prontuario prontuario = Prontuario.builder()
                .paciente(paciente)
                .titulo(titulo)
                .descricao(descricao)
                .nomeArquivo(file.getOriginalFilename())
                .tipoArquivo(file.getContentType())
                .tamanhoBytes(file.getSize())
                .storagePath(storagePath)
                .storageUrl(storageUrl)
                .uploadedBy(uploadedBy)
                .build();

        prontuario = prontuarioRepository.save(prontuario);
        return toDTO(prontuario);
    }

    public Page<ProntuarioResponseDTO> getByPaciente(UUID pacienteId, Pageable pageable) {
        return prontuarioRepository.findByPacienteId(pacienteId, pageable).map(this::toDTO);
    }

    public ProntuarioResponseDTO getById(UUID id) {
        Prontuario p = prontuarioRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Prontuário não encontrado"));
        // Refresh signed URL
        p.setStorageUrl(storageService.getSignedUrl(p.getStoragePath()));
        return toDTO(p);
    }

    public void delete(UUID id) {
        Prontuario p = prontuarioRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Prontuário não encontrado"));
        storageService.delete(p.getStoragePath());
        prontuarioRepository.delete(p);
    }

    private ProntuarioResponseDTO toDTO(Prontuario p) {
        ProntuarioResponseDTO dto = new ProntuarioResponseDTO();
        dto.setId(p.getId());
        dto.setPacienteId(p.getPaciente().getId());
        dto.setPacienteNome(p.getPaciente().getNomeCompleto());
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
