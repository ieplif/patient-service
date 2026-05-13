# Regras de Negocio — Clinica Humaniza

Documento consolidado das regras de negocio implementadas no Patient Service.
Escrito em linguagem de negocio (sem detalhes de codigo) para servir de
referencia para a clinica e para quem mantem o sistema.

> **Convencoes**: campos em `codigo` referem-se ao schema do banco/API. Status
> sao escritos em CAIXA ALTA (ex.: `AGENDADO`).

---

## Pacientes

### Cadastro

- **Obrigatorios**: nome completo + telefone
- **Opcionais (recomendados)**: e-mail, CPF, data de nascimento, endereco,
  profissao, estado civil, medico responsavel
- **CPF e e-mail**: unicos quando informados. Multiplos pacientes podem ficar
  sem CPF/e-mail simultaneamente (PostgreSQL aceita varios NULLs em UNIQUE).
- **Telefone**: NAO unico. Mae e filha (ou casal) podem compartilhar.
- **Edicao posterior**: CPF, e-mail e nascimento podem ser preenchidos depois
  via PUT do paciente.

### Status do paciente (visual)

No header da pagina do paciente, o badge "Ativo"/"Inativo" e calculado:

- "Ativo" se o paciente tem >= 1 assinatura ATIVO **ou** nenhuma assinatura
- "Inativo" se tem assinaturas mas todas estao SUSPENSO/CANCELADO/VENCIDO/
  FINALIZADO, **ou** se `statusAtivo=false` no banco (paciente excluido)

A lista de pacientes (`/pacientes`) continua usando o `statusAtivo` do banco
direto, sem essa derivacao.

### Soft delete

`DELETE /api/v1/pacientes/{id}` faz soft delete (`statusAtivo=false`). Hard
delete e feito apenas via SQL direto no banco quando solicitado.

---

## Profissionais

- Cada profissional tem nome, telefone, e-mail, lista de **atividades** que
  atende (ex.: Pilates, Fisioterapia), opcional `google_calendar_id`.
- **Horarios disponiveis** (`HorarioDisponivel`): por profissional + dia da
  semana + faixa horaria (horaInicio/horaFim). Usado para validar agendamentos
  futuros.
- A validacao de "dentro do horario disponivel" e **pulada para agendamentos
  retroativos** (data < hoje) — assume-se que o profissional pode ter mudado
  de turnos desde entao.

---

## Servicos

- Vinculados a uma **Atividade** (ex.: "Pilates Classico", "Fisioterapia
  Pelvica") e um **Plano** (ex.: "Mensalidade 2x/semana", "Trimestral").
- Atividade define a `capacidadeMaxima` da turma (ex.: Pilates Classico = 6
  pacientes simultaneos).
- Plano define `validadeDias` (ex.: Mensalidade = 30, Trimestral = 90).
- Servico tem `quantidade` (sessoes inclusas), `valor`, `tipoAtendimento`
  (presencial/online), `modalidadeLocal`.

---

## Assinaturas

### Estados

| Status | Descricao |
|---|---|
| `ATIVO` | Em uso. Recebe agendamentos. Conta no card "Assinaturas Ativas" do Dashboard. |
| `SUSPENSO` | Pausada (gravidez/lesao/viagem). Saldo preservado. Renovacao automatica nao roda. |
| `CANCELADO` | Cancelada manualmente pela recepcao. |
| `VENCIDO` | Data de vencimento passou sem renovacao automatica. |
| `FINALIZADO` | Sessoes contratadas todas realizadas. |

### Suspensao (gravidez, lesao, viagem)

Acao "Suspender" no dropdown da assinatura ATIVO:

- Pede **motivo** (obrigatorio) + **data prevista de retomada** (opcional)
- Cancela agendamentos futuros pendentes (AGENDADO/CONFIRMADO) com motivo
  "Suspensao da assinatura: ..." e **sem** direito a reposicao
- Desativa templates `AgendamentoRecorrente` vinculados
- Renovacao automatica nao toca em assinatura SUSPENSO (scheduler filtra por
  ATIVO)
- Saldo de sessoes (`sessoesContratadas - sessoesRealizadas`) **preservado**

### Reativacao

Acao "Reativar" no dropdown da assinatura SUSPENSO:

- Reseta `dataInicio = hoje` e `dataVencimento = hoje + plano.validadeDias`
- Limpa os campos de suspensao
- Saldo de sessoes intacto
- Para recriar agendamentos com os horarios antigos (ou novos): usar
  "Editar" depois (chama `regenerar-horarios`)

### Profissional opcional

Para servicos de Pilates onde o profissional varia (quem ministra a aula
do dia), a assinatura pode ser criada **sem profissional fixo**. Os
agendamentos gerados ficam com `profissionalId=null` e aparecem como
"Sem profissional". A recepcao pode atribuir depois via edicao do agendamento.

### Renovacao automatica (mensalidades)

- Checkbox "Renovacao automatica" no form de assinatura — auto-marcado para
  servicos com plano "Mensal"
- Scheduler roda diariamente as **06h** (`AssinaturaRenovacaoService`)
- Renova assinaturas ATIVO com `dataVencimento <= hoje + 3 dias`
- Estende `dataVencimento`, incrementa `sessoesContratadas`
- Recria agendamentos usando os templates `AgendamentoRecorrente` ATIVOS

### Regenerar horarios

Editando uma assinatura, ao preencher novos slots de horarios fixos:

- Cancela agendamentos futuros pendentes (a partir de amanha)
- Desativa templates antigos
- Cria novos templates + agendamentos do dia seguinte ate `dataVencimento`
- Historico (REALIZADO/CANCELADO/NAO_COMPARECEU) **nao** e tocado

---

## Agendamentos

### Estados

| Status | Descricao |
|---|---|
| `AGENDADO` | Marcado, ainda nao confirmado pela paciente. |
| `CONFIRMADO` | Paciente confirmou presenca. |
| `REALIZADO` | Aula/sessao aconteceu. Conta como sessao usada da assinatura. |
| `CANCELADO` | Cancelado pela recepcao. Pode gerar reposicao (Pilates). |
| `NAO_COMPARECEU` | Paciente faltou sem avisar. Sem direito a reposicao. |

### Transicoes permitidas

```
AGENDADO ─┬─→ CONFIRMADO ─┬─→ REALIZADO
          │               ├─→ CANCELADO
          │               └─→ NAO_COMPARECEU
          ├─→ REALIZADO       (atalho — util para retroativos e quando
          │                    recepcao esquece de confirmar antes)
          ├─→ CANCELADO
          └─→ NAO_COMPARECEU

REALIZADO, CANCELADO, NAO_COMPARECEU → (estados finais, nao mudam mais)
```

### Diferenca CANCELADO vs NAO_COMPARECEU

| | CANCELADO | NAO_COMPARECEU |
|---|---|---|
| Quem aciona | Recepcao quando paciente avisa antes | Recepcao depois (paciente faltou) |
| Modal proprio | Sim (motivo + checkbox de reposicao) | Nao |
| Direito a reposicao | Pode gerar (Pilates) | Nunca |
| Google Calendar | Evento removido | Permanece (historico) |
| Origem de reposicao | Sim (`criarReposicao` exige CANCELADO) | Nao |

### Validacoes na criacao

- **Conflito de horario**: numero de agendamentos sobrepostos do profissional
  no slot nao pode exceder `atividade.capacidadeMaxima`
- **Dentro do horario disponivel**: o slot tem que cair dentro de um
  `HorarioDisponivel` ativo do profissional **(pulado para agendamentos
  retroativos)**
- **Assinatura valida**: se `assinaturaId` informado, ela tem que estar `ATIVO`
- **Profissional atende a atividade**: validado quando profissional e informado

### Datas retroativas

- Aceitas tanto em criacao individual quanto em recorrente
- Validacao de `HorarioDisponivel` e **pulada** quando `dataHora < agora`
- Validacao de capacidade da turma continua valendo (integridade do historico)
- Forms exibem badge ambar **"Retroativo"** quando data < hoje
- Confirmacao adicional (`window.confirm`) quando data > 30 dias no passado

---

## Reposicao (especifico de Pilates)

Cancelamento de agendamento Pilates pode gerar uma "reposicao" — direito de
agendar uma aula em outro horario sem custo adicional.

### Quando e concedida

Checkbox **"Gerar direito a reposicao"** no modal de cancelamento decide:

- Default checado para Pilates, desmarcado para outros servicos
- A recepcao pode mudar caso a caso (regra de 3h removida — "atuamos de
  acordo com a situacao")

Quando concedida, o calculo automatico tambem verifica:

- Servico e Pilates? (substring "pilates" no nome da atividade)
- Nao e uma reposicao de reposicao? (`tipoAgendamento != REPOSICAO`)
- Nao e feriado? (`FeriadoRepository.isFeriado(data)`)

Se algum desses falha, mesmo com checkbox marcado o override e respeitado.

### Limites

- **Validade**: 20 dias corridos a partir do cancelamento
  (`dataLimiteReposicao = agora + 20 dias`)
- **Maximo por mes**: 2 reposicoes por paciente por mes calendario
  (`countReposicoesNoMes` rejeita se >= 2)
- **Nao gera reposicao de reposicao** (evita loop infinito)
- **NAO_COMPARECEU nunca gera reposicao**

### Fluxo

```
Agendamento CANCELADO com direitoReposicao=true
    ↓ (menu "⋯" → "Agendar Reposicao", botao violeta)
ReposicaoFormSheet — escolhe nova data/hora/profissional opcional
    ↓
Novo Agendamento com tipoAgendamento=REPOSICAO + reposicaoOrigemId
    ↓ (vira agendamento normal — gerencia pelo mesmo dropdown)
[AGENDADO] → REALIZADO / CANCELADO / NAO_COMPARECEU
```

A reposicao aparece na lista com badge "Reposicao" violeta ao lado do status.

---

## Pagamentos

### Estados

| Status | Descricao |
|---|---|
| `PENDENTE` | Lancado, ainda nao pago. Conta no card "Pagamentos Pendentes". |
| `PARCIALMENTE_PAGO` | Algumas parcelas pagas. |
| `PAGO` | Quitado. Conta na "Receita do mes" pelo `dataVencimento`. |
| `CANCELADO` | Cancelado antes da quitacao. |
| `REEMBOLSADO` | Pago e depois estornado. |

### Vinculacao

Cada pagamento referencia:
- Um paciente (obrigatorio)
- Opcionalmente uma assinatura **ou** um agendamento avulso
- Tem N parcelas (default 1) — relacao 1:N com `parcelas`

### Forma de pagamento

`PIX`, `CARTAO_CREDITO`, `CARTAO_DEBITO`, `DINHEIRO`. Numero de parcelas
limitado: cartao credito ate 12x, outros ate 2x.

### Marcar como Pago

Acao "Marcar como Pago" pergunta a `dataPagamento` (default hoje, editavel
para permitir registros retroativos com a data correta do recibo). Status
vai para `PAGO`.

### Editar

`PUT /api/v1/pagamentos/{id}` permite alterar:
- forma de pagamento
- data de vencimento
- observacoes

**Nao** editaveis: valor, parcelas, paciente, assinatura/agendamento vinculado.
Para mudar esses campos, cancelar e recriar.

### Receita do mes (Dashboard)

Filtra pagamentos com `status=PAGO` e `dataVencimento` no mes atual. Regime
de competencia: mensalidade vencida em abril, paga em maio, conta como
receita de abril.

### Datas retroativas

`@FutureOrPresent` foi removido de `dataVencimento`. Mensalidades de meses
passados podem ser lancadas normalmente.

---

## Prontuarios

### Tipos de documento

`PRONTUARIO`, `TERMO`, `NOTA_FISCAL` — UI tem abas separadas em
PacienteResumoPage.

### Upload

- Bucket `prontuarios` no Supabase Storage (privado)
- Path: `{pacienteId}/{UUID}_{nomeSanitizado}`
- **Sanitizacao**: acentos, espacos e caracteres especiais sao normalizados
  no nome do arquivo (Supabase Storage rejeita)
- **Tipos aceitos**: PDF, JPEG, PNG, DOC, DOCX
- **Tamanho maximo**: 10MB
- **Rollback automatico**: se o save no banco falhar apos o upload, o arquivo
  e removido do Storage (evita arquivos orfaos consumindo espaco e expondo
  dados de pacientes)
- Colunas `storage_url`, `storage_path` e `nome_arquivo` sao TEXT (URLs
  assinadas do Supabase tem JWT longo e estouravam VARCHAR(255))

### Download

`GET /api/v1/prontuarios/{id}` retorna o DTO com `storageUrl` (URL assinada
com TTL 1h). O frontend abre direto no navegador.

---

## Datas e Horarios

### Datas retroativas (geral)

Suportado em: criacao de agendamento, criacao de recorrente, criacao de
pagamento (vencimento), marcar pagamento como pago (com data efetiva).

Validacoes que sao puladas quando data < hoje:
- `validarDentroDoHorarioDisponivel` em agendamento (profissional pode ter
  mudado de turnos)

Validacoes que continuam valendo:
- Conflito de capacidade da turma (integridade do historico)
- Tipo/status da assinatura
- Pertinencia do profissional a atividade

### UI

Forms (`AgendamentoFormSheet`, `PagamentoFormSheet`, `AssinaturaFormSheet`)
mostram badge ambar **"Retroativo"** quando a data informada esta no passado.

Confirmacao adicional (`window.confirm`) quando data > 30 dias no passado.

### Feriados

Tabela `feriados` com data + descricao + flag `recorrente` (anual). Verificado
em duas situacoes:
- Calculo automatico de direito a reposicao (cancelamento em dia de feriado
  nao gera reposicao)
- Possivelmente em validacoes futuras

---

## Renovacao automatica

`AssinaturaRenovacaoService.renovarAssinaturasProximasDoVencimento`:

- Dispara no scheduler diario as **06h** (cron `"0 0 6 * * *"`)
- Tambem pode ser disparado manualmente via `POST /api/v1/assinaturas/renovar`
- Procura assinaturas com `renovacaoAutomatica=true`, `status=ATIVO`,
  `dataVencimento <= hoje + 3 dias`
- Para cada uma:
  - Estende `dataVencimento` em `plano.validadeDias` dias
  - Incrementa `sessoesContratadas`
  - Recria agendamentos a partir dos templates `AgendamentoRecorrente`
    **ATIVOS** (templates inativados por regenerar/suspender nao sao usados)
  - Registra em `observacoes`: "Renovado automaticamente em DD/MM/YYYY"

---

## Migrations

Hibernate `ddl-auto=update` em producao **adiciona** colunas/tabelas mas
**nao**:
- Remove constraint NOT NULL
- Altera tipo de coluna (ex.: VARCHAR -> TEXT)
- Atualiza valores aceitos por CHECK constraint
- Adiciona DEFAULT em coluna existente

Por isso temos scripts manuais idempotentes (rodaveis varias vezes).
**Rodar ANTES do `update.sh` correspondente**.

| Arquivo | O que faz |
|---|---|
| `seed-postgres.sql` | Seed inicial — atividades, planos, servicos, profissionais e pacientes de teste. **1x quando criar o banco.** |
| `migration-campos-opcionais.sql` | Profissional opcional em agendamentos + email/CPF/nascimento opcionais em pacientes. |
| `migration-prontuarios-text-columns.sql` | storage_url/storage_path/nome_arquivo como TEXT + DEFAULT true em `ativo`. |
| `migration-assinatura-suspender.sql` | Colunas de suspensao + UPDATE do CHECK constraint para aceitar SUSPENSO. |

---

## Seguranca

- Registro publico fechado: `/api/auth/registrar` requer `ROLE_ADMIN`
- Login em `/api/auth/login` (POST {email, senha}) — devolve JWT em cookie
  httpOnly
- JWT expira em 24h
- Roles: `ROLE_ADMIN` (acesso total) e `ROLE_PROFISSIONAL` (parcial — sem
  ver pagamentos)
- CORS configurado para o dominio `app.humanizarj.com.br`
- Senhas com BCrypt (forca 10)

---

## Auditoria / LGPD

- `createdAt` / `updatedAt` em todas as entidades principais
- `uploadedBy` (email do usuario logado) em prontuarios
- Soft delete: `ativo=false` preserva o registro para auditoria
- Hard delete disponivel para pacientes (LGPD direito ao esquecimento) — feito
  via SQL direto, com anonimizacao de campos antes do DELETE
- Consentimento LGPD: campo `consentimentoLgpd` + `dataConsentimentoLgpd`
  no paciente. Quando o usuario marca, o sistema registra a data automaticamente
