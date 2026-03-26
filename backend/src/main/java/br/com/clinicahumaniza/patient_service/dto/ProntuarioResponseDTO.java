package br.com.clinicahumaniza.patient_service.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ProntuarioResponseDTO {
    private UUID id;
    private UUID pacienteId;
    private String pacienteNome;
    private String titulo;
    private String descricao;
    private String nomeArquivo;
    private String tipoArquivo;
    private Long tamanhoBytes;
    private String storagePath;
    private String storageUrl;
    private String uploadedBy;
    private LocalDateTime createdAt;
}
