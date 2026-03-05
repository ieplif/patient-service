import { apiClient } from "./client"
import type { PageResponse, Patient } from "@/types"

export async function getPatients(params?: {
  page?: number
  size?: number
  sort?: string
  nome?: string
  email?: string
  cpf?: string
}): Promise<PageResponse<Patient>> {
  const { data } = await apiClient.get<PageResponse<Patient>>("/api/v1/patients", { params })
  return data
}

export async function getPatient(id: string): Promise<Patient> {
  const { data } = await apiClient.get<Patient>(`/api/v1/patients/${id}`)
  return data
}
