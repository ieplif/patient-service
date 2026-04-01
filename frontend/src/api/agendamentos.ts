import { apiClient } from "./client"
import type { PageResponse, Agendamento, StatusAgendamento, ReposicaoInfo } from "@/types"

export interface AgendamentoCreateData {
  pacienteId: string
  profissionalId: string
  servicoId: string
  dataHora: string
  duracaoMinutos?: number
  observacoes?: string
}

export interface AgendamentoUpdateData {
  dataHora?: string
  duracaoMinutos?: number
  observacoes?: string
}

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

export async function createAgendamento(payload: AgendamentoCreateData): Promise<Agendamento> {
  const { data } = await apiClient.post<Agendamento>("/api/v1/agendamentos", payload)
  return data
}

export async function updateAgendamento(id: string, payload: AgendamentoUpdateData): Promise<Agendamento> {
  const { data } = await apiClient.put<Agendamento>(`/api/v1/agendamentos/${id}`, payload)
  return data
}

export async function deleteAgendamento(id: string): Promise<void> {
  await apiClient.delete(`/api/v1/agendamentos/${id}`)
}

export async function updateAgendamentoStatus(
  id: string,
  status: StatusAgendamento,
  motivoCancelamento?: string
): Promise<Agendamento> {
  const { data } = await apiClient.patch<Agendamento>(`/api/v1/agendamentos/${id}/status`, {
    status,
    ...(motivoCancelamento ? { motivoCancelamento } : {}),
  })
  return data
}

export interface ReposicaoCreateData {
  agendamentoOrigemId: string
  profissionalId: string
  dataHora: string
  duracaoMinutos?: number
  observacoes?: string
}

export async function criarReposicao(payload: ReposicaoCreateData): Promise<Agendamento> {
  const { data } = await apiClient.post<Agendamento>("/api/v1/agendamentos/reposicao", payload)
  return data
}

export interface AgendamentoRecorrenteData {
  pacienteId: string
  profissionalId: string
  servicoId: string
  assinaturaId?: string
  frequencia: "SEMANAL" | "QUINZENAL" | "MENSAL"
  diasSemana: string[]
  horaInicio: string
  duracaoMinutos?: number
  totalSessoes?: number
  dataFim?: string
  observacoes?: string
}

export interface AgendamentoRecorrenteResult {
  agendamentosCriados: Agendamento[]
  datasIgnoradas: { data: string; motivo: string }[]
}

export async function createAgendamentoRecorrente(
  payload: AgendamentoRecorrenteData
): Promise<AgendamentoRecorrenteResult> {
  const { data } = await apiClient.post<AgendamentoRecorrenteResult>(
    "/api/v1/agendamentos/recorrente",
    payload
  )
  return data
}

export async function getReposicoesInfo(pacienteId: string): Promise<ReposicaoInfo> {
  const { data } = await apiClient.get<ReposicaoInfo>(
    `/api/v1/agendamentos/paciente/${pacienteId}/reposicoes-info`
  )
  return data
}
