import { apiClient } from "./client"
import type { PageResponse, Assinatura, StatusAssinatura, Agendamento } from "@/types"

export interface HorarioFixoSlotDTO {
  diaSemana: "MONDAY" | "TUESDAY" | "WEDNESDAY" | "THURSDAY" | "FRIDAY" | "SATURDAY" | "SUNDAY"
  horaInicio: string // "HH:mm"
}

export interface RegenerarHorariosPayload {
  horariosFixos: HorarioFixoSlotDTO[]
  profissionalId?: string
  dataInicioRegeneracao?: string // YYYY-MM-DD
}

export interface RegenerarHorariosResult {
  agendamentosCancelados: number
  agendamentosCriados: number
  novosAgendamentos: Agendamento[]
  datasIgnoradas: { data: string; motivo: string }[]
}

export async function getAssinaturas(params?: {
  page?: number
  size?: number
  sort?: string
  status?: StatusAssinatura
  pacienteId?: string
  /** Busca por nome do paciente (LIKE case-insensitive). */
  pacienteNome?: string
}): Promise<PageResponse<Assinatura>> {
  const { data } = await apiClient.get<PageResponse<Assinatura>>("/api/v1/assinaturas", {
    params,
  })
  return data
}

export async function getAssinatura(id: string): Promise<Assinatura> {
  const { data } = await apiClient.get<Assinatura>(`/api/v1/assinaturas/${id}`)
  return data
}

export async function createAssinatura(payload: {
  pacienteId: string
  servicoId: string
  dataInicio: string
  dataVencimento?: string
  sessoesContratadas: number
  valor: number
  observacoes?: string
  renovacaoAutomatica?: boolean
}): Promise<Assinatura> {
  const { data } = await apiClient.post<Assinatura>("/api/v1/assinaturas", payload)
  return data
}

export async function updateAssinatura(
  id: string,
  payload: {
    pacienteId?: string
    servicoId?: string
    dataInicio?: string
    dataVencimento?: string
    sessoesContratadas?: number
    valor?: number
    observacoes?: string
    renovacaoAutomatica?: boolean
  }
): Promise<Assinatura> {
  const { data } = await apiClient.put<Assinatura>(`/api/v1/assinaturas/${id}`, payload)
  return data
}

export async function updateAssinaturaStatus(
  id: string,
  status: StatusAssinatura
): Promise<Assinatura> {
  const { data } = await apiClient.patch<Assinatura>(`/api/v1/assinaturas/${id}/status`, {
    status,
  })
  return data
}

export async function deleteAssinatura(id: string): Promise<void> {
  await apiClient.delete(`/api/v1/assinaturas/${id}`)
}

/** Renova manualmente uma assinatura: gera o próximo ciclo e reativa se estiver FINALIZADA. */
export async function renovarAssinatura(id: string): Promise<Assinatura> {
  const { data } = await apiClient.post<Assinatura>(`/api/v1/assinaturas/${id}/renovar`)
  return data
}

export async function suspenderAssinatura(
  id: string,
  payload: { motivo: string; dataPrevistaRetomada?: string }
): Promise<Assinatura> {
  const { data } = await apiClient.post<Assinatura>(
    `/api/v1/assinaturas/${id}/suspender`,
    payload
  )
  return data
}

export async function reativarAssinatura(
  id: string,
  payload?: { dataInicio?: string; recriarAgendamentos?: boolean }
): Promise<Assinatura> {
  const { data } = await apiClient.post<Assinatura>(
    `/api/v1/assinaturas/${id}/reativar`,
    payload ?? {}
  )
  return data
}

export async function regenerarHorarios(
  id: string,
  payload: RegenerarHorariosPayload
): Promise<RegenerarHorariosResult> {
  const { data } = await apiClient.post<RegenerarHorariosResult>(
    `/api/v1/assinaturas/${id}/regenerar-horarios`,
    payload
  )
  return data
}
