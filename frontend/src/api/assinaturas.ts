import { apiClient } from "./client"
import type { PageResponse, Assinatura, StatusAssinatura } from "@/types"

export async function getAssinaturas(params?: {
  page?: number
  size?: number
  sort?: string
  status?: StatusAssinatura
  pacienteId?: string
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

export async function updateAssinaturaStatus(
  id: string,
  status: StatusAssinatura
): Promise<Assinatura> {
  const { data } = await apiClient.patch<Assinatura>(`/api/v1/assinaturas/${id}/status`, {
    status,
  })
  return data
}
