import { apiClient } from "./client"
import type { PageResponse, Agendamento, StatusAgendamento } from "@/types"

export async function getAgendamentos(params?: {
  page?: number
  size?: number
  sort?: string
  status?: StatusAgendamento
  pacienteId?: string
  profissionalId?: string
  dataInicio?: string
  dataFim?: string
}): Promise<PageResponse<Agendamento>> {
  const { data } = await apiClient.get<PageResponse<Agendamento>>("/api/v1/agendamentos", {
    params,
  })
  return data
}

export async function getAgendamento(id: string): Promise<Agendamento> {
  const { data } = await apiClient.get<Agendamento>(`/api/v1/agendamentos/${id}`)
  return data
}

export async function updateAgendamentoStatus(
  id: string,
  status: StatusAgendamento
): Promise<Agendamento> {
  const { data } = await apiClient.patch<Agendamento>(`/api/v1/agendamentos/${id}/status`, {
    status,
  })
  return data
}
