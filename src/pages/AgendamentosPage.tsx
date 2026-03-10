import { useState } from "react"
import { useQuery } from "@tanstack/react-query"
import { Calendar } from "lucide-react"
import { format } from "date-fns"
import { ptBR } from "date-fns/locale"
import { getAgendamentos } from "@/api/agendamentos"
import type { StatusAgendamento } from "@/types"
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

const statusConfig: Record<StatusAgendamento, { label: string; className: string }> = {
  AGENDADO: { label: "Agendado", className: "bg-secondary/40 text-[hsl(202,40%,40%)] border-secondary/50" },
  CONFIRMADO: { label: "Confirmado", className: "bg-primary/15 text-primary border-primary/30" },
  REALIZADO: { label: "Realizado", className: "bg-muted text-muted-foreground border-border" },
  CANCELADO: { label: "Cancelado", className: "bg-destructive/10 text-destructive border-destructive/20" },
  NAO_COMPARECEU: { label: "Não compareceu", className: "bg-accent/15 text-accent border-accent/30" },
}

const statusOptions: { value: StatusAgendamento | "TODOS"; label: string }[] = [
  { value: "TODOS", label: "Todos os status" },
  { value: "AGENDADO", label: "Agendado" },
  { value: "CONFIRMADO", label: "Confirmado" },
  { value: "REALIZADO", label: "Realizado" },
  { value: "CANCELADO", label: "Cancelado" },
  { value: "NAO_COMPARECEU", label: "Não compareceu" },
]

export function AgendamentosPage() {
  const [page, setPage] = useState(0)
  const [statusFilter, setStatusFilter] = useState<StatusAgendamento | "TODOS">("TODOS")

  const { data, isLoading } = useQuery({
    queryKey: ["agendamentos", page, statusFilter],
    queryFn: () =>
      getAgendamentos({
        page,
        size: PAGE_SIZE,
        sort: "dataHora,desc",
        status: statusFilter !== "TODOS" ? statusFilter : undefined,
      }),
  })

  return (
    <div className="space-y-5 animate-fade-in">
      <div>
        <h1 className="text-2xl font-bold font-primary text-foreground tracking-tight flex items-center gap-2">
          <Calendar className="h-6 w-6 text-primary" />
          Agendamentos
        </h1>
        <p className="text-sm text-muted-foreground font-secondary mt-0.5">
          {data ? `${data.totalElements} agendamentos` : "Carregando..."}
        </p>
      </div>

      <Card className="border border-border/60 shadow-soft">
        <CardHeader className="pb-3">
          <Select
            value={statusFilter}
            onValueChange={(v) => { setStatusFilter(v as StatusAgendamento | "TODOS"); setPage(0) }}
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
                    {["Paciente", "Profissional", "Serviço", "Data/Hora", "Duração", "Status"].map((h) => (
                      <TableHead key={h} className="text-xs font-semibold font-primary text-muted-foreground uppercase tracking-wide">
                        {h}
                      </TableHead>
                    ))}
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {data?.content.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={6} className="text-center text-muted-foreground font-secondary py-12">
                        Nenhum agendamento encontrado
                      </TableCell>
                    </TableRow>
                  ) : (
                    data?.content.map((ag) => {
                      const cfg = statusConfig[ag.status]
                      return (
                        <TableRow key={ag.id} className="border-border/40 hover:bg-muted/20">
                          <TableCell className="font-semibold font-primary text-sm text-foreground">
                            {ag.pacienteNome}
                          </TableCell>
                          <TableCell className="text-sm font-secondary text-muted-foreground">
                            {ag.profissionalNome}
                          </TableCell>
                          <TableCell className="text-sm font-secondary text-muted-foreground max-w-[140px] truncate">
                            {ag.servicoDescricao}
                          </TableCell>
                          <TableCell className="text-sm font-secondary text-muted-foreground whitespace-nowrap">
                            {format(new Date(ag.dataHora), "dd/MM/yyyy 'às' HH:mm", { locale: ptBR })}
                          </TableCell>
                          <TableCell className="text-sm font-secondary text-muted-foreground">
                            {ag.duracaoMinutos ? `${ag.duracaoMinutos} min` : "—"}
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
