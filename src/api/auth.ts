import { apiClient } from "./client"
import type { AuthResponse } from "@/types"

export async function loginRequest(
  email: string,
  senha: string
): Promise<AuthResponse> {
  const { data } = await apiClient.post<AuthResponse>("/api/auth/login", {
    email,
    senha,
  })
  return data
}
