package br.com.clinicahumaniza.patient_service.config;

import br.com.clinicahumaniza.patient_service.model.Role;
import br.com.clinicahumaniza.patient_service.model.User;
import br.com.clinicahumaniza.patient_service.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Cria os usuários iniciais do sistema na primeira execução.
 * Idempotente: verifica se o usuário já existe antes de criar.
 * Configure via variáveis de ambiente (veja .env.example).
 */
@Component
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Usuário 1 — Admin principal
    @Value("${app.seed.admin1.email:caissa@humaniza.com}")
    private String admin1Email;

    @Value("${app.seed.admin1.nome:Caissa Humaniza}")
    private String admin1Nome;

    @Value("${app.seed.admin1.password:Humaniza@2025}")
    private String admin1Password;

    // Usuário 2 — Opcional (configurado via env var)
    @Value("${app.seed.admin2.email:}")
    private String admin2Email;

    @Value("${app.seed.admin2.nome:}")
    private String admin2Nome;

    @Value("${app.seed.admin2.password:}")
    private String admin2Password;

    // Usuário profissional — para teste de acesso restrito
    @Value("${app.seed.profissional1.email:}")
    private String prof1Email;

    @Value("${app.seed.profissional1.nome:}")
    private String prof1Nome;

    @Value("${app.seed.profissional1.password:}")
    private String prof1Password;

    @Autowired
    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (admin1Email != null && !admin1Email.isBlank()) {
            criarUsuarioSeNaoExistir(admin1Email, admin1Nome, admin1Password, Role.ROLE_ADMIN);
        }

        if (admin2Email != null && !admin2Email.isBlank()) {
            criarUsuarioSeNaoExistir(admin2Email, admin2Nome, admin2Password, Role.ROLE_ADMIN);
        }

        if (prof1Email != null && !prof1Email.isBlank()) {
            criarUsuarioSeNaoExistir(prof1Email, prof1Nome, prof1Password, Role.ROLE_PROFISSIONAL);
        }
    }

    private void criarUsuarioSeNaoExistir(String email, String nome, String password, Role role) {
        if (email == null || email.isBlank()) return;
        if (password == null || password.isBlank()) {
            log.warn("Senha não configurada para {}. Usuário não será criado.", email);
            return;
        }
        if (userRepository.existsByEmail(email)) {
            log.info("Usuário já existe, ignorando seed: {}", email);
            return;
        }
        if (password.equals("Humaniza@2025") || password.equals("Prof@2025")) {
            log.warn("⚠️  ATENÇÃO: Usuário {} criado com senha padrão. Altere imediatamente via /api/auth/alterar-senha", email);
        }

        User user = new User();
        user.setNome(nome);
        user.setEmail(email);
        user.setSenha(passwordEncoder.encode(password));
        user.setRole(role);
        userRepository.save(user);
        log.info("Usuário criado com sucesso: {} ({})", email, role);
    }
}
