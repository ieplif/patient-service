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
│   └── OpenApiConfig.java              # Configuracao Swagger/OpenAPI com JWT Bearer
├── controller/
│   ├── AuthController.java             # POST /api/auth/registrar, /api/auth/login
│   ├── PatientController.java          # CRUD /api/v1/patients (protegido)
│   ├── AtividadeController.java        # CRUD /api/v1/atividades (protegido)
│   ├── PlanoController.java            # CRUD /api/v1/planos (protegido)
│   ├── ServicoController.java          # CRUD /api/v1/servicos (protegido)
│   └── AssinaturaController.java       # CRUD + sessoes /api/v1/assinaturas (protegido)
├── dto/
│   ├── AuthResponseDTO.java            # Resposta de auth (token, tipo, nome, email, role)
│   ├── LoginRequestDTO.java            # Request de login (email, senha)
│   ├── RegisterRequestDTO.java         # Request de registro (nome, email, senha)
│   ├── PatientRequestDTO.java          # Request de criacao de paciente
│   ├── PatientResponseDTO.java         # Resposta de paciente (sem CPF por seguranca)
│   ├── PatientUpdateDTO.java           # Request de atualizacao parcial
│   ├── AtividadeRequestDTO.java        # Request de criacao de atividade
│   ├── AtividadeResponseDTO.java       # Resposta de atividade
│   ├── AtividadeUpdateDTO.java         # Request de atualizacao de atividade
│   ├── PlanoRequestDTO.java            # Request de criacao de plano
│   ├── PlanoResponseDTO.java           # Resposta de plano
│   ├── PlanoUpdateDTO.java             # Request de atualizacao de plano
│   ├── ServicoRequestDTO.java          # Request de criacao de servico
│   ├── ServicoResponseDTO.java         # Resposta de servico
│   ├── ServicoUpdateDTO.java           # Request de atualizacao de servico
│   ├── AssinaturaRequestDTO.java       # Request de criacao de assinatura
│   ├── AssinaturaResponseDTO.java      # Resposta de assinatura (com sessoesRestantes)
│   ├── AssinaturaUpdateDTO.java        # Request de atualizacao de assinatura
│   └── AssinaturaStatusDTO.java        # Request de alteracao de status
├── exception/
│   ├── BusinessException.java          # 422 - erro de regra de negocio
│   ├── DuplicateResourceException.java # 409 - recurso duplicado
│   ├── GlobalExceptionHandler.java     # Handler global de excecoes
│   ├── PatientNotFoundException.java   # 404 - paciente nao encontrado
│   └── ResourceNotFoundException.java  # 404 - recurso generico nao encontrado
├── mapper/
│   ├── PatientMapper.java              # Conversao Patient Entity <-> DTO
│   ├── AtividadeMapper.java            # Conversao Atividade Entity <-> DTO
│   ├── PlanoMapper.java                # Conversao Plano Entity <-> DTO
│   ├── ServicoMapper.java              # Conversao Servico Entity <-> DTO
│   └── AssinaturaMapper.java           # Conversao Assinatura Entity <-> DTO
├── model/
│   ├── Patient.java                    # Entidade paciente (soft delete, LGPD)
│   ├── Role.java                       # Enum: ROLE_USER, ROLE_ADMIN
│   ├── User.java                       # Entidade usuario do sistema
│   ├── Atividade.java                  # Entidade atividade (ex: Pilates, Fisioterapia)
│   ├── Plano.java                      # Entidade plano (ex: Mensal, Trimestral)
│   ├── Servico.java                    # Entidade servico (atividade + plano + valor)
│   ├── Assinatura.java                 # Entidade assinatura (paciente + servico + sessoes)
│   └── StatusAssinatura.java           # Enum: ATIVO, CANCELADO, VENCIDO, FINALIZADO
├── repository/
│   ├── PatientRepository.java          # JPA Repository de pacientes
│   ├── UserRepository.java             # JPA Repository de usuarios
│   ├── AtividadeRepository.java        # JPA Repository de atividades
│   ├── PlanoRepository.java            # JPA Repository de planos
│   ├── ServicoRepository.java          # JPA Repository de servicos
│   └── AssinaturaRepository.java       # JPA Repository de assinaturas
├── security/
│   ├── JwtAuthenticationFilter.java    # Filtro JWT (OncePerRequestFilter)
│   ├── JwtService.java                 # Geracao e validacao de tokens JWT
│   ├── SecurityConfig.java             # SecurityFilterChain, BCrypt, CORS/CSRF
│   └── UserDetailsServiceImpl.java     # Carrega User do banco para Spring Security
└── service/
    ├── AuthService.java                # Logica de registro e login
    ├── PatientService.java             # Logica de CRUD de pacientes
    ├── AtividadeService.java           # Logica de CRUD de atividades
    ├── PlanoService.java               # Logica de CRUD de planos
    ├── ServicoService.java             # Logica de CRUD de servicos
    └── AssinaturaService.java          # Logica de assinaturas, sessoes e status
```

## Como Executar

```bash
# Compilar
./mvnw compile

# Rodar a aplicacao (porta 8080)
./mvnw spring-boot:run

# Rodar testes (116 testes)
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

### Atividades (requer JWT)

| Metodo | Endpoint                    | Descricao                  | Status |
|--------|-----------------------------|----------------------------|--------|
| POST   | `/api/v1/atividades`        | Criar atividade            | 201    |
| GET    | `/api/v1/atividades`        | Listar atividades ativas   | 200    |
| GET    | `/api/v1/atividades/{id}`   | Buscar por ID              | 200    |
| PUT    | `/api/v1/atividades/{id}`   | Atualizar atividade        | 200    |
| DELETE | `/api/v1/atividades/{id}`   | Desativar (soft delete)    | 204    |

### Planos (requer JWT)

| Metodo | Endpoint                | Descricao              | Status |
|--------|-------------------------|------------------------|--------|
| POST   | `/api/v1/planos`        | Criar plano            | 201    |
| GET    | `/api/v1/planos`        | Listar planos ativos   | 200    |
| GET    | `/api/v1/planos/{id}`   | Buscar por ID          | 200    |
| PUT    | `/api/v1/planos/{id}`   | Atualizar plano        | 200    |
| DELETE | `/api/v1/planos/{id}`   | Desativar (soft delete) | 204   |

### Servicos (requer JWT)

| Metodo | Endpoint                                  | Descricao                | Status |
|--------|-------------------------------------------|--------------------------|--------|
| POST   | `/api/v1/servicos`                        | Criar servico            | 201    |
| GET    | `/api/v1/servicos`                        | Listar servicos ativos   | 200    |
| GET    | `/api/v1/servicos/{id}`                   | Buscar por ID            | 200    |
| GET    | `/api/v1/servicos/atividade/{atividadeId}`| Listar por atividade     | 200    |
| GET    | `/api/v1/servicos/plano/{planoId}`        | Listar por plano         | 200    |
| PUT    | `/api/v1/servicos/{id}`                   | Atualizar servico        | 200    |
| DELETE | `/api/v1/servicos/{id}`                   | Desativar (soft delete)  | 204    |

### Assinaturas (requer JWT)

| Metodo | Endpoint                                       | Descricao                       | Status |
|--------|-------------------------------------------------|---------------------------------|--------|
| POST   | `/api/v1/assinaturas`                           | Criar assinatura                | 201    |
| GET    | `/api/v1/assinaturas`                           | Listar assinaturas ativas       | 200    |
| GET    | `/api/v1/assinaturas/{id}`                      | Buscar por ID                   | 200    |
| GET    | `/api/v1/assinaturas/paciente/{pacienteId}`     | Listar por paciente             | 200    |
| GET    | `/api/v1/assinaturas/servico/{servicoId}`       | Listar por servico              | 200    |
| PUT    | `/api/v1/assinaturas/{id}`                      | Atualizar assinatura            | 200    |
| PATCH  | `/api/v1/assinaturas/{id}/status`               | Alterar status                  | 200    |
| PATCH  | `/api/v1/assinaturas/{id}/registrar-sessao`     | Registrar sessao realizada      | 200    |
| DELETE | `/api/v1/assinaturas/{id}`                      | Desativar (soft delete)         | 204    |

**Criar assinatura:**
```json
POST /api/v1/assinaturas
{
  "pacienteId": "uuid-do-paciente",
  "servicoId": "uuid-do-servico",
  "dataInicio": "2025-01-01",
  "sessoesContratadas": 4,
  "valor": 350.00,
  "observacoes": "Assinatura mensal de Pilates"
}
```

**Alterar status:**
```json
PATCH /api/v1/assinaturas/{id}/status
{
  "status": "CANCELADO"
}
```

**Registrar sessao (sem body):**
```
PATCH /api/v1/assinaturas/{id}/registrar-sessao
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

### Atividade (atividades da clinica)
Representa as atividades oferecidas (ex: Pilates, Fisioterapia, Yoga).

| Campo         | Tipo     | Descricao                        |
|---------------|----------|----------------------------------|
| id            | UUID     | Chave primaria                   |
| nome          | String   | Nome da atividade (unico)        |
| descricao     | String   | Descricao da atividade           |
| duracaoPadrao | Integer  | Duracao padrao em minutos        |
| ativo         | boolean  | Soft delete flag                 |
| createdAt     | DateTime | Data de criacao                  |
| updatedAt     | DateTime | Data de atualizacao              |

### Plano (planos da clinica)
Representa os planos oferecidos (ex: Mensal, Trimestral, Avulso).

| Campo                | Tipo     | Descricao                         |
|----------------------|----------|-----------------------------------|
| id                   | UUID     | Chave primaria                    |
| nome                 | String   | Nome do plano (unico)             |
| descricao            | String   | Descricao do plano                |
| tipoPlano            | String   | Tipo (mensal, trimestral, avulso) |
| validadeDias         | Integer  | Validade em dias                  |
| sessoesIncluidas     | Integer  | Qtd de sessoes incluidas          |
| permiteTransferencia | boolean  | Permite transferencia de sessoes  |
| ativo                | boolean  | Soft delete flag                  |
| createdAt            | DateTime | Data de criacao                   |
| updatedAt            | DateTime | Data de atualizacao               |

### Servico (servicos da clinica)
Combina uma Atividade com um Plano, definindo valor e modalidade.

| Campo           | Tipo       | Descricao                          |
|-----------------|------------|------------------------------------|
| id              | UUID       | Chave primaria                     |
| atividade       | Atividade  | FK ManyToOne                       |
| plano           | Plano      | FK ManyToOne                       |
| tipoAtendimento | String     | individual, grupo, etc.            |
| quantidade      | Integer    | Qtd de sessoes                     |
| unidadeServico  | String     | sessao, pacote, etc.               |
| modalidadeLocal | String     | clinica, domicilio, online         |
| valor           | BigDecimal | Valor do servico                   |
| ativo           | boolean    | Soft delete flag                   |
| createdAt       | DateTime   | Data de criacao                    |
| updatedAt       | DateTime   | Data de atualizacao                |

### Assinatura (assinaturas de pacientes)
Vincula um Paciente a um Servico com controle de vigencia, sessoes e status.

| Campo              | Tipo             | Descricao                                |
|--------------------|------------------|------------------------------------------|
| id                 | UUID             | Chave primaria                           |
| paciente           | Patient          | FK ManyToOne                             |
| servico            | Servico          | FK ManyToOne                             |
| dataInicio         | LocalDate        | Data de inicio                           |
| dataVencimento     | LocalDate        | Data de vencimento (calculada ou manual) |
| sessoesContratadas | Integer          | Qtd de sessoes contratadas               |
| sessoesRealizadas  | Integer          | Qtd de sessoes realizadas (default 0)    |
| status             | StatusAssinatura | ATIVO, CANCELADO, VENCIDO, FINALIZADO    |
| valor              | BigDecimal       | Valor da assinatura                      |
| observacoes        | String           | Anotacoes livres                         |
| ativo              | boolean          | Soft delete flag                         |
| createdAt          | DateTime         | Data de criacao                          |
| updatedAt          | DateTime         | Data de atualizacao                      |

**Campo calculado (no response):** `sessoesRestantes = sessoesContratadas - sessoesRealizadas`

**Regras de negocio:**
- Ao registrar sessao, se `sessoesRealizadas >= sessoesContratadas`, status muda para FINALIZADO automaticamente
- Nao e possivel registrar sessao em assinatura com status diferente de ATIVO
- Nao e possivel alterar status de assinatura FINALIZADA
- Se `dataVencimento` nao for informada na criacao, e calculada a partir de `dataInicio + plano.validadeDias`

## Testes

116 testes organizados em categorias:

| Categoria                | Arquivo                       | Testes | Tipo        |
|--------------------------|-------------------------------|--------|-------------|
| Service - Pacientes      | PatientServiceTest.java       | 9      | Unitario    |
| Service - Auth           | AuthServiceTest.java          | 4      | Unitario    |
| Service - Atividades     | AtividadeServiceTest.java     | 7      | Unitario    |
| Service - Planos         | PlanoServiceTest.java         | 7      | Unitario    |
| Service - Servicos       | ServicoServiceTest.java       | 10     | Unitario    |
| Service - Assinaturas    | AssinaturaServiceTest.java    | 15     | Unitario    |
| Security - JWT           | JwtServiceTest.java           | 5      | Unitario    |
| Controller - Pacientes   | PatientControllerTest.java    | 8      | WebMvcTest  |
| Controller - Auth        | AuthControllerTest.java       | 5      | WebMvcTest  |
| Controller - Atividades  | AtividadeControllerTest.java  | 7      | WebMvcTest  |
| Controller - Planos      | PlanoControllerTest.java      | 7      | WebMvcTest  |
| Controller - Servicos    | ServicoControllerTest.java    | 8      | WebMvcTest  |
| Controller - Assinaturas | AssinaturaControllerTest.java | 10     | WebMvcTest  |
| Integracao - Auth        | AuthIntegrationTest.java      | 3      | SpringBoot  |
| Integracao - Pacientes   | PatientIntegrationTest.java   | 3      | SpringBoot  |
| Integracao - Atividades  | AtividadeIntegrationTest.java | 3      | SpringBoot  |
| Integracao - Servicos    | ServicoIntegrationTest.java   | 3      | SpringBoot  |
| Integracao - Assinaturas | AssinaturaIntegrationTest.java| 3      | SpringBoot  |
| Contexto                 | PatientServiceApplicationTests| 1      | SpringBoot  |

## Configuracao

### application.properties
- `jwt.secret` — Chave Base64 para assinatura JWT (minimo 256 bits)
- `jwt.expiration` — Tempo de expiracao do token em ms (padrao: 86400000 = 24h)
- H2 in-memory database em `jdbc:h2:mem:testdb`

### application-test.properties
- Chave JWT separada para testes
- SQL logging desabilitado
- `ddl-auto=create-drop`
