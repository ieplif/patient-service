import { apiClient } from "./client"
import type { PageResponse, Pagamento, StatusPagamento, FormaPagamento } from "@/types"

export async function getPagamentos(params?: {
  page?: number
  size?: number
  sort?: string
  status?: StatusPagamento
  /** Filtro por múltiplos status (status IN ...). Ex.: pendentes em aberto = PENDENTE + PARCIALMENTE_PAGO. */
  statusIn?: StatusPagamento[]
  formaPagamento?: FormaPagamento
  pacienteId?: string
  /** Busca por nome do paciente (LIKE case-insensitive). */
  pacienteNome?: string
  /** Filtro por dataVencimento. */
  inicio?: string
  fim?: string
  /** Filtro por dataPagamento (data efetiva). Use para Receita do mês, relatórios. */
  pagamentoInicio?: string
  pagamentoFim?: string
}): Promise<PageResponse<Pagamento>> {
  const { statusIn, ...rest } = params ?? {}
  const query = {
    ...rest,
    ...(statusIn && statusIn.length ? { statusIn: statusIn.join(",") } : {}),
  }
  const { data } = await apiClient.get<PageResponse<Pagamento>>("/api/v1/pagamentos", {
    params: query,
  })
  return data
}

export async function getPagamento(id: string): Promise<Pagamento> {
  const { data } = await apiClient.get<Pagamento>(`/api/v1/pagamentos/${id}`)
  return data
}

/**
 * Soma de parcelas PAGAS no período. Reflete o caixa efetivo —
 * inclui pagamentos parcialmente pagos. Para o card "Receita do mês".
 */
export async function getReceita(inicio: string, fim: string): Promise<number> {
  const { data } = await apiClient.get<number>("/api/v1/pagamentos/receita", {
    params: { inicio, fim },
  })
  return Number(data)
}

export async function createPagamento(payload: {
  pacienteId: string
  assinaturaIds?: string[]
  agendamentoId?: string
  valor: number
  formaPagamento: FormaPagamento
  numeroParcelas?: number
  dataVencimento: string
  observacoes?: string
}): Promise<Pagamento> {
  const { data } = await apiClient.post<Pagamento>("/api/v1/pagamentos", payload)
  return data
}

export async function updatePagamento(
  id: string,
  payload: {
    pacienteId?: string
    assinaturaIds?: string[]
    agendamentoId?: string
    valor?: number
    formaPagamento?: FormaPagamento
    numeroParcelas?: number
    dataVencimento?: string
    observacoes?: string
  }
): Promise<Pagamento> {
  const { data } = await apiClient.put<Pagamento>(`/api/v1/pagamentos/${id}`, payload)
  return data
}

export async function updatePagamentoStatus(
  id: string,
  status: StatusPagamento,
  dataPagamento?: string
): Promise<Pagamento> {
  const { data } = await apiClient.patch<Pagamento>(`/api/v1/pagamentos/${id}/status`, {
    status,
    ...(dataPagamento ? { dataPagamento } : {}),
  })
  return data
}

export type StatusParcela = "PENDENTE" | "PAGO" | "CANCELADO"

export async function updateParcelaStatus(
  pagamentoId: string,
  parcelaId: string,
  status: StatusParcela,
  dataPagamento?: string
): Promise<Pagamento> {
  const { data } = await apiClient.patch<Pagamento>(
    `/api/v1/pagamentos/${pagamentoId}/parcelas/${parcelaId}/status`,
    {
      status,
      ...(dataPagamento ? { dataPagamento } : {}),
    }
  )
  return data
}
