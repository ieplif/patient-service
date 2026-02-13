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
│   ├── OpenApiConfig.java              # Configuracao Swagger/OpenAPI com JWT Bearer
│   ├── AsyncConfig.java                # Configuracao de execucao assincrona
│   └── GoogleCalendarConfig.java       # Configuracao do Google Calendar
├── controller/
│   ├── AuthController.java             # POST /api/auth/registrar, /api/auth/login
│   ├── PatientController.java          # CRUD /api/v1/patients
│   ├── AtividadeController.java        # CRUD /api/v1/atividades
│   ├── PlanoController.java            # CRUD /api/v1/planos
│   ├── ServicoController.java          # CRUD /api/v1/servicos
│   ├── ProfissionalController.java     # CRUD /api/v1/profissionais
│   ├── AssinaturaController.java       # CRUD + sessoes /api/v1/assinaturas
│   ├── HorarioDisponivelController.java# CRUD /api/v1/disponibilidades
│   ├── AgendamentoController.java      # CRUD + status + slots /api/v1/agendamentos
│   └── PagamentoController.java        # CRUD + parcelas /api/v1/pagamentos
├── dto/
│   ├── AuthResponseDTO.java            # Resposta de auth (token, tipo, nome, email, role)
│   ├── LoginRequestDTO.java            # Request de login (email, senha)
│   ├── RegisterRequestDTO.java         # Request de registro (nome, email, senha)
│   ├── Patient*DTO.java                # Request, Response, Update de paciente
│   ├── Atividade*DTO.java              # Request, Response, Update de atividade
│   ├── Plano*DTO.java                  # Request, Response, Update de plano
│   ├── Servico*DTO.java                # Request, Response, Update de servico
│   ├── Profissional*DTO.java           # Request, Response, Update de profissional
│   ├── Assinatura*DTO.java             # Request, Response, Update, Status de assinatura
│   ├── HorarioDisponivel*DTO.java      # Request, Response, Update de horario
│   ├── Agendamento*DTO.java            # Request, Response, Update, Status de agendamento
│   ├── Pagamento*DTO.java              # Request, Response, Update, Status de pagamento
│   └── Parcela*DTO.java                # Response, Status de parcela
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
│   ├── ProfissionalMapper.java         # Conversao Profissional Entity <-> DTO
│   ├── AssinaturaMapper.java           # Conversao Assinatura Entity <-> DTO
│   ├── HorarioDisponivelMapper.java    # Conversao HorarioDisponivel Entity <-> DTO
│   ├── AgendamentoMapper.java          # Conversao Agendamento Entity <-> DTO
│   └── PagamentoMapper.java            # Conversao Pagamento/Parcela Entity <-> DTO
├── model/
│   ├── Patient.java                    # Entidade paciente (soft delete, LGPD)
│   ├── Role.java                       # Enum: ROLE_USER, ROLE_ADMIN
│   ├── User.java                       # Entidade usuario do sistema
│   ├── Atividade.java                  # Entidade atividade (ex: Pilates, Fisioterapia)
│   ├── Plano.java                      # Entidade plano (ex: Mensal, Trimestral)
│   ├── Servico.java                    # Entidade servico (atividade + plano + valor)
│   ├── Profissional.java               # Entidade profissional (com atividades e Google Calendar)
│   ├── StatusAssinatura.java           # Enum: ATIVO, CANCELADO, VENCIDO, FINALIZADO
│   ├── Assinatura.java                 # Entidade assinatura (paciente + servico + sessoes)
│   ├── HorarioDisponivel.java          # Entidade horario disponivel do profissional
│   ├── StatusAgendamento.java          # Enum: AGENDADO, CONFIRMADO, REALIZADO, CANCELADO, NAO_COMPARECEU
│   ├── Agendamento.java                # Entidade agendamento (paciente + profissional + servico)
│   ├── FormaPagamento.java             # Enum: PIX, CARTAO_CREDITO, CARTAO_DEBITO, DINHEIRO
│   ├── StatusPagamento.java            # Enum: PENDENTE, PARCIALMENTE_PAGO, PAGO, CANCELADO, REEMBOLSADO
│   ├── StatusParcela.java              # Enum: PENDENTE, PAGO, ATRASADO, CANCELADO
│   ├── Pagamento.java                  # Entidade pagamento (vinculo com assinatura/agendamento)
│   └── Parcela.java                    # Entidade parcela (vinculo com pagamento)
├── repository/
│   ├── PatientRepository.java          # JPA Repository de pacientes
│   ├── UserRepository.java             # JPA Repository de usuarios
│   ├── AtividadeRepository.java        # JPA Repository de atividades
│   ├── PlanoRepository.java            # JPA Repository de planos
│   ├── ServicoRepository.java          # JPA Repository de servicos
│   ├── ProfissionalRepository.java     # JPA Repository de profissionais
│   ├── AssinaturaRepository.java       # JPA Repository de assinaturas
│   ├── HorarioDisponivelRepository.java# JPA Repository de horarios disponiveis
│   ├── AgendamentoRepository.java      # JPA Repository de agendamentos
│   ├── PagamentoRepository.java        # JPA Repository de pagamentos
│   └── ParcelaRepository.java          # JPA Repository de parcelas
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
    ├── ProfissionalService.java        # Logica de CRUD de profissionais
    ├── AssinaturaService.java          # Logica de assinaturas, sessoes e status
    ├── HorarioDisponivelService.java   # Logica de horarios disponiveis
    ├── AgendamentoService.java         # Logica de agendamentos, validacoes e slots
    ├── GoogleCalendarService.java      # Integracao com Google Calendar (opcional)
    └── PagamentoService.java           # Logica de pagamentos, parcelas e status
```

## Como Executar

```bash
# Compilar
./mvnw compile

# Rodar a aplicacao (porta 8080)
./mvnw spring-boot:run

# Rodar testes (257 testes)
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

### Profissionais (requer JWT)

| Metodo | Endpoint                                            | Descricao                       | Status |
|--------|-----------------------------------------------------|---------------------------------|--------|
| POST   | `/api/v1/profissionais`                             | Criar profissional              | 201    |
| GET    | `/api/v1/profissionais`                             | Listar profissionais ativos     | 200    |
| GET    | `/api/v1/profissionais/{id}`                        | Buscar por ID                   | 200    |
| GET    | `/api/v1/profissionais/atividade/{atividadeId}`     | Listar por atividade            | 200    |
| PUT    | `/api/v1/profissionais/{id}`                        | Atualizar profissional          | 200    |
| DELETE | `/api/v1/profissionais/{id}`                        | Desativar (soft delete)         | 204    |

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

### Horarios Disponiveis (requer JWT)

| Metodo | Endpoint                                                      | Descricao                          | Status |
|--------|---------------------------------------------------------------|------------------------------------|--------|
| POST   | `/api/v1/disponibilidades`                                    | Criar horario disponivel           | 201    |
| GET    | `/api/v1/disponibilidades`                                    | Listar horarios disponiveis        | 200    |
| GET    | `/api/v1/disponibilidades/{id}`                               | Buscar por ID                      | 200    |
| GET    | `/api/v1/disponibilidades/profissional/{profissionalId}`      | Listar por profissional            | 200    |
| PUT    | `/api/v1/disponibilidades/{id}`                               | Atualizar horario                  | 200    |
| DELETE | `/api/v1/disponibilidades/{id}`                               | Excluir horario                    | 204    |

### Agendamentos (requer JWT)

| Metodo | Endpoint                                                             | Descricao                       | Status |
|--------|----------------------------------------------------------------------|---------------------------------|--------|
| POST   | `/api/v1/agendamentos`                                               | Criar agendamento               | 201    |
| GET    | `/api/v1/agendamentos`                                               | Listar agendamentos             | 200    |
| GET    | `/api/v1/agendamentos/{id}`                                          | Buscar por ID                   | 200    |
| GET    | `/api/v1/agendamentos/paciente/{pacienteId}`                         | Listar por paciente             | 200    |
| GET    | `/api/v1/agendamentos/profissional/{profissionalId}`                 | Listar por profissional         | 200    |
| GET    | `/api/v1/agendamentos/data?inicio=...&fim=...`                       | Listar por periodo              | 200    |
| GET    | `/api/v1/agendamentos/profissional/{id}/slots-disponiveis?data=...`  | Consultar slots disponiveis     | 200    |
| PUT    | `/api/v1/agendamentos/{id}`                                          | Atualizar agendamento           | 200    |
| PATCH  | `/api/v1/agendamentos/{id}/status`                                   | Alterar status                  | 200    |
| DELETE | `/api/v1/agendamentos/{id}`                                          | Excluir agendamento             | 204    |

**Criar agendamento:**
```json
POST /api/v1/agendamentos
{
  "pacienteId": "uuid-do-paciente",
  "profissionalId": "uuid-do-profissional",
  "servicoId": "uuid-do-servico",
  "assinaturaId": "uuid-da-assinatura (opcional)",
  "dataHora": "2025-02-15T10:00:00",
  "duracaoMinutos": 50
}
```

### Pagamentos (requer JWT)

| Metodo | Endpoint                                                    | Descricao                          | Status |
|--------|-------------------------------------------------------------|------------------------------------|--------|
| POST   | `/api/v1/pagamentos`                                        | Criar pagamento                    | 201    |
| GET    | `/api/v1/pagamentos`                                        | Listar pagamentos                  | 200    |
| GET    | `/api/v1/pagamentos/{id}`                                   | Buscar por ID                      | 200    |
| GET    | `/api/v1/pagamentos/paciente/{pacienteId}`                  | Listar por paciente                | 200    |
| GET    | `/api/v1/pagamentos/assinatura/{assinaturaId}`              | Listar por assinatura              | 200    |
| GET    | `/api/v1/pagamentos/agendamento/{agendamentoId}`            | Listar por agendamento             | 200    |
| GET    | `/api/v1/pagamentos/periodo?inicio=...&fim=...`             | Listar por periodo                 | 200    |
| PUT    | `/api/v1/pagamentos/{id}`                                   | Atualizar pagamento                | 200    |
| PATCH  | `/api/v1/pagamentos/{id}/status`                            | Alterar status do pagamento        | 200    |
| PATCH  | `/api/v1/pagamentos/{id}/parcelas/{parcelaId}/status`       | Alterar status da parcela          | 200    |
| DELETE | `/api/v1/pagamentos/{id}`                                   | Desativar (soft delete)            | 204    |

**Criar pagamento (parcelado em 3x vinculado a assinatura):**
```json
POST /api/v1/pagamentos
{
  "pacienteId": "uuid-do-paciente",
  "assinaturaId": "uuid-da-assinatura",
  "valor": 300.00,
  "formaPagamento": "CARTAO_CREDITO",
  "numeroParcelas": 3,
  "dataVencimento": "2025-03-15",
  "observacoes": "Pagamento mensal"
}
```

**Criar pagamento avulso (1x via PIX vinculado a agendamento):**
```json
POST /api/v1/pagamentos
{
  "pacienteId": "uuid-do-paciente",
  "agendamentoId": "uuid-do-agendamento",
  "valor": 150.00,
  "formaPagamento": "PIX",
  "numeroParcelas": 1,
  "dataVencimento": "2025-03-15"
}
```

**Pagar parcela:**
```json
PATCH /api/v1/pagamentos/{id}/parcelas/{parcelaId}/status
{
  "status": "PAGO",
  "dataPagamento": "2025-03-15T14:30:00"
}
```

**Formas de pagamento:** `PIX`, `CARTAO_CREDITO`, `CARTAO_DEBITO`, `DINHEIRO`

**Status do pagamento:** `PENDENTE` -> `PARCIALMENTE_PAGO` -> `PAGO` -> `REEMBOLSADO` | `CANCELADO`

**Regras de negocio:**
- Ao menos uma assinatura ou agendamento deve ser informado
- Parcelas sao geradas automaticamente (valor dividido, datas mensais)
- Ultima parcela absorve diferenca de arredondamento
- Ao pagar parcela: se todas PAGO -> Pagamento = PAGO; se alguma PAGO -> PARCIALMENTE_PAGO
- Transicoes validas: PENDENTE->PAGO/CANCELADO, PARCIALMENTE_PAGO->PAGO/CANCELADO, PAGO->REEMBOLSADO

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

| Campo            | Tipo     | Descricao                          |
|------------------|----------|------------------------------------|
| id               | UUID     | Chave primaria                     |
| nome             | String   | Nome da atividade (unico)          |
| descricao        | String   | Descricao da atividade             |
| duracaoPadrao    | Integer  | Duracao padrao em minutos          |
| capacidadeMaxima | Integer  | Capacidade maxima de atendimentos  |
| ativo            | boolean  | Soft delete flag                   |
| createdAt        | DateTime | Data de criacao                    |
| updatedAt        | DateTime | Data de atualizacao                |

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

### Profissional (profissionais da clinica)
Representa os profissionais que atendem na clinica.

| Campo            | Tipo           | Descricao                              |
|------------------|----------------|----------------------------------------|
| id               | UUID           | Chave primaria                         |
| nome             | String         | Nome do profissional                   |
| telefone         | String         | Telefone                               |
| user             | User           | FK OneToOne (vinculo com login)        |
| atividades       | Set<Atividade> | ManyToMany (atividades que atende)     |
| googleCalendarId | String         | ID do Google Calendar (opcional)       |
| ativo            | boolean        | Soft delete flag                       |
| createdAt        | DateTime       | Data de criacao                        |
| updatedAt        | DateTime       | Data de atualizacao                    |

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

### HorarioDisponivel (disponibilidade dos profissionais)
Define os horarios em que um profissional esta disponivel para atendimento.

| Campo        | Tipo          | Descricao                       |
|--------------|---------------|---------------------------------|
| id           | UUID          | Chave primaria                  |
| profissional | Profissional  | FK ManyToOne                    |
| diaSemana    | DayOfWeek     | Dia da semana (MONDAY..FRIDAY)  |
| horaInicio   | LocalTime     | Hora de inicio                  |
| horaFim      | LocalTime     | Hora de fim                     |
| ativo        | boolean       | Soft delete flag                |
| createdAt    | DateTime      | Data de criacao                 |
| updatedAt    | DateTime      | Data de atualizacao             |

### Agendamento (agendamentos de sessoes)
Registra uma sessao agendada vinculando paciente, profissional e servico.

| Campo                  | Tipo              | Descricao                                     |
|------------------------|-------------------|-----------------------------------------------|
| id                     | UUID              | Chave primaria                                |
| paciente               | Patient           | FK ManyToOne                                  |
| profissional           | Profissional      | FK ManyToOne                                  |
| servico                | Servico           | FK ManyToOne                                  |
| assinatura             | Assinatura        | FK ManyToOne (opcional)                       |
| dataHora               | LocalDateTime     | Data e hora do agendamento                    |
| duracaoMinutos         | Integer           | Duracao em minutos                            |
| status                 | StatusAgendamento | AGENDADO, CONFIRMADO, REALIZADO, etc.         |
| observacoes            | String            | Anotacoes livres                              |
| googleCalendarEventId  | String            | ID do evento no Google Calendar               |
| ativo                  | boolean           | Soft delete flag                              |
| createdAt              | DateTime          | Data de criacao                               |
| updatedAt              | DateTime          | Data de atualizacao                           |

### Pagamento (pagamentos de assinaturas e sessoes)
Registra pagamentos vinculados a assinaturas e/ou agendamentos avulsos, com suporte a parcelamento.

| Campo          | Tipo            | Descricao                                       |
|----------------|-----------------|-------------------------------------------------|
| id             | UUID            | Chave primaria                                  |
| paciente       | Patient         | FK ManyToOne                                    |
| assinatura     | Assinatura      | FK ManyToOne (opcional - pagamento de plano)     |
| agendamento    | Agendamento     | FK ManyToOne (opcional - pagamento avulso)        |
| valor          | BigDecimal      | Valor total (precision=10, scale=2)             |
| formaPagamento | FormaPagamento  | PIX, CARTAO_CREDITO, CARTAO_DEBITO, DINHEIRO   |
| status         | StatusPagamento | PENDENTE, PARCIALMENTE_PAGO, PAGO, etc.        |
| numeroParcelas | Integer         | Numero de parcelas (default 1)                  |
| dataPagamento  | LocalDateTime   | Data em que foi totalmente pago                 |
| dataVencimento | LocalDate       | Vencimento da 1a parcela ou pagamento unico     |
| observacoes    | String          | Anotacoes livres                                |
| gatewayId      | String          | Reservado para integracao futura com gateway    |
| gatewayStatus  | String          | Reservado para integracao futura com gateway    |
| parcelas       | List<Parcela>   | OneToMany (cascade ALL, orphanRemoval)          |
| ativo          | boolean         | Soft delete flag                                |
| createdAt      | DateTime        | Data de criacao                                 |
| updatedAt      | DateTime        | Data de atualizacao                             |

### Parcela (parcelas de pagamentos)
Representa cada parcela de um pagamento parcelado.

| Campo          | Tipo          | Descricao                              |
|----------------|---------------|----------------------------------------|
| id             | UUID          | Chave primaria                         |
| pagamento      | Pagamento     | FK ManyToOne                           |
| numero         | Integer       | Numero da parcela (1, 2, 3...)         |
| valor          | BigDecimal    | Valor da parcela (precision=10, scale=2)|
| dataVencimento | LocalDate     | Data de vencimento da parcela          |
| dataPagamento  | LocalDateTime | Data em que foi efetivamente paga      |
| status         | StatusParcela | PENDENTE, PAGO, ATRASADO, CANCELADO   |
| ativo          | boolean       | Soft delete flag                       |
| createdAt      | DateTime      | Data de criacao                        |
| updatedAt      | DateTime      | Data de atualizacao                    |

## Testes

257 testes organizados em categorias:

| Categoria                    | Arquivo                           | Testes | Tipo        |
|------------------------------|-----------------------------------|--------|-------------|
| Service - Pacientes          | PatientServiceTest.java           | 9      | Unitario    |
| Service - Auth               | AuthServiceTest.java              | 4      | Unitario    |
| Service - Atividades         | AtividadeServiceTest.java         | 7      | Unitario    |
| Service - Planos             | PlanoServiceTest.java             | 7      | Unitario    |
| Service - Servicos           | ServicoServiceTest.java           | 10     | Unitario    |
| Service - Profissionais      | ProfissionalServiceTest.java      | 10     | Unitario    |
| Service - Assinaturas        | AssinaturaServiceTest.java        | 15     | Unitario    |
| Service - Horarios           | HorarioDisponivelServiceTest.java | 10     | Unitario    |
| Service - Agendamentos       | AgendamentoServiceTest.java       | 35     | Unitario    |
| Service - Google Calendar    | GoogleCalendarServiceTest.java    | 6      | Unitario    |
| Service - Pagamentos         | PagamentoServiceTest.java         | 29     | Unitario    |
| Security - JWT               | JwtServiceTest.java               | 5      | Unitario    |
| Controller - Pacientes       | PatientControllerTest.java        | 8      | WebMvcTest  |
| Controller - Auth            | AuthControllerTest.java           | 5      | WebMvcTest  |
| Controller - Atividades      | AtividadeControllerTest.java      | 7      | WebMvcTest  |
| Controller - Planos          | PlanoControllerTest.java          | 7      | WebMvcTest  |
| Controller - Servicos        | ServicoControllerTest.java        | 8      | WebMvcTest  |
| Controller - Profissionais   | ProfissionalControllerTest.java   | 7      | WebMvcTest  |
| Controller - Assinaturas     | AssinaturaControllerTest.java     | 10     | WebMvcTest  |
| Controller - Horarios        | HorarioDisponivelControllerTest.java| 7    | WebMvcTest  |
| Controller - Agendamentos    | AgendamentoControllerTest.java    | 10     | WebMvcTest  |
| Controller - Pagamentos      | PagamentoControllerTest.java      | 13     | WebMvcTest  |
| Integracao - Auth            | AuthIntegrationTest.java          | 3      | SpringBoot  |
| Integracao - Pacientes       | PatientIntegrationTest.java       | 3      | SpringBoot  |
| Integracao - Atividades      | AtividadeIntegrationTest.java     | 3      | SpringBoot  |
| Integracao - Servicos        | ServicoIntegrationTest.java       | 3      | SpringBoot  |
| Integracao - Profissionais   | ProfissionalIntegrationTest.java  | 3      | SpringBoot  |
| Integracao - Assinaturas     | AssinaturaIntegrationTest.java    | 3      | SpringBoot  |
| Integracao - Horarios        | HorarioDisponivelIntegrationTest.java| 3   | SpringBoot  |
| Integracao - Agendamentos    | AgendamentoIntegrationTest.java   | 3      | SpringBoot  |
| Contexto                     | PatientServiceApplicationTests    | 1      | SpringBoot  |

## Configuracao

### application.properties
- `jwt.secret` — Chave Base64 para assinatura JWT (minimo 256 bits)
- `jwt.expiration` — Tempo de expiracao do token em ms (padrao: 86400000 = 24h)
- H2 in-memory database em `jdbc:h2:mem:testdb`

### application-test.properties
- Chave JWT separada para testes
- SQL logging desabilitado
- `ddl-auto=create-drop`
