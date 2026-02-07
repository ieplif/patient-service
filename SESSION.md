# Historico de Sessoes - Patient Service

## Sessao 1 — Setup Inicial do Projeto
**Data:** 14/09/2025

- Criacao do projeto Spring Boot 3.5.5 com Maven
- Configuracao de dependencias: Spring Web, Spring Data JPA, Validation, Lombok, H2, DevTools
- Criacao da entidade `Patient` com campos: nomeCompleto, email, cpf, dataNascimento, telefone, endereco, profissao, estadoCivil, statusAtivo, consentimentoLgpd
- Implementacao de soft delete com `@SQLRestriction("status_ativo = true")`
- Campos de auditoria: createdAt (`@PrePersist`) e updatedAt (`@PreUpdate`)
- UUID como chave primaria
- Criacao dos DTOs: `PatientRequestDTO` (com validacoes), `PatientResponseDTO` (sem CPF por seguranca), `PatientUpdateDTO` (campos opcionais)
- Criacao do `PatientMapper` para conversao Entity <-> DTO
- Criacao do `PatientRepository` com queries: findByCpf, findByEmail, findAllIncludingInactive, findInactiveById

**Commits:**
- `64ca142` — first commit
- `6922afc` — DTO
- `2e168db` — PatientService ajuste

---

## Sessao 2 — CRUD de Pacientes
**Data:** 14/09/2025 - 21/09/2025

- Implementacao do `PatientService` com logica de negocio:
  - Criacao com validacao de CPF e e-mail duplicados
  - Busca por ID com tratamento de null e not found
  - Listagem de todos os pacientes ativos
  - Atualizacao parcial (campos nao nulos do DTO)
  - Soft delete (seta statusAtivo = false)
- Implementacao do `PatientController` com endpoints REST:
  - `POST /api/v1/patients` (201)
  - `GET /api/v1/patients` (200)
  - `GET /api/v1/patients/{id}` (200)
  - `PUT /api/v1/patients/{id}` (200)
  - `DELETE /api/v1/patients/{id}` (204)
- Criacao de excecoes customizadas: `PatientNotFoundException`, `DuplicateResourceException`
- Criacao do `GlobalExceptionHandler` com handlers para:
  - `MethodArgumentNotValidException` (400)
  - `PatientNotFoundException` (404)
  - `DuplicateResourceException` (409)

**Commits:**
- `9580ce4` — PUT
- `132f357` — Metodo Delete
- `d9981de` — Adicionando metodo em Repository

---

## Sessao 3 — Correcoes
**Data:** 06/02/2026

- Correcao de erro no tratamento de ID em endpoints

**Commits:**
- `ea237c7` — DTO
- `1c5c78f` — erro id

---

## Sessao 4 — Autenticacao JWT, Swagger e Testes
**Data:** 06/02/2026

### Fase 1: Dependencias e Configuracao
- Adicionadas dependencias ao `pom.xml`:
  - `spring-boot-starter-security`
  - `jjwt-api`, `jjwt-impl`, `jjwt-jackson` (v0.12.6)
  - `springdoc-openapi-starter-webmvc-ui` (v2.8.4)
  - `spring-security-test` (scope test)
- Atualizado `application.properties` com jwt.secret, jwt.expiration e springdoc config
- Criado `application-test.properties` para perfil de teste

### Fase 2: Model e Repository de Usuario
- Criado enum `Role` (ROLE_USER, ROLE_ADMIN)
- Criada entidade `User` (UUID, nome, email, senha BCrypt, role)
- Criado `UserRepository` com findByEmail() e existsByEmail()

### Fase 3: Seguranca (JWT + Spring Security)
- `JwtService` — Geracao e validacao de tokens JWT usando jjwt 0.12.6
  - generateToken(), extractEmail(), isTokenValid()
  - Chave HMAC-SHA derivada de Base64
- `UserDetailsServiceImpl` — Carrega User do banco e converte para Spring Security UserDetails
- `JwtAuthenticationFilter` — OncePerRequestFilter que:
  - Extrai JWT do header Authorization
  - Valida o token
  - Seta o SecurityContext
  - Try-catch para tokens invalidos (continua sem auth)
- `SecurityConfig` — SecurityFilterChain com:
  - CSRF desabilitado (API stateless)
  - Frame options desabilitado (H2 Console)
  - Rotas publicas: /api/auth/**, /h2-console/**, /swagger-ui/**, /v3/api-docs/**
  - HttpStatusEntryPoint(UNAUTHORIZED) para retornar 401 em vez de 403
  - Session STATELESS
  - BCryptPasswordEncoder bean
  - AuthenticationManager bean

### Fase 4: Auth (DTOs, Service, Controller)
- DTOs: `RegisterRequestDTO`, `LoginRequestDTO`, `AuthResponseDTO`
- `AuthService`:
  - registrar() — valida email duplicado, salva com BCrypt, retorna JWT
  - login() — autentica via AuthenticationManager, retorna JWT
- `AuthController`:
  - POST /api/auth/registrar (201)
  - POST /api/auth/login (200)

### Fase 5: Exception Handler
- Adicionado handler para `BadCredentialsException` (401 - "E-mail ou senha incorretos")
- Adicionado handler para `AccessDeniedException` (403 - "Acesso negado")

### Fase 6: Swagger/OpenAPI
- Criado `OpenApiConfig` com SecurityScheme JWT Bearer e info da API
- Adicionadas anotacoes `@Tag`, `@Operation`, `@ApiResponse` no PatientController
- Swagger UI acessivel em /swagger-ui.html com botao "Authorize"

### Fase 7: Testes Unitarios (22 testes)
- `PatientServiceTest` (9 testes) — Mockito + AssertJ
  - Criar paciente, CPF duplicado, email duplicado
  - Buscar por ID, nao encontrado, ID nulo
  - Listar todos, atualizar, deletar
- `AuthServiceTest` (4 testes)
  - Registrar com sucesso, email duplicado
  - Login com sucesso, credenciais invalidas
- `JwtServiceTest` (5 testes)
  - Gerar token, extrair email, validar token, usuario incorreto, token expirado
- `PatientControllerTest` (8 testes) — @WebMvcTest + @Import(SecurityConfig)
  - CRUD autenticado com @WithMockUser (201, 200, 204)
  - 401 sem autenticacao, 404 nao encontrado, 400 dados invalidos
- `AuthControllerTest` (5 testes) — @WebMvcTest + @Import(SecurityConfig)
  - Registro 201, login 200, 400 invalido, 409 duplicado, 401 credenciais

### Fase 8: Testes de Integracao (6 testes)
- `AuthIntegrationTest` (3 testes) — @SpringBootTest
  - Fluxo completo: registrar -> login -> obter token
  - Email duplicado, senha incorreta
- `PatientIntegrationTest` (3 testes) — @SpringBootTest
  - Fluxo completo: registrar -> autenticar -> CRUD de pacientes
  - 401 sem token, 401 token invalido

### Fase 9: Correcao de teste existente
- Adicionado `@ActiveProfiles("test")` no PatientServiceApplicationTests

### Problemas Resolvidos
1. **@MockBean removido no Spring Boot 3.5.5** — Substituido por `@MockitoBean` de `org.springframework.test.context.bean.override.mockito`
2. **@WebMvcTest nao carrega SecurityConfig** — Resolvido com `@Import({SecurityConfig.class, JwtAuthenticationFilter.class})`
3. **CSRF bloqueando POST/PUT/DELETE em testes** — Adicionado `with(csrf())` nos testes @WebMvcTest
4. **Spring Security retornando 403 em vez de 401** — Adicionado `HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)`
5. **Token invalido causando MalformedJwtException** — Adicionado try-catch no JwtAuthenticationFilter
6. **VS Code nao reconhecendo @MockitoBean** — Limitacao da extensao Java do VS Code com Spring Boot 3.5.5; testes compilam e passam normalmente via Maven

**Resultado:** 38 testes, 0 falhas, BUILD SUCCESS

**Commits:**
- `1057ce9` — Adicionar autenticacao JWT, Swagger/OpenAPI e testes

---

## Sessao 5 — Modulos Atividade, Plano, Servico e Assinatura
**Data:** 06-07/02/2026

### Fase 1: Atividade, Plano e Servico (CRUD completo)
- Criacao das entidades `Atividade`, `Plano` e `Servico` com soft delete (`@SQLRestriction`)
- Servico usa `@ManyToOne` para Atividade e Plano com `@UniqueConstraint`
- DTOs (Request, Response, Update), Mappers e Services para cada modulo
- Controllers REST com Swagger annotations:
  - `GET/POST/PUT/DELETE /api/v1/atividades`
  - `GET/POST/PUT/DELETE /api/v1/planos`
  - `GET/POST/PUT/DELETE /api/v1/servicos` (+ filtros por atividade e plano)
- `ResourceNotFoundException` generica para reutilizar em todos os modulos
- Testes unitarios, controller e integracao para cada modulo

### Fase 2: Modulo de Assinaturas
- Criacao do enum `StatusAssinatura` (ATIVO, CANCELADO, VENCIDO, FINALIZADO)
- Criacao da entidade `Assinatura` com `@ManyToOne` para Patient e Servico
- DTOs: `AssinaturaRequestDTO`, `AssinaturaResponseDTO` (com `sessoesRestantes` calculado), `AssinaturaUpdateDTO`, `AssinaturaStatusDTO`
- `AssinaturaMapper` com `servicoDescricao = atividade.nome + " - " + plano.nome`
- `AssinaturaService` com logica de negocio:
  - Criacao com calculo automatico de `dataVencimento` (dataInicio + plano.validadeDias)
  - Listagem por paciente e por servico
  - Atualizacao parcial
  - Alteracao de status (bloqueia se FINALIZADO)
  - Registro de sessao (incrementa sessoesRealizadas, finaliza automaticamente ao atingir limite)
  - Soft delete
- `AssinaturaController` com 9 endpoints:
  - CRUD padrao + `PATCH /{id}/status` + `PATCH /{id}/registrar-sessao`
- `BusinessException` para regras de negocio (422 UNPROCESSABLE_ENTITY)
- Handler adicionado no `GlobalExceptionHandler`

### Fase 3: Testes
- `AssinaturaServiceTest` (15 testes unitarios) — criacao, validacoes, sessoes, finalizacao automatica, regras de negocio
- `AssinaturaControllerTest` (10 testes) — todos os endpoints + 401 sem auth
- `AssinaturaIntegrationTest` (3 testes) — fluxo completo (paciente → atividade → plano → servico → assinatura → sessoes → finalizacao), 401 sem token, 404 paciente inexistente
- Testes de Atividade, Plano e Servico (unitarios, controller, integracao)

**Resultado:** 116 testes, 0 falhas, BUILD SUCCESS

**Commits:**
- `23eba3c` — Adicionar modulos Atividade, Plano, Servico e Assinatura com CRUD completo e testes

---

## Resumo de Progresso

| Sessao | Data       | Foco                                    | Testes |
|--------|------------|-----------------------------------------|--------|
| 1      | 14/09/2025 | Setup, entidades, DTOs                  | 0      |
| 2      | 14-21/09   | CRUD completo, excecoes                 | 0      |
| 3      | 06/02/2026 | Correcoes                               | 0      |
| 4      | 06/02/2026 | JWT, Swagger, testes                    | 38     |
| 5      | 06-07/02   | Atividade, Plano, Servico, Assinatura   | 116    |
