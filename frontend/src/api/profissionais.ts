import { apiClient } from "./client"
import type { PageResponse, Profissional } from "@/types"

export async function getProfissionais(params?: {
  page?: number
  size?: number
  sort?: string
}): Promise<PageResponse<Profissional>> {
  const { data } = await apiClient.get<PageResponse<Profissional>>("/api/v1/profissionais", {
    params,
  })
  return data
}

export async function getProfissional(id: string): Promise<Profissional> {
  const { data } = await apiClient.get<Profissional>(`/api/v1/profissionais/${id}`)
  return data
}
