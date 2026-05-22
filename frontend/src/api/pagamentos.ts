import { apiClient } from "./client"
import type { PageResponse, Pagamento, StatusPagamento, FormaPagamento } from "@/types"

export async function getPagamentos(params?: {
  page?: number
  size?: number
  sort?: string
  status?: StatusPagamento
  formaPagamento?: FormaPagamento
  pacienteId?: string
  /** Filtro por dataVencimento. */
  inicio?: string
  fim?: string
  /** Filtro por dataPagamento (data efetiva). Use para Receita do mês, relatórios. */
  pagamentoInicio?: string
  pagamentoFim?: string
}): Promise<PageResponse<Pagamento>> {
  const { data } = await apiClient.get<PageResponse<Pagamento>>("/api/v1/pagamentos", { params })
  return data
}

export async function getPagamento(id: string): Promise<Pagamento> {
  const { data } = await apiClient.get<Pagamento>(`/api/v1/pagamentos/${id}`)
  return data
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
