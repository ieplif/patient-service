# CLAUDE.md — Sistema Humaniza / Patient Service

## Visao Geral

Monorepo com backend Spring Boot + frontend React (Vite/TypeScript) para gestao de pacientes, agendamentos, pagamentos, assinaturas e prontuarios da Clinica Humaniza.

```
patient-service/
├── backend/       # Spring Boot 3.5.5, Java 17
├── frontend/      # React + Vite + TypeScript + Tailwind + shadcn/ui
├── docker-compose.yml
├── .env           # NAO commitado — contem credenciais reais
├── .env.example   # Template das variaveis necessarias
└── seed-postgres.sql  # Seed unico para Supabase (executar 1x no SQL Editor)
```

## Como Rodar (Desenvolvimento Local)

### Backend
```bash
cd backend
export $(grep -v '^#' ../.env | xargs) && SPRING_PROFILES_ACTIVE=prod ./mvnw spring-boot:run
```
> Para forcar recompilacao completa: adicionar `clean` antes de `spring-boot:run`

### Frontend
```bash
cd frontend
npm run dev
```

Acesse: **http://localhost:5173**

### Parar Docker (se estiver rodando)
```bash
docker compose down
```

## Banco de Dados

- **Dev/Testes**: H2 em memoria (padrao, sem configuracao extra)
- **Producao**: PostgreSQL via Supabase
  - Project ID: `rzsqyjeqrizzyazkcezc`
  - Organizacao: `humaniza`
  - Conexao JDBC: `jdbc:postgresql://db.rzsqyjeqrizzyazkcezc.supabase.co:5432/postgres`

### Seed inicial (Supabase — executar 1x)
Abrir o SQL Editor em:
`https://supabase.com/dashboard/project/rzsqyjeqrizzyazkcezc/sql/new`

Colar e executar o conteudo de `seed-postgres.sql` para popular atividades, planos, servicos, profissionais e pacientes de teste.

## Supabase Storage (Prontuarios)

- **Bucket**: `prontuarios` (privado) — criar manualmente no painel Supabase Storage
- **Integracao**: `SupabaseStorageService` usa a REST API do Supabase com service_role key
- **Limite upload**: 10MB por arquivo
- **Variaveis necessarias**: `SUPABASE_URL`, `SUPABASE_SERVICE_KEY`, `SUPABASE_STORAGE_BUCKET`

## Usuario Admin

- **Email**: `caissa@humaniza.com`
- **Role**: `ROLE_ADMIN`
- Criado automaticamente pelo `DataInitializer` ao subir com o perfil `prod`
- Senha configurada via `ADMIN1_PASSWORD` no `.env`

## Variaveis de Ambiente (.env)

```env
DATABASE_URL=jdbc:postgresql://db.rzsqyjeqrizzyazkcezc.supabase.co:5432/postgres
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=<senha do Supabase>
JWT_SECRET=<secret JWT gerado>
SUPABASE_URL=https://rzsqyjeqrizzyazkcezc.supabase.co
SUPABASE_SERVICE_KEY=<service role key do Supabase>
SUPABASE_STORAGE_BUCKET=prontuarios
ADMIN1_EMAIL=caissa@humaniza.com
ADMIN1_NOME=Caissa Humaniza
ADMIN1_PASSWORD=<senha do sistema>
```

## Telas do Sistema

| Rota | Pagina | Descricao |
|------|--------|-----------|
| `/dashboard` | Dashboard | Resumo com cards de pacientes, agendamentos do dia, pagamentos pendentes, assinaturas ativas |
| `/pacientes` | Pacientes | CRUD completo — nome clicavel leva ao resumo |
| `/pacientes/:id` | Resumo do Paciente | Dados pessoais, assinaturas, agendamentos recentes, prontuarios (upload/download) |
| `/agendamentos` | Agendamentos | CRUD com regras Pilates (cancelamento 3h, reposicao, feriados) |
| `/pagamentos` | Pagamentos | CRUD com parcelas e acoes de status (pagar/cancelar/reembolsar) |
| `/assinaturas` | Assinaturas | CRUD com progresso de sessoes e acoes (cancelar/reativar) |
| `/profissionais` | Profissionais | CRUD simples com atividades vinculadas |

## Regras de Negocio — Pilates

- Cancelamento com >= 3h de antecedencia: gera direito a reposicao
- Cancelamento com < 3h: sessao perdida (sem reposicao)
- Limite: 2 reposicoes por mes por paciente
- Validade da reposicao: 20 dias corridos
- Feriados sao verificados via `FeriadoRepository` (inclui recorrentes)

## Seguranca

- Registro publico **fechado** — `/api/auth/registrar` requer `ROLE_ADMIN`
- Sem OAuth / login social
- Maximo 2-3 usuarios internos
- JWT com expiracao de 24h

## Perfis Spring

| Perfil | Banco | Uso |
|--------|-------|-----|
| (default) | H2 em memoria | Desenvolvimento local |
| `test` | H2 em memoria | Testes automatizados |
| `prod` | PostgreSQL/Supabase | Producao |

## Testes

```bash
cd backend
./mvnw test
```

Os testes usam o perfil `test` com H2 e um admin seed (`admin@test.com` / `senha123`).

## Docker

> **Nota**: O Docker Compose pode ter problemas de rede no macOS para alcancar o Supabase.
> Prefira rodar backend diretamente com Maven para desenvolvimento.

```bash
docker compose up --build
```

## GitHub

- Repositorio: `ieplif/patient-service`
- Branch principal: `main`

## Stack Tecnica

- **Backend**: Spring Boot 3.5.5, Java 17, Spring Security, JWT (jjwt 0.12.6), JPA/Hibernate
- **Frontend**: React 18, Vite, TypeScript, Tailwind CSS, shadcn/ui, TanStack React Query
- **Banco**: PostgreSQL (Supabase) / H2 (dev/test)
- **Storage**: Supabase Storage (prontuarios)
- **Deploy**: Docker Compose (opcional)
