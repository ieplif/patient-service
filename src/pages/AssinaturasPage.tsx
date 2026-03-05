import { useState } from "react"
import { useQuery } from "@tanstack/react-query"
import { Star } from "lucide-react"
import { format } from "date-fns"
import { getAssinaturas } from "@/api/assinaturas"
import type { StatusAssinatura } from "@/types"
import { Skeleton } from "@/components/ui/skeleton"
import { Card, CardContent, CardHeader } from "@/components/ui/card"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { Pagination } from "@/components/shared/Pagination"

const PAGE_SIZE = 15

const statusConfig: Record<StatusAssinatura, { label: string; className: string }> = {
  ATIVO: { label: "Ativo", className: "bg-primary/15 text-primary border-primary/30" },
  CANCELADO: { label: "Cancelado", className: "bg-destructive/10 text-destructive border-destructive/20" },
  VENCIDO: { label: "Vencido", className: "bg-accent/15 text-accent border-accent/30" },
  FINALIZADO: { label: "Finalizado", className: "bg-muted text-muted-foreground border-border" },
}

function formatCurrency(v: number) {
  return new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }).format(v)
}

const statusOptions: { value: StatusAssinatura | "TODOS"; label: string }[] = [
  { value: "TODOS", label: "Todos os status" },
  { value: "ATIVO", label: "Ativo" },
  { value: "CANCELADO", label: "Cancelado" },
  { value: "VENCIDO", label: "Vencido" },
  { value: "FINALIZADO", label: "Finalizado" },
]

export function AssinaturasPage() {
  const [page, setPage] = useState(0)
  const [statusFilter, setStatusFilter] = useState<StatusAssinatura | "TODOS">("TODOS")

  const { data, isLoading } = useQuery({
    queryKey: ["assinaturas", page, statusFilter],
    queryFn: () =>
      getAssinaturas({
        page,
        size: PAGE_SIZE,
        sort: "createdAt,desc",
        status: statusFilter !== "TODOS" ? statusFilter : undefined,
      }),
  })

  return (
    <div className="space-y-5 animate-fade-in">
      <div>
        <h1 className="text-2xl font-bold font-primary text-foreground tracking-tight flex items-center gap-2">
          <Star className="h-6 w-6 text-primary" />
          Assinaturas
        </h1>
        <p className="text-sm text-muted-foreground font-secondary mt-0.5">
          {data ? `${data.totalElements} assinaturas` : "Carregando..."}
        </p>
      </div>

      <Card className="border border-border/60 shadow-soft">
        <CardHeader className="pb-3">
          <Select
            value={statusFilter}
            onValueChange={(v) => { setStatusFilter(v as StatusAssinatura | "TODOS"); setPage(0) }}
          >
            <SelectTrigger className="w-52 bg-background border-border/70 font-secondary text-sm">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {statusOptions.map((o) => (
                <SelectItem key={o.value} value={o.value} className="font-secondary text-sm">
                  {o.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </CardHeader>

        <CardContent className="p-0">
          {isLoading ? (
            <div className="space-y-2 p-6">
              {Array.from({ length: 8 }).map((_, i) => (
                <Skeleton key={i} className="h-11 w-full" />
              ))}
            </div>
          ) : (
            <>
              <Table>
                <TableHeader>
                  <TableRow className="border-border/50 hover:bg-transparent">
                    {["Paciente", "Serviço", "Valor", "Sessões", "Progresso", "Vencimento", "Status"].map((h) => (
                      <TableHead key={h} className="text-xs font-semibold font-primary text-muted-foreground uppercase tracking-wide">
                        {h}
                      </TableHead>
                    ))}
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {data?.content.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={7} className="text-center text-muted-foreground font-secondary py-12">
                        Nenhuma assinatura encontrada
                      </TableCell>
                    </TableRow>
                  ) : (
                    data?.content.map((as) => {
                      const cfg = statusConfig[as.status]
                      const progresso = Math.round((as.sessoesRealizadas / as.sessoesContratadas) * 100)
                      return (
                        <TableRow key={as.id} className="border-border/40 hover:bg-muted/20">
                          <TableCell className="font-semibold font-primary text-sm text-foreground">
                            {as.pacienteNome}
                          </TableCell>
                          <TableCell className="text-sm font-secondary text-muted-foreground max-w-[130px] truncate">
                            {as.servicoDescricao}
                          </TableCell>
                          <TableCell className="text-sm font-secondary font-semibold text-accent">
                            {formatCurrency(as.valor)}
                          </TableCell>
                          <TableCell className="text-sm font-secondary text-muted-foreground text-center">
                            {as.sessoesContratadas}
                          </TableCell>
                          <TableCell>
                            <div className="flex items-center gap-2 min-w-[80px]">
                              <div className="flex-1 h-1.5 rounded-full bg-muted overflow-hidden">
                                <div
                                  className="h-full bg-primary rounded-full transition-all"
                                  style={{ width: `${progresso}%` }}
                                />
                              </div>
                              <span className="text-xs text-muted-foreground font-secondary shrink-0">
                                {as.sessoesRealizadas}/{as.sessoesContratadas}
                              </span>
                            </div>
                          </TableCell>
                          <TableCell className="text-sm font-secondary text-muted-foreground">
                            {as.dataVencimento
                              ? format(new Date(as.dataVencimento), "dd/MM/yyyy")
                              : "—"}
                          </TableCell>
                          <TableCell>
                            <span className={`inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-semibold font-primary ${cfg.className}`}>
                              {cfg.label}
                            </span>
                          </TableCell>
                        </TableRow>
                      )
                    })
                  )}
                </TableBody>
              </Table>
              <Pagination
                page={page}
                totalPages={data?.totalPages ?? 0}
                totalElements={data?.totalElements ?? 0}
                size={PAGE_SIZE}
                onPageChange={setPage}
              />
            </>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
