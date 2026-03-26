import { apiClient } from "./client"
import type { PageResponse, Prontuario } from "@/types"

export async function getProntuarios(pacienteId: string, params?: {
  page?: number
  size?: number
}): Promise<PageResponse<Prontuario>> {
  const { data } = await apiClient.get<PageResponse<Prontuario>>(
    `/api/v1/prontuarios/paciente/${pacienteId}`,
    { params }
  )
  return data
}

export async function uploadProntuario(
  pacienteId: string,
  titulo: string,
  file: File,
  descricao?: string
): Promise<Prontuario> {
  const formData = new FormData()
  formData.append("pacienteId", pacienteId)
  formData.append("titulo", titulo)
  formData.append("file", file)
  if (descricao) formData.append("descricao", descricao)

  const { data } = await apiClient.post<Prontuario>("/api/v1/prontuarios", formData, {
    headers: { "Content-Type": "multipart/form-data" },
  })
  return data
}

export async function getProntuarioUrl(id: string): Promise<string> {
  const { data } = await apiClient.get<Prontuario>(`/api/v1/prontuarios/${id}`)
  return data.storageUrl
}

export async function deleteProntuario(id: string): Promise<void> {
  await apiClient.delete(`/api/v1/prontuarios/${id}`)
}
