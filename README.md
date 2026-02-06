# Patient Service - Clinica Humaniza RJ

Microsservico para gestao de pacientes da Clinica Humaniza, com autenticacao JWT, documentacao OpenAPI e cobertura de testes.

## Stack

- **Java 17** + **Spring Boot 3.5.5**
- **Spring Data JPA** + **H2** (desenvolvimento)
- **Spring Security** + **JWT** (jjwt 0.12.6)
- **SpringDoc OpenAPI** 2.8.4 (Swagger UI)
- **Lombok** + **Jakarta Validation**
- **JUnit 5** + **Mockito** + **AssertJ**

## Estrutura do Projeto

```
src/main/java/br/com/clinicahumaniza/patient_service/
├── config/
│   └── OpenApiConfig.java          # Configuracao Swagger/OpenAPI com JWT Bearer
├── controller/
│   ├── AuthController.java         # POST /api/auth/registrar, /api/auth/login
│   └── PatientController.java      # CRUD /api/v1/patients (protegido)
├── dto/
│   ├── AuthResponseDTO.java        # Resposta de auth (token, tipo, nome, email, role)
│   ├── LoginRequestDTO.java        # Request de login (email, senha)
│   ├── RegisterRequestDTO.java     # Request de registro (nome, email, senha)
│   ├── PatientRequestDTO.java      # Request de criacao de paciente
│   ├── PatientResponseDTO.java     # Resposta de paciente (sem CPF por seguranca)
│   └── PatientUpdateDTO.java       # Request de atualizacao parcial
├── exception/
│   ├── DuplicateResourceException.java   # 409 - recurso duplicado
│   ├── GlobalExceptionHandler.java       # Handler global de excecoes
│   └── PatientNotFoundException.java     # 404 - paciente nao encontrado
├── mapper/
│   └── PatientMapper.java          # Conversao Entity <-> DTO
├── model/
│   ├── Patient.java                # Entidade paciente (soft delete, LGPD)
│   ├── Role.java                   # Enum: ROLE_USER, ROLE_ADMIN
│   └── User.java                   # Entidade usuario do sistema
├── repository/
│   ├── PatientRepository.java      # JPA Repository de pacientes
│   └── UserRepository.java         # JPA Repository de usuarios
├── security/
│   ├── JwtAuthenticationFilter.java # Filtro JWT (OncePerRequestFilter)
│   ├── JwtService.java             # Geracao e validacao de tokens JWT
│   ├── SecurityConfig.java         # SecurityFilterChain, BCrypt, CORS/CSRF
│   └── UserDetailsServiceImpl.java # Carrega User do banco para Spring Security
└── service/
    ├── AuthService.java            # Logica de registro e login
    └── PatientService.java         # Logica de CRUD de pacientes
```

## Como Executar

```bash
# Compilar
./mvnw compile

# Rodar a aplicacao (porta 8080)
./mvnw spring-boot:run

# Rodar testes (38 testes)
./mvnw test
```

## Endpoints da API

### Autenticacao (publico)

| Metodo | Endpoint              | Descricao                | Status |
|--------|-----------------------|--------------------------|--------|
| POST   | `/api/auth/registrar` | Registrar usuario        | 201    |
| POST   | `/api/auth/login`     | Login (retorna JWT)      | 200    |

**Registro:**
```json
POST /api/auth/registrar
{
  "nome": "Maria Silva",
  "email": "maria@email.com",
  "senha": "senha123"
}
```

**Login:**
```json
POST /api/auth/login
{
  "email": "maria@email.com",
  "senha": "senha123"
}
```

**Resposta (ambos):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tipo": "Bearer",
  "nome": "Maria Silva",
  "email": "maria@email.com",
  "role": "ROLE_USER"
}
```

### Pacientes (requer JWT)

Adicionar header: `Authorization: Bearer <token>`

| Metodo | Endpoint                 | Descricao               | Status |
|--------|--------------------------|-------------------------|--------|
| POST   | `/api/v1/patients`       | Cadastrar paciente      | 201    |
| GET    | `/api/v1/patients`       | Listar pacientes ativos | 200    |
| GET    | `/api/v1/patients/{id}`  | Buscar por ID           | 200    |
| PUT    | `/api/v1/patients/{id}`  | Atualizar paciente      | 200    |
| DELETE | `/api/v1/patients/{id}`  | Desativar (soft delete) | 204    |

**Criar paciente:**
```json
POST /api/v1/patients
{
  "nomeCompleto": "Joao Santos",
  "email": "joao@email.com",
  "cpf": "12345678901",
  "dataNascimento": "1990-01-15",
  "telefone": "21999999999"
}
```

### Documentacao e Ferramentas (publico)

| URL                              | Descricao           |
|----------------------------------|---------------------|
| `/swagger-ui.html`               | Swagger UI          |
| `/v3/api-docs`                   | OpenAPI JSON        |
| `/h2-console`                    | Console H2 Database |

## Seguranca

- **Autenticacao:** JWT stateless com HMAC-SHA256
- **Senhas:** BCrypt hash
- **Rotas publicas:** `/api/auth/**`, `/swagger-ui/**`, `/v3/api-docs/**`, `/h2-console/**`
- **Rotas protegidas:** Todas as demais requerem token JWT valido
- **Sessao:** STATELESS (sem cookies/sessao)
- **CSRF:** Desabilitado (API REST stateless)
- **Token:** Expiracao de 24h

## Entidades

### User (usuarios do sistema)
Representa funcionarios da clinica (recepcionistas, administradores). Usa autenticacao JWT.

| Campo  | Tipo   | Descricao              |
|--------|--------|------------------------|
| id     | UUID   | Chave primaria         |
| nome   | String | Nome do usuario        |
| email  | String | E-mail (unico, login)  |
| senha  | String | Senha BCrypt           |
| role   | Enum   | ROLE_USER, ROLE_ADMIN  |

### Patient (pacientes da clinica)
Representa os pacientes atendidos. Gerenciado pelos Users autenticados.

| Campo             | Tipo        | Descricao                    |
|-------------------|-------------|------------------------------|
| id                | UUID        | Chave primaria               |
| nomeCompleto      | String      | Nome completo                |
| email             | String      | E-mail (unico)               |
| cpf               | String      | CPF 11 digitos (unico)       |
| dataNascimento    | LocalDate   | Data de nascimento           |
| telefone          | String      | Telefone                     |
| endereco          | String      | Endereco (opcional)          |
| profissao         | String      | Profissao (opcional)         |
| estadoCivil       | String      | Estado civil (opcional)      |
| statusAtivo       | boolean     | Soft delete flag             |
| consentimentoLgpd | boolean     | Consentimento LGPD           |
| createdAt         | DateTime    | Data de criacao              |
| updatedAt         | DateTime    | Data de atualizacao          |

## Testes

38 testes organizados em 4 categorias:

| Categoria              | Arquivo                    | Testes | Tipo        |
|------------------------|----------------------------|--------|-------------|
| Service - Pacientes    | PatientServiceTest.java    | 9      | Unitario    |
| Service - Auth         | AuthServiceTest.java       | 4      | Unitario    |
| Security - JWT         | JwtServiceTest.java        | 5      | Unitario    |
| Controller - Pacientes | PatientControllerTest.java | 8      | WebMvcTest  |
| Controller - Auth      | AuthControllerTest.java    | 5      | WebMvcTest  |
| Integracao - Auth      | AuthIntegrationTest.java   | 3      | SpringBoot  |
| Integracao - Pacientes | PatientIntegrationTest.java| 3      | SpringBoot  |
| Contexto               | PatientServiceApplicationTests | 1  | SpringBoot  |

## Configuracao

### application.properties
- `jwt.secret` — Chave Base64 para assinatura JWT (minimo 256 bits)
- `jwt.expiration` — Tempo de expiracao do token em ms (padrao: 86400000 = 24h)
- H2 in-memory database em `jdbc:h2:mem:testdb`

### application-test.properties
- Chave JWT separada para testes
- SQL logging desabilitado
- `ddl-auto=create-drop`
