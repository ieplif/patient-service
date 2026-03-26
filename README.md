# Sistema Humaniza — Patient Service

Sistema de gestao para a Clinica Humaniza RJ. Gerencia pacientes, agendamentos, pagamentos, assinaturas de servicos e prontuarios.

## Stack

- **Backend**: Spring Boot 3.5.5 / Java 17
- **Frontend**: React + Vite + TypeScript + Tailwind CSS + shadcn/ui
- **Banco de Dados**: PostgreSQL (Supabase)
- **Storage**: Supabase Storage (upload de prontuarios)
- **Autenticacao**: JWT

## Funcionalidades

- Dashboard com resumo geral da clinica
- CRUD de pacientes com pagina de resumo individual
- Agendamentos com regras de negocio Pilates (reposicao, cancelamento 3h, feriados)
- Controle de pagamentos com parcelas
- Assinaturas de servicos com acompanhamento de sessoes
- Cadastro de profissionais e atividades
- Upload de prontuarios via Supabase Storage

## Requisitos

- Java 17+
- Node.js 18+
- Conta Supabase (PostgreSQL + Storage)

## Configuracao

1. Copie `.env.example` para `.env` e preencha as variaveis:
```bash
cp .env.example .env
```

2. No Supabase, crie o bucket `prontuarios` em Storage (privado).

3. Execute o seed inicial no SQL Editor do Supabase:
```
https://supabase.com/dashboard/project/SEU_PROJECT_REF/sql/new
```
Cole o conteudo de `seed-postgres.sql`.

## Rodando

### Backend
```bash
cd backend
export $(grep -v '^#' ../.env | xargs) && SPRING_PROFILES_ACTIVE=prod ./mvnw spring-boot:run
```

### Frontend
```bash
cd frontend
npm install
npm run dev
```

Acesse **http://localhost:5173**

## Testes

```bash
cd backend
./mvnw test
```

## Licenca

Projeto privado — Clinica Humaniza RJ.
