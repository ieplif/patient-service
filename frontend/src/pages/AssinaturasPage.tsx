import { useState } from "react"
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query"
import { Star, Plus, Pencil, Ban, MoreHorizontal, Search } from "lucide-react"
import { format } from "date-fns"
import { getAssinaturas, createAssinatura, updateAssinatura, updateAssinaturaStatus } from "@/api/assinaturas"
import type { StatusAssinatura, Assinatura } from "@/types"
import { Skeleton } from "@/components/ui/skeleton"
import { Card, CardContent, CardHeader } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
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
import { AssinaturaFormSheet } from "@/components/shared/AssinaturaFormSheet"

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
  const [search, setSearch] = useState("")
  const [sheetOpen, setSheetOpen] = useState(false)
  const [editAssinatura, setEditAssinatura] = useState<Assinatura | null>(null)
  const queryClient = useQueryClient()
  const { toast } = useToast()

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

  const createMutation = useMutation({
    mutationFn: createAssinatura,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["assinaturas"] })
      toast({ title: "Assinatura criada", description: "Nova assinatura cadastrada com sucesso." })
      setSheetOpen(false)
    },
    onError: (err: unknown) => {
      const msg = (err as { response?: { data?: { mensagem?: string; message?: string } } })?.response?.data?.mensagem
        || (err as { response?: { data?: { message?: string } } })?.response?.data?.message
        || "Erro ao criar assinatura."
      toast({ title: "Erro", description: msg, variant: "destructive" })
    },
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, ...payload }: { id: string } & Parameters<typeof updateAssinatura>[1]) =>
      updateAssinatura(id, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["assinaturas"] })
      toast({ title: "Assinatura atualizada", description: "Os dados foram salvos." })
      setSheetOpen(false)
      setEditAssinatura(null)
    },
    onError: (err: unknown) => {
      const msg = (err as { response?: { data?: { mensagem?: string; message?: string } } })?.response?.data?.mensagem
        || (err as { response?: { data?: { message?: string } } })?.response?.data?.message
        || "Erro ao atualizar assinatura."
      toast({ title: "Erro", description: msg, variant: "destructive" })
    },
  })

  const statusMutation = useMutation({
    mutationFn: ({ id, status }: { id: string; status: StatusAssinatura }) =>
      updateAssinaturaStatus(id, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["assinaturas"] })
      toast({ title: "Status atualizado", description: "O status da assinatura foi alterado." })
    },
    onError: (err: unknown) => {
      const msg = (err as { response?: { data?: { mensagem?: string; message?: string } } })?.response?.data?.mensagem
        || (err as { response?: { data?: { message?: string } } })?.response?.data?.message
        || "Erro ao alterar status."
      toast({ title: "Erro", description: msg, variant: "destructive" })
    },
  })

  function handleSubmit(formData: Parameters<typeof createAssinatura>[0]) {
    if (editAssinatura) {
      updateMutation.mutate({ id: editAssinatura.id, ...formData })
    } else {
      createMutation.mutate(formData)
    }
  }

  function handleEdit(as: Assinatura) {
    setEditAssinatura(as)
    setSheetOpen(true)
  }

  function handleNewAssinatura() {
    setEditAssinatura(null)
    setSheetOpen(true)
  }

  const filtered = data?.content.filter((as) =>
    !search || as.pacienteNome.toLowerCase().includes(search.toLowerCase())
  )

  return (
    <div className="space-y-5 animate-fade-in">
      <div className="flex items-start justify-between">
        <div>
          <h1 className="text-2xl font-bold font-primary text-foreground tracking-tight flex items-center gap-2">
            <Star className="h-6 w-6 text-primary" />
            Assinaturas
          </h1>
          <p className="text-sm text-muted-foreground font-secondary mt-0.5">
            {data ? `${data.totalElements} assinaturas` : "Carregando..."}
          </p>
        </div>
        <Button className="bg-primary text-primary-foreground font-primary" onClick={handleNewAssinatura}>
          <Plus className="h-4 w-4 mr-2" /> Nova Assinatura
        </Button>
      </div>

      <Card className="border border-border/60 shadow-soft">
        <CardHeader className="pb-3">
          <div className="flex gap-3">
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
                    {["Paciente", "Serviço", "Valor", "Sessões", "Progresso", "Vencimento", "Status", ""].map((h) => (
                      <TableHead key={h || "actions"} className="text-xs font-semibold font-primary text-muted-foreground uppercase tracking-wide">
                        {h}
                      </TableHead>
                    ))}
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {!filtered || filtered.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={8} className="text-center text-muted-foreground font-secondary py-12">
                        Nenhuma assinatura encontrada
                      </TableCell>
                    </TableRow>
                  ) : (
                    filtered.map((as) => {
                      const cfg = statusConfig[as.status]
                      const progresso = as.sessoesContratadas > 0
                        ? Math.round((as.sessoesRealizadas / as.sessoesContratadas) * 100)
                        : 0
                      return (
                        <TableRow key={as.id} className="border-border/40 hover:bg-muted/20">
                          <TableCell className="font-semibold font-primary text-sm text-foreground">
                            {as.pacienteNome}
                          </TableCell>
                          <TableCell className="text-sm font-secondary text-muted-foreground max-w-[160px] truncate" title={as.servicoDescricao}>
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
                                  style={{ width: `${Math.min(progresso, 100)}%` }}
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
                          <TableCell>
                            <DropdownMenu>
                              <DropdownMenuTrigger asChild>
                                <Button variant="ghost" size="icon" className="h-8 w-8">
                                  <MoreHorizontal className="h-4 w-4" />
                                </Button>
                              </DropdownMenuTrigger>
                              <DropdownMenuContent align="end" className="font-secondary">
                                {as.status === "ATIVO" && (
                                  <>
                                    <DropdownMenuItem onClick={() => handleEdit(as)}>
                                      <Pencil className="h-4 w-4 mr-2" /> Editar
                                    </DropdownMenuItem>
                                    <DropdownMenuItem
                                      className="text-destructive"
                                      onClick={() => statusMutation.mutate({ id: as.id, status: "CANCELADO" })}
                                    >
                                      <Ban className="h-4 w-4 mr-2" /> Cancelar
                                    </DropdownMenuItem>
                                  </>
                                )}
                                {as.status === "CANCELADO" && (
                                  <DropdownMenuItem onClick={() => statusMutation.mutate({ id: as.id, status: "ATIVO" })}>
                                    Reativar
                                  </DropdownMenuItem>
                                )}
                                {as.status === "VENCIDO" && (
                                  <DropdownMenuItem onClick={() => handleEdit(as)}>
                                    <Pencil className="h-4 w-4 mr-2" /> Renovar
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

      <AssinaturaFormSheet
        open={sheetOpen}
        onOpenChange={(open) => {
          setSheetOpen(open)
          if (!open) setEditAssinatura(null)
        }}
        onSubmit={handleSubmit}
        assinatura={editAssinatura}
        isPending={createMutation.isPending || updateMutation.isPending}
      />
    </div>
  )
}
