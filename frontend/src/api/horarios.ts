import { apiClient } from "./client"
import type { HorarioDisponivel } from "@/types"

export interface HorarioCreateData {
  profissionalId: string
  diaSemana: string
  horaInicio: string
  horaFim: string
}

export interface HorarioUpdateData {
  diaSemana?: string
  horaInicio?: string
  horaFim?: string
}

export async function getHorariosByProfissional(profissionalId: string): Promise<HorarioDisponivel[]> {
  const { data } = await apiClient.get<HorarioDisponivel[]>(
    `/api/v1/disponibilidades/profissional/${profissionalId}`
  )
  return data
}

export async function createHorario(payload: HorarioCreateData): Promise<HorarioDisponivel> {
  const { data } = await apiClient.post<HorarioDisponivel>("/api/v1/disponibilidades", payload)
  return data
}

export async function updateHorario(id: string, payload: HorarioUpdateData): Promise<HorarioDisponivel> {
  const { data } = await apiClient.put<HorarioDisponivel>(`/api/v1/disponibilidades/${id}`, payload)
  return data
}

export async function deleteHorario(id: string): Promise<void> {
  await apiClient.delete(`/api/v1/disponibilidades/${id}`)
}
