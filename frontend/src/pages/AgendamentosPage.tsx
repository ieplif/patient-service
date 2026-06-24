import { useState } from "react"
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query"
import { Calendar, Plus, Clock, MoreHorizontal, Search, RefreshCw } from "lucide-react"
import { format } from "date-fns"
import { ptBR } from "date-fns/locale"
import { getAgendamentos, updateAgendamentoStatus } from "@/api/agendamentos"
import type { Agendamento, StatusAgendamento } from "@/types"
import { Skeleton } from "@/components/ui/skeleton"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
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
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from "@/components/ui/dialog"
import { Pagination } from "@/components/shared/Pagination"
import { AgendamentoFormSheet } from "@/components/shared/AgendamentoFormSheet"
import { ReposicaoFormSheet } from "@/components/shared/ReposicaoFormSheet"
import { useToast } from "@/hooks/use-toast"
import { shortenName } from "@/lib/names"

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
  AGENDADO:   [
    { status: "CONFIRMADO", label: "Confirmar" },
    { status: "REALIZADO",  label: "Marcar como realizado" },
    { status: "CANCELADO",  label: "Cancelar" },
    { status: "NAO_COMPARECEU", label: "Não compareceu" },
  ],
  CONFIRMADO: [
    { status: "AGENDADO",   label: "Reverter para Agendado" },
    { status: "REALIZADO",  label: "Marcar como realizado" },
    { status: "CANCELADO",  label: "Cancelar" },
    { status: "NAO_COMPARECEU", label: "Não compareceu" },
  ],
  // Os 3 status "finais" agora podem ser revertidos para corrigir engano da recepção
  REALIZADO: [
    { status: "AGENDADO",       label: "Reverter para Agendado" },
    { status: "CONFIRMADO",     label: "Reverter para Confirmado" },
    { status: "NAO_COMPARECEU", label: "Marcar como não compareceu" },
    { status: "CANCELADO",      label: "Cancelar" },
  ],
  CANCELADO: [
    { status: "AGENDADO",   label: "Reverter para Agendado" },
    { status: "CONFIRMADO", label: "Reverter para Confirmado" },
    { status: "REALIZADO",  label: "Marcar como realizado" },
  ],
  NAO_COMPARECEU: [
    { status: "AGENDADO",   label: "Reverter para Agendado" },
    { status: "REALIZADO",  label: "Marcar como realizado" },
    { status: "CANCELADO",  label: "Cancelar" },
  ],
}

export function AgendamentosPage() {
  const [page, setPage] = useState(0)
  // Default "AGENDADO" — recepção quase sempre quer ver o que ainda está em aberto.
  // Pode trocar para "TODOS" pelo filtro acima da tabela.
  const [statusFilter, setStatusFilter] = useState<StatusAgendamento | "TODOS">("AGENDADO")
  const [search, setSearch] = useState("")
  const [debouncedSearch, setDebouncedSearch] = useState("")
  const [sheetOpen, setSheetOpen] = useState(false)

  function handleSearch(value: string) {
    setSearch(value)
    clearTimeout((window as unknown as { _stAg?: ReturnType<typeof setTimeout> })._stAg)
    ;(window as unknown as { _stAg?: ReturnType<typeof setTimeout> })._stAg = setTimeout(() => {
      setDebouncedSearch(value)
      setPage(0)
    }, 400)
  }
  const [selectedAg, setSelectedAg] = useState<Agendamento | null>(null)
  const [reposicaoSheetOpen, setReposicaoSheetOpen] = useState(false)
  const [reposicaoAg, setReposicaoAg] = useState<Agendamento | null>(null)
  const [cancelDialogOpen, setCancelDialogOpen] = useState(false)
  const [cancelAg, setCancelAg] = useState<Agendamento | null>(null)
  const [motivoCancelamento, setMotivoCancelamento] = useState("")
  const [gerarReposicao, setGerarReposicao] = useState(true)
  const { toast } = useToast()
  const queryClient = useQueryClient()

  // "Realizado" lista o histórico, então o interesse é ver o mais recente primeiro
  // (decrescente). Nos demais status (Agendado etc.) a ordem é crescente — o
  // próximo a acontecer no topo.
  const sort = statusFilter === "REALIZADO" ? "dataHora,desc" : "dataHora,asc"

  const { data, isLoading } = useQuery({
    queryKey: ["agendamentos", page, statusFilter, debouncedSearch, sort],
    queryFn: () =>
      getAgendamentos({
        page,
        size: PAGE_SIZE,
        sort,
        status: statusFilter !== "TODOS" ? statusFilter : undefined,
        pacienteNome: debouncedSearch || undefined,
      }),
  })

  const statusMutation = useMutation({
    mutationFn: ({ id, status, motivoCancelamento: motivo, gerarReposicao: rep }:
      { id: string; status: StatusAgendamento; motivoCancelamento?: string; gerarReposicao?: boolean }) =>
      updateAgendamentoStatus(id, status, motivo, rep),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["agendamentos"] })
      toast({ title: "Status atualizado", description: "O status do agendamento foi atualizado." })
      setCancelDialogOpen(false)
      setCancelAg(null)
      setMotivoCancelamento("")
      setGerarReposicao(true)
    },
    onError: (err: unknown) => {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message || "Erro ao atualizar status."
      toast({ title: "Erro", description: msg, variant: "destructive" })
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

  function handleCancelar(ag: Agendamento) {
    setCancelAg(ag)
    setMotivoCancelamento("")
    setGerarReposicao(true)  // default checked para todos os serviços
    setCancelDialogOpen(true)
  }

  function handleConfirmCancel() {
    if (!cancelAg) return
    statusMutation.mutate({
      id: cancelAg.id,
      status: "CANCELADO",
      motivoCancelamento: motivoCancelamento || undefined,
      gerarReposicao,
    })
  }

  function handleAgendarReposicao(ag: Agendamento) {
    setReposicaoAg(ag)
    setReposicaoSheetOpen(true)
  }

  function isFisioterapia(ag: Agendamento): boolean {
    return ag.servicoDescricao.toLowerCase().includes("fisio")
  }

  function isAbdomen360(ag: Agendamento): boolean {
    return ag.servicoDescricao.toLowerCase().includes("abdômen 360")
      || ag.servicoDescricao.toLowerCase().includes("abdomen 360")
  }

  // Começa com "drenagem" para casar só a atividade "Drenagem", sem pegar o combo
  // "Pacote Pilates, Fisio e Drenagem" (que cai no destaque de Fisioterapia).
  function isDrenagem(ag: Agendamento): boolean {
    return ag.servicoDescricao.trim().toLowerCase().startsWith("drenagem")
  }

  // Busca por paciente agora é server-side (parâmetro pacienteNome) — cobre todas as páginas
  const filteredContent = data?.content

  return (
    <div className="space-y-5 animate-fade-in">
      <div className="flex flex-col sm:flex-row items-start sm:items-center gap-4">
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
          <div className="flex flex-col sm:flex-row gap-3">
            <Select
              value={statusFilter}
              onValueChange={(v) => { setStatusFilter(v as StatusAgendamento | "TODOS"); setPage(0) }}
            >
              <SelectTrigger className="w-full sm:w-52 bg-background border-border/70 font-secondary text-sm">
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
                onChange={(e) => handleSearch(e.target.value)}
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
              <div className="overflow-x-auto">
                <Table>
                  <TableHeader>
                    <TableRow className="border-border/50 hover:bg-transparent">
                      {["Paciente", "Serviço", "Data/Hora", "Status", ""].map((h, i) => (
                      <TableHead key={i} className="text-xs font-semibold font-primary text-muted-foreground uppercase tracking-wide">
                        {h}
                      </TableHead>
                    ))}
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {!filteredContent?.length ? (
                    <TableRow>
                      <TableCell colSpan={5} className="text-center text-muted-foreground font-secondary py-12">
                        Nenhum agendamento encontrado
                      </TableCell>
                    </TableRow>
                  ) : (
                    filteredContent.map((ag) => {
                      const cfg = statusConfig[ag.status]
                      const transitions = nextStatuses[ag.status] ?? []
                      const fisio = isFisioterapia(ag)
                      const abdomen = isAbdomen360(ag)
                      const drenagem = isDrenagem(ag)
                      // Prioridade visual: Abdômen 360° (violeta) > Drenagem (teal) > Fisioterapia (earth) > default
                      const rowClass = abdomen
                        ? "border-border/40 bg-violet-50 border-l-4 border-l-violet-400 hover:bg-violet-100"
                        : drenagem
                          ? "border-border/40 bg-teal-50 border-l-4 border-l-teal-400 hover:bg-teal-100"
                          : fisio
                            ? "border-border/40 bg-[#B47C64]/10 border-l-4 border-l-[#B47C64] hover:bg-[#B47C64]/20"
                            : "border-border/40 hover:bg-muted/20"
                      return (
                        <TableRow key={ag.id} className={rowClass}>
                          <TableCell className="font-semibold font-primary text-sm text-foreground" title={ag.pacienteNome}>
                            {shortenName(ag.pacienteNome)}
                          </TableCell>
                          <TableCell className="text-sm font-secondary text-muted-foreground max-w-[140px] truncate" title={ag.servicoDescricao}>
                            {ag.servicoDescricao}
                          </TableCell>
                          <TableCell className="text-sm font-secondary text-muted-foreground whitespace-nowrap">
                            {format(new Date(ag.dataHora), "dd/MM/yyyy 'às' HH:mm", { locale: ptBR })}
                          </TableCell>
                          <TableCell>
                            <div className="flex flex-wrap items-center gap-1">
                              <span className={`inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-semibold font-primary ${cfg.className}`}>
                                {cfg.label}
                              </span>
                              {ag.tipoAgendamento === "REPOSICAO" && (
                                <span className="inline-flex items-center rounded-full border border-violet-300 bg-violet-100 text-violet-700 px-2 py-0.5 text-xs font-medium font-primary">
                                  Reposicao
                                </span>
                              )}
                              {ag.status === "CANCELADO" && ag.direitoReposicao === true && (
                                ag.reposicaoAgendada ? (
                                  <span className="inline-flex items-center rounded-full border border-border bg-muted text-muted-foreground px-2 py-0.5 text-xs font-medium font-primary">
                                    Reposicao feita
                                  </span>
                                ) : (
                                  <span className="inline-flex items-center rounded-full border border-emerald-300 bg-emerald-50 text-emerald-700 px-2 py-0.5 text-xs font-medium font-primary">
                                    Direito a reposicao
                                  </span>
                                )
                              )}
                            </div>
                          </TableCell>
                          <TableCell className="text-right">
                            <DropdownMenu>
                              <DropdownMenuTrigger asChild>
                                <Button size="icon" variant="ghost" className="h-8 w-8 text-muted-foreground hover:text-foreground">
                                  <MoreHorizontal className="h-4 w-4" />
                                </Button>
                              </DropdownMenuTrigger>
                              <DropdownMenuContent align="end" className="font-secondary text-sm">
                                {ag.status !== "CANCELADO" && (
                                  <DropdownMenuItem onClick={() => handleReagendar(ag)} className="gap-2">
                                    <Clock className="h-4 w-4" /> Reagendar
                                  </DropdownMenuItem>
                                )}
                                {ag.status === "CANCELADO" && ag.direitoReposicao === true && !ag.reposicaoAgendada && (
                                  <DropdownMenuItem onClick={() => handleAgendarReposicao(ag)} className="gap-2 text-violet-600 focus:text-violet-600">
                                    <RefreshCw className="h-4 w-4" /> Agendar Reposicao
                                  </DropdownMenuItem>
                                )}
                                {transitions.length > 0 && <DropdownMenuSeparator />}
                                {transitions.map((t) => (
                                  <DropdownMenuItem
                                    key={t.status}
                                    onClick={() => {
                                      if (t.status === "CANCELADO") {
                                        handleCancelar(ag)
                                      } else {
                                        statusMutation.mutate({ id: ag.id, status: t.status })
                                      }
                                    }}
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
              </div>
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

      <ReposicaoFormSheet
        open={reposicaoSheetOpen}
        onOpenChange={setReposicaoSheetOpen}
        agendamento={reposicaoAg}
      />

      <Dialog open={cancelDialogOpen} onOpenChange={setCancelDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle className="font-primary">Cancelar agendamento</DialogTitle>
            <DialogDescription className="font-secondary">
              {cancelAg ? `Cancelar agendamento de ${cancelAg.pacienteNome}?` : ""}
            </DialogDescription>
          </DialogHeader>

          {cancelAg && (
            <div className="flex items-start gap-2 rounded-md border border-border/50 bg-muted/30 p-3">
              <input
                id="gerar-reposicao"
                type="checkbox"
                checked={gerarReposicao}
                onChange={(e) => setGerarReposicao(e.target.checked)}
                className="mt-0.5 h-4 w-4 rounded border-gray-300 text-primary focus:ring-primary"
              />
              <div className="flex-1">
                <Label htmlFor="gerar-reposicao" className="font-primary text-sm cursor-pointer">
                  Gerar direito a reposição
                </Label>
                <p className="text-xs text-muted-foreground font-secondary mt-0.5">
                  Quando marcado, a paciente poderá agendar uma reposição desta sessão.
                </p>
              </div>
            </div>
          )}

          <div className="space-y-1.5">
            <Label className="font-primary text-sm">Motivo do cancelamento (opcional)</Label>
            <textarea
              value={motivoCancelamento}
              onChange={(e) => setMotivoCancelamento(e.target.value)}
              placeholder="Informe o motivo do cancelamento..."
              className="flex min-h-[80px] w-full rounded-md border border-input bg-background px-3 py-2 text-sm font-secondary ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
            />
          </div>

          <DialogFooter>
            <Button variant="outline" className="font-primary" onClick={() => setCancelDialogOpen(false)}>
              Voltar
            </Button>
            <Button
              variant="destructive"
              className="font-primary"
              onClick={handleConfirmCancel}
              disabled={statusMutation.isPending}
            >
              {statusMutation.isPending ? "Cancelando..." : "Confirmar cancelamento"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
