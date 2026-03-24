import { apiClient } from "./client"
import type { PageResponse, Profissional } from "@/types"

export interface ProfissionalCreateData {
  nome: string
  email: string
  telefone: string
  senha: string
  atividadeIds: string[]
  googleCalendarId?: string
}

export interface ProfissionalUpdateData {
  nome?: string
  telefone?: string
  atividadeIds?: string[]
  googleCalendarId?: string
}

export async function getProfissionais(params?: {
  page?: number
  size?: number
  sort?: string
}): Promise<PageResponse<Profissional>> {
  const { data } = await apiClient.get<PageResponse<Profissional>>("/api/v1/profissionais", { params })
  return data
}

export async function getProfissional(id: string): Promise<Profissional> {
  const { data } = await apiClient.get<Profissional>(`/api/v1/profissionais/${id}`)
  return data
}

export async function createProfissional(payload: ProfissionalCreateData): Promise<Profissional> {
  const { data } = await apiClient.post<Profissional>("/api/v1/profissionais", payload)
  return data
}

export async function updateProfissional(id: string, payload: ProfissionalUpdateData): Promise<Profissional> {
  const { data } = await apiClient.put<Profissional>(`/api/v1/profissionais/${id}`, payload)
  return data
}

export async function deleteProfissional(id: string): Promise<void> {
  await apiClient.delete(`/api/v1/profissionais/${id}`)
}
