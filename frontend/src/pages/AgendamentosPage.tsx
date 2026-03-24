import { useState } from "react"
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query"
import { Calendar, Plus, Clock, MoreHorizontal, Search } from "lucide-react"
import { format } from "date-fns"
import { ptBR } from "date-fns/locale"
import { getAgendamentos, updateAgendamentoStatus, deleteAgendamento } from "@/api/agendamentos"
import type { Agendamento, StatusAgendamento } from "@/types"
import { Skeleton } from "@/components/ui/skeleton"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
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
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { Pagination } from "@/components/shared/Pagination"
import { AgendamentoFormSheet } from "@/components/shared/AgendamentoFormSheet"
import { useToast } from "@/hooks/use-toast"

const PAGE_SIZE = 15

const statusConfig: Record<StatusAgendamento, { label: string; className: string }> = {
  AGENDADO:       { label: "Agendado",        className: "bg-secondary/40 text-[hsl(202,40%,40%)] border-secondary/50" },
  CONFIRMADO:     { label: "Confirmado",      className: "bg-primary/15 text-primary border-primary/30" },
  REALIZADO:      { label: "Realizado",       className: "bg-muted text-muted-foreground border-border" },
  CANCELADO:      { label: "Cancelado",       className: "bg-destructive/10 text-destructive border-destructive/20" },
  NAO_COMPARECEU: { label: "Não compareceu",  className: "bg-accent/15 text-accent border-accent/30" },
}

const statusOptions: { value: StatusAgendamento | "TODOS"; label: string }[] = [
  { value: "TODOS",          label: "Todos os status" },
  { value: "AGENDADO",       label: "Agendado" },
  { value: "CONFIRMADO",     label: "Confirmado" },
  { value: "REALIZADO",      label: "Realizado" },
  { value: "CANCELADO",      label: "Cancelado" },
  { value: "NAO_COMPARECEU", label: "Não compareceu" },
]

// Transições permitidas por status
const nextStatuses: Partial<Record<StatusAgendamento, { status: StatusAgendamento; label: string }[]>> = {
  AGENDADO:   [{ status: "CONFIRMADO", label: "Confirmar" }, { status: "CANCELADO", label: "Cancelar" }, { status: "NAO_COMPARECEU", label: "Não compareceu" }],
  CONFIRMADO: [{ status: "REALIZADO",  label: "Marcar como realizado" }, { status: "CANCELADO", label: "Cancelar" }, { status: "NAO_COMPARECEU", label: "Não compareceu" }],
}

export function AgendamentosPage() {
  const [page, setPage] = useState(0)
  const [statusFilter, setStatusFilter] = useState<StatusAgendamento | "TODOS">("TODOS")
  const [search, setSearch] = useState("")
  const [sheetOpen, setSheetOpen] = useState(false)
  const [selectedAg, setSelectedAg] = useState<Agendamento | null>(null)
  const { toast } = useToast()
  const queryClient = useQueryClient()

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

  const statusMutation = useMutation({
    mutationFn: ({ id, status }: { id: string; status: StatusAgendamento }) =>
      updateAgendamentoStatus(id, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["agendamentos"] })
      toast({ title: "Status atualizado", description: "O status do agendamento foi atualizado." })
    },
    onError: (err: unknown) => {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message || "Erro ao atualizar status."
      toast({ title: "Erro", description: msg, variant: "destructive" })
    },
  })

  const deleteMutation = useMutation({
    mutationFn: deleteAgendamento,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["agendamentos"] })
      toast({ title: "Agendamento cancelado", description: "O agendamento foi removido." })
    },
  })

  function handleNew() {
    setSelectedAg(null)
    setSheetOpen(true)
  }

  function handleReagendar(ag: Agendamento) {
    setSelectedAg(ag)
    setSheetOpen(true)
  }

  const filteredContent = search.trim()
    ? data?.content.filter((ag) =>
        ag.pacienteNome.toLowerCase().includes(search.toLowerCase())
      )
    : data?.content

  return (
    <div className="space-y-5 animate-fade-in">
      <div className="flex items-start justify-between">
        <div>
          <h1 className="text-2xl font-bold font-primary text-foreground tracking-tight flex items-center gap-2">
            <Calendar className="h-6 w-6 text-primary" />
            Agendamentos
          </h1>
          <p className="text-sm text-muted-foreground font-secondary mt-0.5">
            {data ? `${data.totalElements} agendamentos` : "Carregando..."}
          </p>
        </div>
        <Button onClick={handleNew} className="font-primary gap-2">
          <Plus className="h-4 w-4" />
          Novo Agendamento
        </Button>
      </div>

      <Card className="border border-border/60 shadow-soft">
        <CardHeader className="pb-3">
          <div className="flex gap-3 flex-wrap">
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

            <div className="relative flex-1 min-w-[200px]">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
              <Input
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                placeholder="Buscar por paciente..."
                className="pl-9 font-secondary text-sm"
              />
            </div>
          </div>
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
                    {["Paciente", "Profissional", "Serviço", "Data/Hora", "Duração", "Status", ""].map((h, i) => (
                      <TableHead key={i} className="text-xs font-semibold font-primary text-muted-foreground uppercase tracking-wide">
                        {h}
                      </TableHead>
                    ))}
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {!filteredContent?.length ? (
                    <TableRow>
                      <TableCell colSpan={7} className="text-center text-muted-foreground font-secondary py-12">
                        Nenhum agendamento encontrado
                      </TableCell>
                    </TableRow>
                  ) : (
                    filteredContent.map((ag) => {
                      const cfg = statusConfig[ag.status]
                      const transitions = nextStatuses[ag.status] ?? []
                      return (
                        <TableRow key={ag.id} className="border-border/40 hover:bg-muted/20">
                          <TableCell className="font-semibold font-primary text-sm text-foreground">
                            {ag.pacienteNome}
                          </TableCell>
                          <TableCell className="text-sm font-secondary text-muted-foreground">
                            {ag.profissionalNome}
                          </TableCell>
                          <TableCell className="text-sm font-secondary text-muted-foreground max-w-[140px] truncate" title={ag.servicoDescricao}>
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
                          <TableCell className="text-right">
                            <DropdownMenu>
                              <DropdownMenuTrigger asChild>
                                <Button size="icon" variant="ghost" className="h-8 w-8 text-muted-foreground hover:text-foreground">
                                  <MoreHorizontal className="h-4 w-4" />
                                </Button>
                              </DropdownMenuTrigger>
                              <DropdownMenuContent align="end" className="font-secondary text-sm">
                                <DropdownMenuItem onClick={() => handleReagendar(ag)} className="gap-2">
                                  <Clock className="h-4 w-4" /> Reagendar
                                </DropdownMenuItem>
                                {transitions.length > 0 && <DropdownMenuSeparator />}
                                {transitions.map((t) => (
                                  <DropdownMenuItem
                                    key={t.status}
                                    onClick={() => statusMutation.mutate({ id: ag.id, status: t.status })}
                                    className={t.status === "CANCELADO" ? "text-destructive focus:text-destructive" : ""}
                                  >
                                    {t.label}
                                  </DropdownMenuItem>
                                ))}
                              </DropdownMenuContent>
                            </DropdownMenu>
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

      <AgendamentoFormSheet
        open={sheetOpen}
        onOpenChange={setSheetOpen}
        agendamento={selectedAg}
      />
    </div>
  )
}
