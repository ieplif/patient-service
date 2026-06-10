import { useState } from "react"
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query"
import { CreditCard, Plus, Search, MoreHorizontal, CheckCircle, Ban, RotateCcw, Pencil } from "lucide-react"
import { format } from "date-fns"
import { getPagamentos, createPagamento, updatePagamentoStatus, updatePagamento, updateParcelaStatus } from "@/api/pagamentos"
import { shortenName } from "@/lib/names"
import type { StatusPagamento, FormaPagamento } from "@/types"
import { Skeleton } from "@/components/ui/skeleton"
import { Card, CardContent, CardHeader } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Badge } from "@/components/ui/badge"
import { useToast } from "@/hooks/use-toast"
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
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { Pagination } from "@/components/shared/Pagination"
import { PagamentoFormSheet } from "@/components/shared/PagamentoFormSheet"
import type { Pagamento, Parcela } from "@/types"

const PAGE_SIZE = 15

const statusConfig: Record<StatusPagamento, { label: string; className: string }> = {
  PENDENTE: { label: "Pendente", className: "bg-accent/15 text-accent border-accent/30" },
  PARCIALMENTE_PAGO: { label: "Parcial", className: "bg-secondary/40 text-[hsl(202,40%,40%)] border-secondary/50" },
  PAGO: { label: "Pago", className: "bg-primary/15 text-primary border-primary/30" },
  CANCELADO: { label: "Cancelado", className: "bg-destructive/10 text-destructive border-destructive/20" },
  REEMBOLSADO: { label: "Reembolsado", className: "bg-muted text-muted-foreground border-border" },
}

// Status da parcela (subset do StatusPagamento — uma parcela só tem PENDENTE/PAGO/CANCELADO)
const parcelaStatusConfig: Record<string, { label: string; className: string }> = {
  PENDENTE: { label: "Pendente", className: "bg-accent/15 text-accent border-accent/30" },
  PAGO: { label: "Pago", className: "bg-primary/15 text-primary border-primary/30" },
  CANCELADO: { label: "Cancelado", className: "bg-destructive/10 text-destructive border-destructive/20" },
}

const formaPagamentoLabel: Record<FormaPagamento, string> = {
  PIX: "Pix",
  CARTAO_CREDITO: "Cartao Credito",
  CARTAO_DEBITO: "Cartao Debito",
  DINHEIRO: "Dinheiro",
}

function formatCurrency(v: number) {
  return new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }).format(v)
}

const statusOptions: { value: StatusPagamento | "TODOS"; label: string }[] = [
  { value: "TODOS", label: "Todos os status" },
  { value: "PENDENTE", label: "Pendente" },
  { value: "PARCIALMENTE_PAGO", label: "Parcialmente pago" },
  { value: "PAGO", label: "Pago" },
  { value: "CANCELADO", label: "Cancelado" },
  { value: "REEMBOLSADO", label: "Reembolsado" },
]

export function PagamentosPage() {
  const [page, setPage] = useState(0)
  // Default "PENDENTE" — assim como Agendamentos abre em "Agendado", a recepção
  // quase sempre quer ver o que ainda está em aberto. Troque para "TODOS" no filtro.
  const [statusFilter, setStatusFilter] = useState<StatusPagamento | "TODOS">("PENDENTE")
  const [search, setSearch] = useState("")
  const [debouncedSearch, setDebouncedSearch] = useState("")
  const [sheetOpen, setSheetOpen] = useState(false)

  function handleSearch(value: string) {
    setSearch(value)
    clearTimeout((window as unknown as { _stPag?: ReturnType<typeof setTimeout> })._stPag)
    ;(window as unknown as { _stPag?: ReturnType<typeof setTimeout> })._stPag = setTimeout(() => {
      setDebouncedSearch(value)
      setPage(0)
    }, 400)
  }
  // Edição de pagamento — reutiliza o mesmo Sheet com initialData
  const [editAlvo, setEditAlvo] = useState<Pagamento | null>(null)
  const queryClient = useQueryClient()
  const { toast } = useToast()

  // Pendentes (e demais status): vencimento crescente — o que vence antes no topo,
  // que é o que a recepção precisa cobrar a seguir. Já em "Pago", o interesse é ver
  // os pagamentos mais recentes primeiro, ordenados pela data em que foram pagos.
  const sort = statusFilter === "PAGO" ? "dataPagamento,desc" : "dataVencimento,asc"

  const { data, isLoading } = useQuery({
    queryKey: ["pagamentos", page, statusFilter, debouncedSearch, sort],
    queryFn: () =>
      getPagamentos({
        page,
        size: PAGE_SIZE,
        sort,
        status: statusFilter !== "TODOS" ? statusFilter : undefined,
        pacienteNome: debouncedSearch || undefined,
      }),
  })

  const createMutation = useMutation({
    mutationFn: createPagamento,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["pagamentos"] })
      toast({ title: "Pagamento registrado", description: "O pagamento foi cadastrado com sucesso." })
      setSheetOpen(false)
    },
    onError: (err: unknown) => {
      const msg = (err as { response?: { data?: { mensagem?: string; message?: string } } })?.response?.data?.mensagem
        || (err as { response?: { data?: { message?: string } } })?.response?.data?.message
        || "Erro ao registrar pagamento."
      toast({ title: "Erro", description: msg, variant: "destructive" })
    },
  })

  const statusMutation = useMutation({
    mutationFn: ({ id, status, dataPagamento }:
      { id: string; status: StatusPagamento; dataPagamento?: string }) =>
      updatePagamentoStatus(id, status, dataPagamento),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["pagamentos"] })
      toast({ title: "Status atualizado", description: "O status do pagamento foi alterado." })
    },
    onError: (err: unknown) => {
      const msg = (err as { response?: { data?: { mensagem?: string; message?: string } } })?.response?.data?.mensagem
        || (err as { response?: { data?: { message?: string } } })?.response?.data?.message
        || "Erro ao alterar status."
      toast({ title: "Erro", description: msg, variant: "destructive" })
    },
  })

  const editMutation = useMutation({
    mutationFn: ({ id, ...payload }: {
      id: string
      pacienteId?: string
      assinaturaIds?: string[]
      agendamentoId?: string
      valor?: number
      formaPagamento?: FormaPagamento
      numeroParcelas?: number
      dataVencimento?: string
      observacoes?: string
    }) => updatePagamento(id, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["pagamentos"] })
      toast({ title: "Pagamento atualizado", description: "Os dados foram salvos." })
      setEditAlvo(null)
    },
    onError: (err: unknown) => {
      const msg = (err as { response?: { data?: { mensagem?: string; message?: string } } })?.response?.data?.mensagem
        || (err as { response?: { data?: { message?: string } } })?.response?.data?.message
        || "Erro ao atualizar pagamento."
      toast({ title: "Erro", description: msg, variant: "destructive" })
    },
  })

  const parcelaMutation = useMutation({
    mutationFn: ({ pagamentoId, parcelaId, dataPagamento }:
      { pagamentoId: string; parcelaId: string; dataPagamento?: string }) =>
      updateParcelaStatus(pagamentoId, parcelaId, "PAGO", dataPagamento),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["pagamentos"] })
      toast({ title: "Parcela paga", description: "A parcela foi marcada como paga." })
    },
    onError: (err: unknown) => {
      const msg = (err as { response?: { data?: { mensagem?: string; message?: string } } })?.response?.data?.mensagem
        || (err as { response?: { data?: { message?: string } } })?.response?.data?.message
        || "Erro ao marcar parcela como paga."
      toast({ title: "Erro", description: msg, variant: "destructive" })
    },
  })

  // Pergunta a data do pagamento ao marcar como PAGO (permite registrar pagamento retroativo)
  function marcarComoPago(id: string) {
    const hoje = new Date().toISOString().split("T")[0]
    const dataPagamento = window.prompt(
      "Data do pagamento (YYYY-MM-DD). Deixe em branco para usar hoje:",
      hoje
    )
    if (dataPagamento === null) return  // cancelado
    statusMutation.mutate({
      id,
      status: "PAGO",
      dataPagamento: dataPagamento.trim() || undefined,
    })
  }

  function marcarParcelaComoPaga(pagamentoId: string, parcelaId: string) {
    const hoje = new Date().toISOString().split("T")[0]
    const dataPagamento = window.prompt(
      "Data do pagamento desta parcela (YYYY-MM-DD). Deixe em branco para usar hoje:",
      hoje
    )
    if (dataPagamento === null) return
    parcelaMutation.mutate({
      pagamentoId,
      parcelaId,
      dataPagamento: dataPagamento.trim() ? `${dataPagamento.trim()}T00:00:00` : undefined,
    })
  }

  // Achata pagamentos em parcelas — cada parcela vira uma linha na lista.
  // Para pagamentos com 1 parcela, mostra a linha "tradicional" (com ações de cancelar/reembolsar do pai).
  // Para pagamentos com N>1 parcelas, mostra N linhas independentes que podem ser pagas separadamente.
  type Linha = { pag: Pagamento; parc: Parcela | null }
  const linhas: Linha[] = (data?.content ?? []).flatMap<Linha>((pag) =>
    pag.parcelas.length > 0
      ? pag.parcelas.map<Linha>((parc) => ({ pag, parc }))
      : [{ pag, parc: null }]
  )

  return (
    <div className="space-y-5 animate-fade-in">
      <div className="flex flex-col sm:flex-row items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold font-primary text-foreground tracking-tight flex items-center gap-2">
            <CreditCard className="h-6 w-6 text-primary" />
            Pagamentos
          </h1>
          <p className="text-sm text-muted-foreground font-secondary mt-0.5">
            {data ? `${data.totalElements} pagamentos` : "Carregando..."}
          </p>
        </div>
        <Button className="bg-primary text-primary-foreground font-primary" onClick={() => setSheetOpen(true)}>
          <Plus className="h-4 w-4 mr-2" /> Novo Pagamento
        </Button>
      </div>

      <Card className="border border-border/60 shadow-soft">
        <CardHeader className="pb-3">
          <div className="flex flex-col sm:flex-row gap-3">
            <Select
              value={statusFilter}
              onValueChange={(v) => { setStatusFilter(v as StatusPagamento | "TODOS"); setPage(0) }}
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
            <div className="relative flex-1">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
              <Input
                placeholder="Buscar por paciente..."
                value={search}
                onChange={(e) => handleSearch(e.target.value)}
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
                      {["Paciente", "Valor", "Forma", "Vencimento", "Parcela", "Status", ""].map((h) => (
                      <TableHead key={h || "actions"} className="text-xs font-semibold font-primary text-muted-foreground uppercase tracking-wide">
                        {h}
                      </TableHead>
                    ))}
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {linhas.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={7} className="text-center text-muted-foreground font-secondary py-12">
                        Nenhum pagamento encontrado
                      </TableCell>
                    </TableRow>
                  ) : (
                    linhas.map(({ pag, parc }) => {
                      // Quando há parcela, a linha representa essa parcela individual.
                      // Caso contrário (raro: pagamento sem parcelas), a linha representa o próprio pagamento.
                      const isParcela = !!parc
                      const valor = isParcela ? parc!.valor : pag.valor
                      const venc = isParcela ? parc!.dataVencimento : pag.dataVencimento
                      const status = isParcela ? parc!.status : pag.status
                      const cfg = parcelaStatusConfig[status] ?? statusConfig[status as StatusPagamento]
                      const rowKey = isParcela ? `${pag.id}-${parc!.id}` : pag.id
                      const parcelaLabel = isParcela && pag.numeroParcelas > 1
                        ? `${parc!.numero}/${pag.numeroParcelas}`
                        : "1x"
                      // O pagamento pai está cancelado/reembolsado → bloqueia ações na parcela
                      const paiBloqueado = pag.status === "CANCELADO" || pag.status === "REEMBOLSADO"

                      return (
                        <TableRow key={rowKey} className="border-border/40 hover:bg-muted/20">
                          <TableCell className="font-semibold font-primary text-sm text-foreground" title={pag.pacienteNome}>
                            {shortenName(pag.pacienteNome)}
                          </TableCell>
                          <TableCell className="text-sm font-secondary font-semibold text-accent">
                            {formatCurrency(valor)}
                          </TableCell>
                          <TableCell>
                            <Badge variant="outline" className="text-xs font-primary border-border/60 text-muted-foreground">
                              {formaPagamentoLabel[pag.formaPagamento] ?? pag.formaPagamento}
                            </Badge>
                          </TableCell>
                          <TableCell className="text-sm font-secondary text-muted-foreground">
                            {format(new Date(venc), "dd/MM/yyyy")}
                          </TableCell>
                          <TableCell className="text-sm font-secondary text-muted-foreground text-center">
                            {parcelaLabel}
                          </TableCell>
                          <TableCell>
                            <span className={`inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-semibold font-primary ${cfg?.className ?? ""}`}>
                              {cfg?.label ?? status}
                            </span>
                          </TableCell>
                          <TableCell>
                            <DropdownMenu>
                              <DropdownMenuTrigger asChild>
                                <Button variant="ghost" size="icon" className="h-8 w-8">
                                  <MoreHorizontal className="h-4 w-4" />
                                </Button>
                              </DropdownMenuTrigger>
                              <DropdownMenuContent align="end" className="font-secondary">
                                {/* Editar sempre opera no PAGAMENTO PAI (afeta todas as parcelas) */}
                                <DropdownMenuItem onClick={() => setEditAlvo(pag)}>
                                  <Pencil className="h-4 w-4 mr-2" /> Editar pagamento
                                </DropdownMenuItem>

                                {/* Marcar parcela como paga — só faz sentido se está pendente e o pai não está bloqueado */}
                                {isParcela && status === "PENDENTE" && !paiBloqueado && (
                                  <DropdownMenuItem onClick={() => marcarParcelaComoPaga(pag.id, parc!.id)}>
                                    <CheckCircle className="h-4 w-4 mr-2" /> Marcar parcela como Paga
                                  </DropdownMenuItem>
                                )}

                                {/* Fallback: pagamento sem parcelas (legado) */}
                                {!isParcela && status === "PENDENTE" && (
                                  <DropdownMenuItem onClick={() => marcarComoPago(pag.id)}>
                                    <CheckCircle className="h-4 w-4 mr-2" /> Marcar como Pago
                                  </DropdownMenuItem>
                                )}

                                {/* Cancelar / Reembolsar — sempre opera no pagamento PAI inteiro */}
                                {pag.status === "PENDENTE" || pag.status === "PARCIALMENTE_PAGO" ? (
                                  <DropdownMenuItem
                                    className="text-destructive"
                                    onClick={() => statusMutation.mutate({ id: pag.id, status: "CANCELADO" })}
                                  >
                                    <Ban className="h-4 w-4 mr-2" /> Cancelar pagamento
                                  </DropdownMenuItem>
                                ) : null}
                                {pag.status === "PAGO" && (
                                  <DropdownMenuItem onClick={() => statusMutation.mutate({ id: pag.id, status: "REEMBOLSADO" })}>
                                    <RotateCcw className="h-4 w-4 mr-2" /> Reembolsar pagamento
                                  </DropdownMenuItem>
                                )}
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

      <PagamentoFormSheet
        open={sheetOpen}
        onOpenChange={setSheetOpen}
        onSubmit={(formData) => createMutation.mutate(formData)}
        isPending={createMutation.isPending}
      />

      <PagamentoFormSheet
        open={!!editAlvo}
        onOpenChange={(open) => { if (!open) setEditAlvo(null) }}
        initialData={editAlvo}
        onSubmit={(formData) => {
          if (!editAlvo) return
          editMutation.mutate({ id: editAlvo.id, ...formData })
        }}
        isPending={editMutation.isPending}
      />
    </div>
  )
}
