import { apiClient } from "./client"
import type { PageResponse, Prontuario, TipoDocumento } from "@/types"

export async function getProntuarios(pacienteId: string, params?: {
  tipo?: TipoDocumento
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
  opts?: { tipo?: TipoDocumento; descricao?: string }
): Promise<Prontuario> {
  const formData = new FormData()
  formData.append("pacienteId", pacienteId)
  formData.append("titulo", titulo)
  formData.append("file", file)
  if (opts?.tipo) formData.append("tipo", opts.tipo)
  if (opts?.descricao) formData.append("descricao", opts.descricao)

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
