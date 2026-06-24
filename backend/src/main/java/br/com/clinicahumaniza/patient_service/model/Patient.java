package br.com.clinicahumaniza.patient_service.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.*;

import br.com.clinicahumaniza.patient_service.util.BuscaNome;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity // 1. Informa ao JPA que esta classe é uma entidade e deve ser mapeada para uma tabela.
@Table(name = "patients") // 2. (Opcional) Especifica o nome da tabela no banco. Se omitido, o nome da classe será
// usado.
@Data // 3. Anotação do Lombok: gera automaticamente getters, setters, toString(), equals() e hashCode().
@NoArgsConstructor // 4. Anotação do Lombok: gera um construtor sem argumentos (requerido pelo JPA).
@AllArgsConstructor // 5. Anotação do Lombok: gera um construtor com todos os argumentos.
public class Patient {

    @Id // 6. Marca este campo como a chave primária (Primary Key) da tabela.
    @GeneratedValue(
            strategy = GenerationType.UUID) // 7. Configura a estratégia de geração do valor da chave. Usaremos UUID.
    private UUID id;

    @Column(
            nullable = false,
            length = 255) // 8. Mapeia para uma coluna. `nullable = false` cria uma restrição NOT NULL.
    private String nomeCompleto;

    // Versão normalizada do nome (sem acento, minúscula, espaços colapsados) usada
    // pelas buscas flexíveis. Mantida automaticamente nos hooks de persistência.
    @Column(length = 255)
    private String nomeNormalizado;

    // Email opcional — quando informado, a unicidade ainda vale (PostgreSQL permite múltiplos NULLs em UNIQUE).
    @Column(unique = true, length = 255)
    private String email;

    // CPF opcional — pode ser preenchido depois (necessário p/ emissão de recibo/NF).
    @Column(unique = true, length = 11)
    private String cpf;

    private LocalDate dataNascimento;

    @Column(nullable = false, length = 20)
    private String telefone;

    @Column(columnDefinition = "TEXT") // 10. Para campos de texto mais longos.
    private String endereco;

    @Column(length = 100)
    private String profissao;

    @Column(length = 50)
    private String estadoCivil;

    @Column(length = 150)
    private String medicoResponsavel;

    private boolean statusAtivo = true; // 11. Um valor padrão pode ser definido diretamente.

    private boolean consentimentoLgpd = false;

    private LocalDateTime dataConsentimentoLgpd;

    @Column(updatable = false) // 12. Este campo não será atualizado após a criação.
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist // 13. Método executado antes de a entidade ser salva pela primeira vez.
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        nomeNormalizado = BuscaNome.normalizar(nomeCompleto);
    }

    @PreUpdate // 14. Método executado antes de uma entidade existente ser atualizada.
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        nomeNormalizado = BuscaNome.normalizar(nomeCompleto);
    }
}
