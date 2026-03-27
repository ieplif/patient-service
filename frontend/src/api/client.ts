import axios from "axios"
import { useAuthStore } from "@/store/authStore"

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? "",
  withCredentials: true, // envia httpOnly cookie automaticamente
})

// Não precisa mais injetar Authorization header — cookie é enviado automaticamente
// Mantemos apenas o interceptor de resposta para logout automático em 401
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      useAuthStore.getState().logout()
      window.location.href = "/login"
    }
    return Promise.reject(error)
  }
)
