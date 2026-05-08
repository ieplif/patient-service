package br.com.clinicahumaniza.patient_service.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "prontuarios")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Prontuario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Patient paciente;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    @Builder.Default
    private TipoDocumento tipo = TipoDocumento.PRONTUARIO;

    @Column(nullable = false)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    // Nome de arquivo pode ter caracteres acentuados/longos — TEXT evita "value too long"
    @Column(name = "nome_arquivo", columnDefinition = "TEXT")
    private String nomeArquivo;

    @Column(name = "tipo_arquivo")
    private String tipoArquivo;

    @Column(name = "tamanho_bytes")
    private Long tamanhoBytes;

    // Path interno (UUID/UUID_arquivo) — TEXT por segurança
    @Column(name = "storage_path", nullable = false, columnDefinition = "TEXT")
    private String storagePath;

    // URL assinada do Supabase contém JWT no querystring — passa facilmente de 1000 chars
    @Column(name = "storage_url", columnDefinition = "TEXT")
    private String storageUrl;

    @Column(name = "uploaded_by")
    private String uploadedBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
