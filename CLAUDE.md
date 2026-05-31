# CLAUDE.md — Sistema Humaniza / Patient Service

> **Importante**: este arquivo e o contexto do projeto para o Claude.
> Mantenha sincronizado quando features novas forem adicionadas.
> Regras de negocio detalhadas: ver [`docs/REGRAS-DE-NEGOCIO.md`](docs/REGRAS-DE-NEGOCIO.md).

## Visao Geral

Monorepo com backend Spring Boot + frontend React (Vite/TypeScript) para gestao
de pacientes, agendamentos, pagamentos, assinaturas e prontuarios da Clinica
Humaniza (Pilates + Fisioterapia).

```
patient-service/
├── backend/                              # Spring Boot 3.5.5, Java 17
├── frontend/                             # React + Vite + TS + Tailwind + shadcn
├── deploy/                               # Scripts AWS Lightsail (setup + update)
├── docs/                                 # Documentacao adicional
├── docker-compose.yml + .prod.yml
├── .env                                  # NAO commitado
├── .env.example                          # Template
├── seed-postgres.sql                     # Seed unico (1x no Supabase)
├── migration-campos-opcionais.sql        # Email/CPF/Nascimento opcionais
├── migration-prontuarios-text-columns.sql# storage_url/path TEXT + ativo default
└── migration-assinatura-suspender.sql    # SUSPENSO + campos suspensao
```

## Como Rodar (Desenvolvimento Local)

### Backend
```bash
cd backend
export $(grep -v '^#' ../.env | xargs) && SPRING_PROFILES_ACTIVE=prod ./mvnw spring-boot:run
```

Para forcar recompilacao completa: `clean` antes de `spring-boot:run`.

### Frontend
```bash
cd frontend
npm run dev
```

Acesse: **http://localhost:5173**

## Banco de Dados

- **Dev/Testes**: H2 em memoria
- **Producao**: PostgreSQL via Supabase
  - Project ID: `rzsqyjeqrizzyazkcezc`
  - Conexao JDBC: `jdbc:postgresql://db.rzsqyjeqrizzyazkcezc.supabase.co:5432/postgres`

### Seed inicial (Supabase — executar 1x)

`https://supabase.com/dashboard/project/rzsqyjeqrizzyazkcezc/sql/new`

Cole o conteudo de `seed-postgres.sql`.

### Migrations pendentes (rodar antes de cada deploy correspondente)

Hibernate `ddl-auto=update` **adiciona** colunas/tabelas mas **nao**
- Remove constraint NOT NULL
- Altera tipo de coluna (VARCHAR -> TEXT)
- Atualiza valores aceitos por CHECK constraint

Por isso temos scripts manuais idempotentes (podem ser rodados varias vezes).
Veja [docs/REGRAS-DE-NEGOCIO.md#migrations](docs/REGRAS-DE-NEGOCIO.md#migrations)
para a lista completa.

## Supabase Storage (Prontuarios)

- **Bucket**: `prontuarios` (privado) — criar manualmente no painel
- **Integracao**: `SupabaseStorageService` usa REST API com service_role key
- **Limite upload**: 10MB por arquivo
- **Tipos aceitos**: PDF, JPEG, PNG, DOC, DOCX
- **Tipos de documento**: PRONTUARIO, TERMO, NOTA_FISCAL
- **Sanitizacao do nome**: acentos/espacos/caracteres especiais sao normalizados
- **Rollback automatico**: se o save no banco falhar apos o upload, o arquivo
  e removido do Storage (`ProntuarioService.upload`)

## Usuario Admin

- **Email**: `caissa@humaniza.com`
- **Role**: `ROLE_ADMIN`
- Criado pelo `DataInitializer` ao subir com perfil `prod`
- Senha em `ADMIN1_PASSWORD` no `.env`

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
| `/dashboard` | Dashboard | Cards de pacientes, agendamentos do dia, pagamentos pendentes, assinaturas ativas, receita do mes (por data de vencimento). Lista proximos agendamentos (status=AGENDADO) e pagamentos pendentes. |
| `/pacientes` | Pacientes | CRUD. Apenas nome + telefone obrigatorios. Email/CPF/nascimento editaveis depois. |
| `/pacientes/:id` | Resumo do Paciente | Dados pessoais, assinaturas (badge "Ativo"/"Inativo" derivado), agendamentos, prontuarios (upload/download). |
| `/agendamentos` | Agendamentos | Filtro padrao "Agendado". Linha azul-claro para Fisioterapia. Coluna Profissional oculta. Datas retroativas + atalho AGENDADO -> REALIZADO. |
| `/pagamentos` | Pagamentos | Editar (forma/vencimento/observacoes), marcar como pago com data efetiva, cancelar, reembolsar. Ordenado por dataVencimento desc. |
| `/assinaturas` | Assinaturas | Suspender (gravidez/lesao), Reativar, Editar (regenera horarios). Badge ambar Suspensa com tooltip do motivo. |
| `/profissionais` | Profissionais | CRUD + horarios disponiveis por dia da semana. |

## Regras de Negocio

**Ver [`docs/REGRAS-DE-NEGOCIO.md`](docs/REGRAS-DE-NEGOCIO.md) para descricao
completa.** Resumo do que mudou desde a versao inicial:

- Profissional **opcional** em agendamentos/assinaturas (Pilates roda sem
  profissional fixo — quem ministra varia)
- Cancelamento Pilates **sem regra de 3h** — checkbox "Gerar direito a
  reposicao" decide caso a caso
- AGENDADO -> REALIZADO direto (atalho para retroativos)
- Datas retroativas aceitas em agendamentos e pagamentos (popula historico)
- Cadastro de paciente simplificado (so nome + telefone obrigatorios)
- Email/CPF UNIQUE no banco mas opcional — multiplos NULLs convivem
- Telefone NAO unico (mae + filha podem compartilhar)
- Assinatura ganha estado **SUSPENSO** (gravidez/lesao/viagem)
- Renovacao automatica de assinaturas mensais (scheduler 06h diario)
- Geracao automatica de cobrancas (pagamentos PENDENTES) das assinaturas de
  Pilates recorrentes (mensal/trimestral/semestral) na virada do mes (scheduler
  dia 1 as 05h, 1h antes da renovacao), seguindo o ciclo do plano; valor = preco
  vigente do servico; idempotente por (assinatura, vencimento)

## Seguranca

- Registro publico fechado — `/api/auth/registrar` requer `ROLE_ADMIN`
- Sem OAuth / login social
- 2-3 usuarios internos previstos
- JWT em **cookie httpOnly** (24h)
- Roles: `ROLE_ADMIN`, `ROLE_PROFISSIONAL`

## Perfis Spring

| Perfil | Banco | Uso |
|--------|-------|-----|
| (default) | H2 em memoria | Desenvolvimento local |
| `test` | H2 em memoria | Testes automatizados (admin seed `admin@test.com`/`senha123`) |
| `prod` | PostgreSQL/Supabase | Producao |

## Testes

```bash
cd backend
./mvnw test                # 285+ testes, ~30s
```

Status atual: **285 testes passando, 0 falhas**. Frontend ainda sem testes
automatizados (planejamento futuro).

## Deploy AWS Lightsail

Instancia: Ubuntu 22.04 em `52.203.244.168` (us-east-1a, 2 GB RAM / 2 vCPU /
60 GB SSD). Setup inicial em `deploy/setup-lightsail.sh`.

### Atualizar producao

1. **Rodar migrations pendentes** no Supabase SQL Editor (caso ainda nao tenha
   rodado nesse deploy)
2. SSH + script de update:

```bash
ssh ubuntu@52.203.244.168
cd /opt/humaniza/patient-service
./deploy/update.sh
```

O script faz `git pull origin main` + `docker compose up -d --build` +
healthcheck em `http://localhost:8080/api/health`. **Reboot da VM nao e
necessario**.

## Docker

```bash
docker compose up --build       # dev
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build   # prod
```

Nota: o Docker Compose pode ter problemas de rede no macOS para alcancar o
Supabase. Em dev, prefira rodar o backend com Maven direto.

## GitHub

- Repositorio: `ieplif/patient-service`
- Branch principal: `main`
- Sem CI/CD automatizado — deploys sao manuais via SSH

## Stack Tecnica

- **Backend**: Spring Boot 3.5.5, Java 17, Spring Security, JWT (jjwt 0.12.6),
  JPA/Hibernate, Lombok, Mockito + JUnit 5
- **Frontend**: React 18, Vite 7, TypeScript, Tailwind CSS, shadcn/ui,
  TanStack React Query, date-fns, axios
- **Banco**: PostgreSQL 15 (Supabase) / H2 (dev/test)
- **Storage**: Supabase Storage (prontuarios)
- **Infra**: AWS Lightsail (Ubuntu + Docker), Nginx (TLS via Certbot)
- **Dominio**: `app.humanizarj.com.br`

## Historico de evolucao (ordem cronologica)

Para entender o **por que** de cada decisao, ver os commits no GitHub. Resumo
das features grandes:

1. **Base**: CRUD de pacientes/profissionais/servicos/agendamentos/pagamentos
2. **Assinaturas + recorrentes**: agendamentos recorrentes vinculados,
   progresso de sessoes
3. **Prontuarios**: upload no Supabase Storage com 3 tipos de documento
4. **Renovacao automatica**: scheduler diario as 06h para mensalidades
5. **Horarios disponiveis**: cadastro por profissional + dia da semana
6. **Deploy AWS**: docker-compose.prod.yml + nginx-ssl.conf + scripts
7. **Profissional opcional**: Pilates sem profissional fixo
8. **Cadastro simplificado**: nome + telefone obrigatorios
9. **Filtro `assinaturaId`**: bugfix em GET /api/v1/agendamentos
10. **Regenerar horarios**: editar dias fixos da assinatura
11. **Encurtar nomes**: primeiro + ultimo em listas
12. **Coluna Profissional oculta** em Agendamentos + Dashboard
13. **Datas retroativas**: agendamentos e pagamentos no passado, AGENDADO ->
    REALIZADO direto, sem regra de 3h
14. **Destaque sky para Fisioterapia** na tabela
15. **Upload de arquivos**: sanitizacao + mensagens de erro reais + colunas
    TEXT + campo ativo + rollback do Storage
16. **Filtro padrao "Agendado"** + pagamentos ordenados por vencimento
17. **Suspender/Reativar assinatura** (SUSPENSO + campos de suspensao)
18. **Editar pagamento**: PUT exposto na UI
19. **Status do paciente derivado** das assinaturas
