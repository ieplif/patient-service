import { apiClient } from "./client"
import type { PageResponse, Servico } from "@/types"

export async function getServicos(): Promise<Servico[]> {
  const { data } = await apiClient.get<PageResponse<Servico>>("/api/v1/servicos", {
    params: { size: 100, sort: "atividade.nome,asc" },
  })
  return data.content
}
