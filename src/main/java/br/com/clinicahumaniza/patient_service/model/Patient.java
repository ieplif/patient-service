package br.com.clinicahumaniza.patient_service.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity // 1. Informa ao JPA que esta classe é uma entidade e deve ser mapeada para uma tabela.
@Table(name = "patients") // 2. (Opcional) Especifica o nome da tabela no banco. Se omitido, o nome da classe seria usado.
@Data // 3. Anotação do Lombok: gera automaticamente getters, setters, toString(), equals() e hashCode().
@NoArgsConstructor // 4. Anotação do Lombok: gera um construtor sem argumentos (requerido pelo JPA).
@AllArgsConstructor // 5. Anotação do Lombok: gera um construtor com todos os argumentos.
public class Patient {

    @Id // 6. Marca este campo como a chave primária (Primary Key) da tabela.
    @GeneratedValue(strategy = GenerationType.UUID) // 7. Configura a estratégia de geração do valor da chave. Usaremos UUID.
    private UUID id;

    @Column(nullable = false, length = 255) // 8. Mapeia para uma coluna. `nullable = false` cria uma restrição NOT NULL.
    private String nomeCompleto;

    @Column(nullable = false, unique = true, length = 255) // 9. `unique = true` garante que não haverá e-mails duplicados.
    private String email;

    @Column(nullable = false, unique = true, length = 11)
    private String cpf;

    @Column(nullable = false)
    private LocalDate dataNascimento;

    @Column(nullable = false, length = 20)
    private String telefone;

    @Column(columnDefinition = "TEXT") // 10. Para campos de texto mais longos.
    private String endereco;

    @Column(length = 100)
    private String profissao;

    @Column(length = 50)
    private String estadoCivil;

    private boolean statusAtivo = true; // 11. Um valor padrão pode ser definido diretamente.

    private boolean consentimentoLgpd = false;

    @Column(updatable = false) // 12. Este campo não será atualizado após a criação.
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist // 13. Método executado antes de a entidade ser salva pela primeira vez.
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate // 14. Método executado antes de uma entidade existente ser atualizada.
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
