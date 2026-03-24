# CLAUDE.md — Sistema Humaniza / Patient Service

## Visão Geral

Monorepo com backend Spring Boot + frontend React (Vite/TypeScript) para gestão de pacientes e agendamentos da Clínica Humaniza.

```
patient-service/
├── backend/       # Spring Boot 3.5.5, Java 17
├── frontend/      # React + Vite + TypeScript
├── docker-compose.yml
├── .env           # NÃO commitado — contém credenciais reais
├── .env.example   # Template das variáveis necessárias
└── seed-postgres.sql  # Seed único para Supabase (executar 1x no SQL Editor)
```

## Como Rodar (Desenvolvimento Local)

### Backend
```bash
cd backend
export $(grep -v '^#' ../.env | xargs) && SPRING_PROFILES_ACTIVE=prod ./mvnw spring-boot:run
```

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

- **Dev/Testes**: H2 em memória (padrão, sem configuração extra)
- **Produção**: PostgreSQL via Supabase
  - Project ID: `rzsqyjeqrizzyazkcezc`
  - Organização: `humaniza`
  - Conexão JDBC: `jdbc:postgresql://db.rzsqyjeqrizzyazkcezc.supabase.co:5432/postgres`

### Seed inicial (Supabase — executar 1x)
Abrir o SQL Editor em:
`https://supabase.com/dashboard/project/rzsqyjeqrizzyazkcezc/sql/new`

Colar e executar o conteúdo de `seed-postgres.sql` para popular atividades, planos e serviços.

## Usuário Admin

- **Email**: `caissa@humaniza.com`
- **Role**: `ROLE_ADMIN`
- Criado automaticamente pelo `DataInitializer` ao subir com o perfil `prod`
- Senha configurada via `ADMIN1_PASSWORD` no `.env`

## Variáveis de Ambiente (.env)

```env
DATABASE_URL=jdbc:postgresql://db.rzsqyjeqrizzyazkcezc.supabase.co:5432/postgres
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=<senha do Supabase>
JWT_SECRET=<secret JWT gerado>
ADMIN1_EMAIL=caissa@humaniza.com
ADMIN1_NOME=Caissa Humaniza
ADMIN1_PASSWORD=<senha do sistema>
```

## Segurança

- Registro público **fechado** — `/api/auth/registrar` requer `ROLE_ADMIN`
- Sem OAuth / login social
- Máximo 2-3 usuários internos
- Novos usuários só podem ser criados por um admin autenticado

## Perfis Spring

| Perfil | Banco | Uso |
|--------|-------|-----|
| (default) | H2 em memória | Desenvolvimento local |
| `test` | H2 em memória | Testes automatizados |
| `prod` | PostgreSQL/Supabase | Produção |

## Testes

```bash
cd backend
./mvnw test
```

Todos os 286 testes devem passar. Os testes usam o perfil `test` com H2 e um admin seed (`admin@test.com` / `senha123`).

## Docker (Produção)

> **Nota**: O Docker Compose pode ter problemas de rede no macOS para alcançar o Supabase.
> Prefira rodar backend diretamente com Maven para desenvolvimento.

```bash
docker compose up --build
```

## GitHub

- Repositório: `ieplif/patient-service`
- Branch principal: `main`
