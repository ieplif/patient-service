import { apiClient } from "./client"
import type { AtividadeSimple, PageResponse } from "@/types"

export async function getAtividades(): Promise<AtividadeSimple[]> {
  const { data } = await apiClient.get<PageResponse<AtividadeSimple>>("/api/v1/atividades", {
    params: { size: 100, sort: "nome,asc" },
  })
  return data.content
}
