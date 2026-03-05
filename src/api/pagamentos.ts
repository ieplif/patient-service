import { apiClient } from "./client"
import type { PageResponse, Pagamento, StatusPagamento, FormaPagamento } from "@/types"

export async function getPagamentos(params?: {
  page?: number
  size?: number
  sort?: string
  status?: StatusPagamento
  formaPagamento?: FormaPagamento
  pacienteId?: string
  inicio?: string
  fim?: string
}): Promise<PageResponse<Pagamento>> {
  const { data } = await apiClient.get<PageResponse<Pagamento>>("/api/v1/pagamentos", { params })
  return data
}

export async function getPagamento(id: string): Promise<Pagamento> {
  const { data } = await apiClient.get<Pagamento>(`/api/v1/pagamentos/${id}`)
  return data
}

export async function updatePagamentoStatus(
  id: string,
  status: StatusPagamento
): Promise<Pagamento> {
  const { data } = await apiClient.patch<Pagamento>(`/api/v1/pagamentos/${id}/status`, {
    status,
  })
  return data
}
