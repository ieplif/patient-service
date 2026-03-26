import { useState } from "react"
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query"
import { CreditCard, Plus, Search, MoreHorizontal, CheckCircle, Ban, RotateCcw } from "lucide-react"
import { format } from "date-fns"
import { getPagamentos, createPagamento, updatePagamentoStatus } from "@/api/pagamentos"
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

const PAGE_SIZE = 15

const statusConfig: Record<StatusPagamento, { label: string; className: string }> = {
  PENDENTE: { label: "Pendente", className: "bg-accent/15 text-accent border-accent/30" },
  PARCIALMENTE_PAGO: { label: "Parcial", className: "bg-secondary/40 text-[hsl(202,40%,40%)] border-secondary/50" },
  PAGO: { label: "Pago", className: "bg-primary/15 text-primary border-primary/30" },
  CANCELADO: { label: "Cancelado", className: "bg-destructive/10 text-destructive border-destructive/20" },
  REEMBOLSADO: { label: "Reembolsado", className: "bg-muted text-muted-foreground border-border" },
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
  const [statusFilter, setStatusFilter] = useState<StatusPagamento | "TODOS">("TODOS")
  const [search, setSearch] = useState("")
  const [sheetOpen, setSheetOpen] = useState(false)
  const queryClient = useQueryClient()
  const { toast } = useToast()

  const { data, isLoading } = useQuery({
    queryKey: ["pagamentos", page, statusFilter],
    queryFn: () =>
      getPagamentos({
        page,
        size: PAGE_SIZE,
        sort: "createdAt,desc",
        status: statusFilter !== "TODOS" ? statusFilter : undefined,
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
    mutationFn: ({ id, status }: { id: string; status: StatusPagamento }) =>
      updatePagamentoStatus(id, status),
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

  const filtered = data?.content.filter((pag) =>
    !search || pag.pacienteNome.toLowerCase().includes(search.toLowerCase())
  )

  return (
    <div className="space-y-5 animate-fade-in">
      <div className="flex items-start justify-between">
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
          <div className="flex gap-3">
            <Select
              value={statusFilter}
              onValueChange={(v) => { setStatusFilter(v as StatusPagamento | "TODOS"); setPage(0) }}
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
            <div className="relative flex-1">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
              <Input
                placeholder="Buscar por paciente..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
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
                    {["Paciente", "Valor", "Forma", "Vencimento", "Parcelas", "Status", ""].map((h) => (
                      <TableHead key={h || "actions"} className="text-xs font-semibold font-primary text-muted-foreground uppercase tracking-wide">
                        {h}
                      </TableHead>
                    ))}
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {!filtered || filtered.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={7} className="text-center text-muted-foreground font-secondary py-12">
                        Nenhum pagamento encontrado
                      </TableCell>
                    </TableRow>
                  ) : (
                    filtered.map((pag) => {
                      const cfg = statusConfig[pag.status]
                      return (
                        <TableRow key={pag.id} className="border-border/40 hover:bg-muted/20">
                          <TableCell className="font-semibold font-primary text-sm text-foreground">
                            {pag.pacienteNome}
                          </TableCell>
                          <TableCell className="text-sm font-secondary font-semibold text-accent">
                            {formatCurrency(pag.valor)}
                          </TableCell>
                          <TableCell>
                            <Badge variant="outline" className="text-xs font-primary border-border/60 text-muted-foreground">
                              {formaPagamentoLabel[pag.formaPagamento] ?? pag.formaPagamento}
                            </Badge>
                          </TableCell>
                          <TableCell className="text-sm font-secondary text-muted-foreground">
                            {format(new Date(pag.dataVencimento), "dd/MM/yyyy")}
                          </TableCell>
                          <TableCell className="text-sm font-secondary text-muted-foreground text-center">
                            {pag.numeroParcelas}x
                          </TableCell>
                          <TableCell>
                            <span className={`inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-semibold font-primary ${cfg.className}`}>
                              {cfg.label}
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
                                {pag.status === "PENDENTE" && (
                                  <>
                                    <DropdownMenuItem onClick={() => statusMutation.mutate({ id: pag.id, status: "PAGO" })}>
                                      <CheckCircle className="h-4 w-4 mr-2" /> Marcar como Pago
                                    </DropdownMenuItem>
                                    <DropdownMenuItem
                                      className="text-destructive"
                                      onClick={() => statusMutation.mutate({ id: pag.id, status: "CANCELADO" })}
                                    >
                                      <Ban className="h-4 w-4 mr-2" /> Cancelar
                                    </DropdownMenuItem>
                                  </>
                                )}
                                {pag.status === "PAGO" && (
                                  <DropdownMenuItem onClick={() => statusMutation.mutate({ id: pag.id, status: "REEMBOLSADO" })}>
                                    <RotateCcw className="h-4 w-4 mr-2" /> Reembolsar
                                  </DropdownMenuItem>
                                )}
                                {pag.status === "PARCIALMENTE_PAGO" && (
                                  <>
                                    <DropdownMenuItem onClick={() => statusMutation.mutate({ id: pag.id, status: "PAGO" })}>
                                      <CheckCircle className="h-4 w-4 mr-2" /> Marcar como Pago
                                    </DropdownMenuItem>
                                    <DropdownMenuItem
                                      className="text-destructive"
                                      onClick={() => statusMutation.mutate({ id: pag.id, status: "CANCELADO" })}
                                    >
                                      <Ban className="h-4 w-4 mr-2" /> Cancelar
                                    </DropdownMenuItem>
                                  </>
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
    </div>
  )
}
