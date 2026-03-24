import { apiClient } from "./client"
import type { PageResponse, Patient } from "@/types"

export interface PatientCreateData {
  nomeCompleto: string
  email: string
  cpf: string
  dataNascimento: string
  telefone: string
  endereco?: string
  profissao?: string
  estadoCivil?: string
  medicoResponsavel?: string
  consentimentoLgpd?: boolean
}

export interface PatientUpdateData {
  nomeCompleto?: string
  telefone?: string
  endereco?: string
  profissao?: string
  estadoCivil?: string
  medicoResponsavel?: string
  statusAtivo?: boolean
  consentimentoLgpd?: boolean
}

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

export async function createPatient(payload: PatientCreateData): Promise<Patient> {
  const { data } = await apiClient.post<Patient>("/api/v1/patients", payload)
  return data
}

export async function updatePatient(id: string, payload: PatientUpdateData): Promise<Patient> {
  const { data } = await apiClient.put<Patient>(`/api/v1/patients/${id}`, payload)
  return data
}

export async function deletePatient(id: string): Promise<void> {
  await apiClient.delete(`/api/v1/patients/${id}`)
}
