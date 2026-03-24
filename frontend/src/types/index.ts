// ── Auth ──────────────────────────────────────────────────────────────────────
export interface User {
  nome: string
  email: string
  role: string
}

// Flat response from backend: { token, tipo, nome, email, role }
export interface AuthResponse {
  token: string
  tipo: string
  nome: string
  email: string
  role: string
}

// ── Pagination ────────────────────────────────────────────────────────────────
export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
  first: boolean
  last: boolean
  empty: boolean
}

// ── Patients ──────────────────────────────────────────────────────────────────
export interface Patient {
  id: string
  nomeCompleto: string
  email: string
  dataNascimento: string
  telefone: string
  endereco?: string
  profissao?: string
  estadoCivil?: string
  consentimentoLgpd?: boolean
  statusAtivo: boolean
  createdAt: string
}

// ── Enums ─────────────────────────────────────────────────────────────────────
export type StatusAgendamento =
  | "AGENDADO"
  | "CONFIRMADO"
  | "REALIZADO"
  | "CANCELADO"
  | "NAO_COMPARECEU"

export type StatusPagamento =
  | "PENDENTE"
  | "PARCIALMENTE_PAGO"
  | "PAGO"
  | "CANCELADO"
  | "REEMBOLSADO"

export type StatusAssinatura = "ATIVO" | "CANCELADO" | "VENCIDO" | "FINALIZADO"

export type FormaPagamento = "PIX" | "CARTAO_CREDITO" | "CARTAO_DEBITO" | "DINHEIRO"

// ── Agendamentos ──────────────────────────────────────────────────────────────
export interface Agendamento {
  id: string
  pacienteId: string
  pacienteNome: string
  profissionalId: string
  profissionalNome: string
  servicoId: string
  servicoDescricao: string
  dataHora: string
  duracaoMinutos?: number
  status: StatusAgendamento
  observacoes?: string
  ativo: boolean
  createdAt: string
}

// ── Pagamentos ────────────────────────────────────────────────────────────────
export interface Parcela {
  id: string
  numero: number
  valor: number
  dataVencimento: string
  dataPagamento?: string
  status: string
}

export interface Pagamento {
  id: string
  pacienteId: string
  pacienteNome: string
  assinaturaId?: string
  agendamentoId?: string
  valor: number
  formaPagamento: FormaPagamento
  status: StatusPagamento
  numeroParcelas: number
  dataPagamento?: string
  dataVencimento: string
  observacoes?: string
  ativo: boolean
  createdAt: string
  parcelas: Parcela[]
}

// ── Assinaturas ───────────────────────────────────────────────────────────────
export interface Assinatura {
  id: string
  pacienteId: string
  pacienteNome: string
  servicoId: string
  servicoDescricao: string
  dataInicio: string
  dataVencimento?: string
  sessoesContratadas: number
  sessoesRealizadas: number
  sessoesRestantes: number
  status: StatusAssinatura
  valor: number
  observacoes?: string
  ativo: boolean
  createdAt: string
}

// ── Profissionais ─────────────────────────────────────────────────────────────
export interface AtividadeSimple {
  id: string
  nome: string
}

export interface Profissional {
  id: string
  nome: string
  telefone: string
  email: string
  atividades: AtividadeSimple[]
  googleCalendarId?: string
  ativo: boolean
  createdAt: string
}
