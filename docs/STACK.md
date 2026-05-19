# Stack Tecnica — Patient Service (Clinica Humaniza)

Documento de referencia da stack para replicar este sistema em outro projeto.
Versoes confirmadas em 2026-05-18.

---

## Visao geral

Monorepo com backend Spring Boot + frontend React, banco PostgreSQL (Supabase),
deploy em AWS Lightsail via Docker.

```
monorepo/
├── backend/                          # Spring Boot — Maven
├── frontend/                         # React — Vite / npm
├── deploy/                           # scripts Lightsail + config Nginx
├── docs/                             # documentacao
├── docker-compose.yml
├── docker-compose.prod.yml
├── .env / .env.example
└── *.sql                             # seed + migrations manuais
```

---

## Backend — Spring Boot

| Item | Versao |
|------|--------|
| Java | 17 |
| Spring Boot | 3.5.5 (spring-boot-starter-parent) |
| Build | Maven (wrapper `mvnw` incluso) |
| Artifact | `patient-service` 0.0.1-SNAPSHOT |

### Dependencias

| Dependencia | Versao | Para que serve |
|-------------|--------|----------------|
| spring-boot-starter-web | 3.5.5 | API REST (Spring MVC) |
| spring-boot-starter-data-jpa | 3.5.5 | Persistencia (Hibernate/JPA) |
| spring-boot-starter-validation | 3.5.5 | Bean Validation (`@NotNull`, `@Email`, etc.) |
| spring-boot-starter-security | 3.5.5 | Autenticacao e autorizacao |
| spring-boot-devtools | 3.5.5 | Hot reload em desenvolvimento |
| spring-boot-starter-test | 3.5.5 | JUnit 5 + Mockito + AssertJ |
| spring-security-test | 3.5.5 | Helpers de teste de seguranca |
| postgresql (driver) | gerenciado | Banco de producao |
| h2 | gerenciado | Banco em memoria (dev/test) |
| lombok | gerenciado | Getters/setters/builders por anotacao |
| jjwt-api / jjwt-impl / jjwt-jackson | 0.12.6 | Geracao e validacao de JWT |
| springdoc-openapi-starter-webmvc-ui | 2.8.4 | Swagger UI (`/swagger-ui.html`) |
| bucket4j-core | 8.10.1 | Rate limiting |
| google-api-services-calendar | v3-rev20250404-2.0.0 | Integracao Google Calendar (opcional) |
| google-auth-library-oauth2-http | 1.40.0 | OAuth2 para Google Calendar |
| jacoco-maven-plugin | 0.8.12 | Relatorio de cobertura de testes |

### Arquitetura em camadas

```
Controller  -> recebe HTTP, valida DTO, devolve ResponseEntity
   |
Service     -> regra de negocio, @Transactional
   |
Repository  -> Spring Data JPA (interfaces)
   |
Entity      -> modelo JPA mapeado para tabela
```

- **DTOs separados** por operacao: `XxxRequestDTO`, `XxxResponseDTO`,
  `XxxUpdateDTO` — nunca expor a entidade direto na API
- **Mappers** dedicados (`XxxMapper`) convertem entre Entity e DTO
- **Specifications** (JPA Criteria) para filtros dinamicos de listagem
- **GlobalExceptionHandler** (`@RestControllerAdvice`) centraliza erros
- **Perfis Spring**: `default`/`test` usam H2; `prod` usa PostgreSQL

### Seguranca

- JWT assinado (HS256) entregue em **cookie httpOnly**
- Expiracao de 24h
- Roles: `ROLE_ADMIN`, `ROLE_PROFISSIONAL`
- Senhas com BCrypt
- Registro publico fechado (criar usuario exige ROLE_ADMIN)
- Rate limiting com bucket4j

### Comandos

```bash
cd backend
./mvnw spring-boot:run                 # rodar (perfil default = H2)
./mvnw test                            # testes + relatorio JaCoCo
./mvnw clean package                   # gerar JAR
# relatorio de cobertura: target/site/jacoco/index.html
```

---

## Frontend — React + Vite

| Item | Versao |
|------|--------|
| React | 19.2 |
| Build | Vite 7.3 |
| Linguagem | TypeScript 5.9 |
| Gerenciador | npm |

### Dependencias de runtime

| Lib | Versao | Para que serve |
|-----|--------|----------------|
| react / react-dom | 19.2 | Biblioteca de UI |
| react-router-dom | 7.13 | Roteamento SPA |
| @tanstack/react-query | 5.90 | Cache/sincronizacao de dados do servidor |
| axios | 1.13 | Cliente HTTP |
| zustand | 5.0 | Estado global do cliente (UI) |
| react-hook-form | 7.71 | Formularios |
| zod | 4.3 | Validacao de schema |
| @hookform/resolvers | 5.2 | Liga react-hook-form + zod |
| tailwindcss | 3.4 | CSS utilitario |
| tailwindcss-animate | 1.0 | Animacoes Tailwind |
| @radix-ui/react-* | varios | Primitivos acessiveis (dialog, select, dropdown, toast, label, avatar, slot) |
| class-variance-authority | 0.7 | Variantes de componente (padrao shadcn) |
| clsx + tailwind-merge | 2.1 / 3.5 | Composicao de classes CSS |
| lucide-react | 0.576 | Icones |
| date-fns | 4.1 | Manipulacao de datas |

### Dependencias de desenvolvimento

| Lib | Versao |
|-----|--------|
| vite | 7.3 |
| @vitejs/plugin-react | 5.1 |
| typescript | 5.9 |
| eslint | 9.39 |
| typescript-eslint | 8.48 |
| eslint-plugin-react-hooks | 7.0 |
| eslint-plugin-react-refresh | 0.4 |
| autoprefixer | 10.4 |
| postcss | 8.5 |
| @types/react, @types/react-dom, @types/node | 19.x / 24.x |

### shadcn/ui

NAO e uma dependencia npm. Sao componentes copiados para
`src/components/ui/`, construidos sobre **Radix UI + Tailwind + CVA**.
Adicionar novos componentes via CLI do shadcn ou copiando do site.

### Organizacao

```
frontend/src/
├── api/           # funcoes que chamam o backend (axios)
├── components/
│   ├── ui/        # componentes shadcn (Button, Dialog, Select...)
│   └── shared/    # componentes de dominio (FormSheets, Pagination...)
├── pages/         # uma por rota
├── hooks/         # hooks customizados
├── lib/           # utilitarios (cn, formatadores)
├── types/         # interfaces TypeScript
└── store/         # estado Zustand
```

- **React Query** para tudo que vem do servidor (cache, invalidacao)
- **Zustand** so para estado de UI puramente do cliente
- Padrao de pagina: `useQuery` para listar, `useMutation` para gravar,
  `queryClient.invalidateQueries` apos sucesso

### Comandos

```bash
cd frontend
npm install
npm run dev                            # servidor de desenvolvimento (porta 5173)
npm run build                          # build de producao (tsc + vite build)
npm run lint                           # ESLint
```

---

## Banco de Dados

| Ambiente | Banco |
|----------|-------|
| Desenvolvimento / Testes | H2 em memoria |
| Producao | PostgreSQL 15 via Supabase |

- Schema gerado por Hibernate `ddl-auto=update`
- **Atencao**: `ddl-auto=update` adiciona colunas/tabelas mas NAO remove
  NOT NULL, NAO altera tipo de coluna e NAO atualiza CHECK constraints —
  por isso o projeto usa **migrations SQL manuais idempotentes**
- Storage de arquivos: **Supabase Storage** acessado via REST API com
  service_role key

---

## Infraestrutura e Deploy

| Item | Tecnologia |
|------|------------|
| Hospedagem | AWS Lightsail (Ubuntu 22.04, 2 GB RAM / 2 vCPU) |
| Containers | Docker + Docker Compose |
| Proxy reverso | Nginx |
| TLS / SSL | Certbot (Let's Encrypt) |
| CI/CD | Nenhum — deploy manual via script SSH |

### Fluxo de deploy

```bash
ssh ubuntu@<IP>
cd /opt/humaniza/patient-service
./deploy/update.sh        # git pull + docker compose up --build + healthcheck
```

- `docker-compose.yml` — base
- `docker-compose.prod.yml` — override de producao
- `deploy/setup-lightsail.sh` — provisionamento inicial da instancia
- `deploy/nginx-ssl.conf` — config do Nginx com TLS

---

## Como replicar a stack num projeto novo

### 1. Backend
- Gerar projeto em https://start.spring.io com Java 17, Spring Boot 3.5.x
- Selecionar starters: Web, Data JPA, Validation, Security, DevTools
- Adicionar manualmente ao `pom.xml`: jjwt (3 artefatos), postgresql, h2,
  lombok, springdoc-openapi, e o jacoco-maven-plugin
- Criar perfis `application.properties` (default/H2),
  `application-test.properties`, `application-prod.properties`

### 2. Frontend
```bash
npm create vite@latest frontend -- --template react-ts
cd frontend
npm install react-router-dom @tanstack/react-query axios zustand \
  react-hook-form zod @hookform/resolvers date-fns lucide-react \
  class-variance-authority clsx tailwind-merge
npm install -D tailwindcss postcss autoprefixer tailwindcss-animate
npx tailwindcss init -p
# inicializar shadcn/ui:
npx shadcn@latest init
```

### 3. Banco
- Criar projeto no Supabase (PostgreSQL gerenciado + Storage)
- Configurar `DATABASE_URL`, `SUPABASE_URL`, `SUPABASE_SERVICE_KEY` no `.env`

### 4. Infra
- Instancia Lightsail Ubuntu, instalar Docker (ver `deploy/setup-lightsail.sh`)
- `docker-compose.yml` com servicos backend + frontend (Nginx)
- Certbot para o dominio

---

## Resumo de uma linha

**Spring Boot 3.5 (Java 17) + React 19 (Vite/TS) + PostgreSQL (Supabase) +
Docker em AWS Lightsail.**
