import { useState } from "react"
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query"
import { Star, Plus, Pencil, Ban, MoreHorizontal, Search, RefreshCw, Pause, Play } from "lucide-react"
import { format } from "date-fns"
import { getAssinaturas, createAssinatura, updateAssinatura, updateAssinaturaStatus, regenerarHorarios, suspenderAssinatura, reativarAssinatura } from "@/api/assinaturas"
import type { HorarioFixoSlotDTO } from "@/api/assinaturas"
import { shortenName } from "@/lib/names"
import { createAgendamento, createAgendamentoRecorrente } from "@/api/agendamentos"
import type { StatusAssinatura, Assinatura } from "@/types"
import type { AssinaturaFormData } from "@/components/shared/AssinaturaFormSheet"
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
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from "@/components/ui/dialog"
import { Label } from "@/components/ui/label"

const PAGE_SIZE = 15

const DAY_TO_JAVA: Record<string, string> = {
  "0": "SUNDAY",
  "1": "MONDAY",
  "2": "TUESDAY",
  "3": "WEDNESDAY",
  "4": "THURSDAY",
  "5": "FRIDAY",
  "6": "SATURDAY",
}

const statusConfig: Record<StatusAssinatura, { label: string; className: string }> = {
  ATIVO: { label: "Ativo", className: "bg-primary/15 text-primary border-primary/30" },
  SUSPENSO: { label: "Suspensa", className: "bg-amber-100 text-amber-800 border-amber-300" },
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
  { value: "SUSPENSO", label: "Suspensa" },
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
  // Suspensão/reativação
  const [suspenderOpen, setSuspenderOpen] = useState(false)
  const [suspenderAlvo, setSuspenderAlvo] = useState<Assinatura | null>(null)
  const [motivoSuspensao, setMotivoSuspensao] = useState("")
  const [dataPrevistaRetomada, setDataPrevistaRetomada] = useState("")
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
    mutationFn: async (formData: AssinaturaFormData) => {
      const { profissionalId, horariosFixos, agendamentosIndividuais, ...assinaturaPayload } = formData
      const assinatura = await createAssinatura(assinaturaPayload)

      let agendamentosCriados = 0
      const errosAgendamento: string[] = []

      // Auto-create recurring appointments for Pilates with horários fixos
      // Profissional é opcional — quando vazio os agendamentos ficam com "Sem profissional"
      if (horariosFixos?.length) {
        const slotsValidos = horariosFixos.filter(h => h.dia && h.horario)
        let aulasRestantes = assinaturaPayload.sessoesContratadas

        for (let idx = 0; idx < slotsValidos.length; idx++) {
          const h = slotsValidos[idx]
          if (aulasRestantes <= 0) break
          const slotsRestantes = slotsValidos.length - idx
          const sessoesParaEsteSlot = Math.ceil(aulasRestantes / slotsRestantes)

          try {
            const result = await createAgendamentoRecorrente({
              pacienteId: assinaturaPayload.pacienteId,
              ...(profissionalId ? { profissionalId } : {}),
              servicoId: assinaturaPayload.servicoId,
              assinaturaId: assinatura.id,
              frequencia: "SEMANAL",
              diasSemana: [DAY_TO_JAVA[h.dia]],
              horaInicio: h.horario,
              dataFim: assinatura.dataVencimento,
              totalSessoes: sessoesParaEsteSlot,
            })
            const criados = result.agendamentosCriados?.length ?? 0
            agendamentosCriados += criados
            aulasRestantes -= criados
            if (result.datasIgnoradas?.length) {
              const motivos = [...new Set(result.datasIgnoradas.map(d => d.motivo))]
              errosAgendamento.push(...motivos)
            }
          } catch (e) {
            const errMsg = (e as { response?: { data?: { mensagem?: string; message?: string } } })?.response?.data?.mensagem
              || (e as { response?: { data?: { message?: string } } })?.response?.data?.message
              || "Erro ao criar agendamentos recorrentes"
            errosAgendamento.push(errMsg)
          }
        }
      }

      // Create individual appointments for non-Pilates services
      // Profissional é opcional — quando vazio o agendamento fica com "Sem profissional"
      if (agendamentosIndividuais?.length) {
        for (const ag of agendamentosIndividuais) {
          if (ag.dataHora) {
            try {
              await createAgendamento({
                pacienteId: assinaturaPayload.pacienteId,
                ...(profissionalId ? { profissionalId } : {}),
                servicoId: assinaturaPayload.servicoId,
                assinaturaId: assinatura.id,
                dataHora: ag.dataHora + ":00",
              })
              agendamentosCriados++
            } catch (e) {
              const errMsg = (e as { response?: { data?: { mensagem?: string; message?: string } } })?.response?.data?.mensagem
                || (e as { response?: { data?: { message?: string } } })?.response?.data?.message
                || "Erro ao criar agendamento"
              errosAgendamento.push(errMsg)
            }
          }
        }
      }

      return { assinatura, agendamentosCriados, errosAgendamento }
    },
    onSuccess: ({ agendamentosCriados, errosAgendamento }) => {
      queryClient.invalidateQueries({ queryKey: ["assinaturas"] })
      queryClient.invalidateQueries({ queryKey: ["agendamentos"] })

      if (agendamentosCriados > 0) {
        toast({
          title: "Assinatura criada",
          description: `Assinatura criada com ${agendamentosCriados} agendamentos.`,
        })
      } else if (errosAgendamento.length > 0) {
        toast({
          title: "Assinatura criada (sem agendamentos)",
          description: `Assinatura salva, mas os agendamentos falharam: ${errosAgendamento[0]}`,
          variant: "destructive",
        })
      } else {
        toast({ title: "Assinatura criada", description: "Nova assinatura cadastrada com sucesso." })
      }
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

  const suspenderMutation = useMutation({
    mutationFn: ({ id, motivo, dataPrevistaRetomada }: { id: string; motivo: string; dataPrevistaRetomada?: string }) =>
      suspenderAssinatura(id, { motivo, dataPrevistaRetomada }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["assinaturas"] })
      queryClient.invalidateQueries({ queryKey: ["agendamentos"] })
      toast({ title: "Assinatura suspensa", description: "Agendamentos futuros foram cancelados." })
      setSuspenderOpen(false)
      setSuspenderAlvo(null)
      setMotivoSuspensao("")
      setDataPrevistaRetomada("")
    },
    onError: (err: unknown) => {
      const msg = (err as { response?: { data?: { mensagem?: string; message?: string } } })?.response?.data?.mensagem
        || (err as { response?: { data?: { message?: string } } })?.response?.data?.message
        || "Erro ao suspender assinatura."
      toast({ title: "Erro", description: msg, variant: "destructive" })
    },
  })

  const reativarMutation = useMutation({
    mutationFn: (id: string) => reativarAssinatura(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["assinaturas"] })
      toast({
        title: "Assinatura reativada",
        description: "Vencimento recalculado. Agende os novos horários quando combinar com a paciente.",
      })
    },
    onError: (err: unknown) => {
      const msg = (err as { response?: { data?: { mensagem?: string; message?: string } } })?.response?.data?.mensagem
        || (err as { response?: { data?: { message?: string } } })?.response?.data?.message
        || "Erro ao reativar assinatura."
      toast({ title: "Erro", description: msg, variant: "destructive" })
    },
  })

  function openSuspenderDialog(as: Assinatura) {
    setSuspenderAlvo(as)
    setMotivoSuspensao("")
    setDataPrevistaRetomada("")
    setSuspenderOpen(true)
  }

  function handleConfirmSuspender() {
    if (!suspenderAlvo) return
    if (!motivoSuspensao.trim()) {
      toast({ title: "Motivo obrigatório", description: "Informe por que está suspendendo (ex.: gravidez).", variant: "destructive" })
      return
    }
    suspenderMutation.mutate({
      id: suspenderAlvo.id,
      motivo: motivoSuspensao.trim(),
      dataPrevistaRetomada: dataPrevistaRetomada || undefined,
    })
  }

  function handleReativar(as: Assinatura) {
    const ok = window.confirm(
      `Reativar a assinatura de ${as.pacienteNome}?\n\n` +
      `O vencimento será recalculado (hoje + validade do plano) e os campos de suspensão serão limpos. ` +
      `O saldo de sessões permanece.\n\n` +
      `Continuar?`
    )
    if (!ok) return
    reativarMutation.mutate(as.id)
  }

  async function handleSubmit(formData: AssinaturaFormData) {
    if (editAssinatura) {
      const { profissionalId, horariosFixos, ...payload } = formData
      const horariosNovos = (horariosFixos ?? []).filter(h => h.dia && h.horario)
      const querRegenerar = horariosNovos.length > 0

      if (querRegenerar) {
        const ok = window.confirm(
          `Você está alterando os horários fixos.\n\n` +
          `Isso vai CANCELAR os agendamentos futuros pendentes (status Agendado/Confirmado) ` +
          `e CRIAR novos agendamentos a partir dos novos horários.\n\n` +
          `Os agendamentos passados (já realizados, cancelados ou faltas) não serão tocados.\n\n` +
          `Continuar?`
        )
        if (!ok) return

        try {
          const slots: HorarioFixoSlotDTO[] = horariosNovos.map(h => ({
            diaSemana: DAY_TO_JAVA[h.dia] as HorarioFixoSlotDTO["diaSemana"],
            horaInicio: h.horario,
          }))
          const result = await regenerarHorarios(editAssinatura.id, {
            horariosFixos: slots,
            profissionalId: profissionalId || undefined,
          })
          toast({
            title: "Horários regenerados",
            description: `${result.agendamentosCancelados} cancelados, ${result.agendamentosCriados} criados.`,
          })
          queryClient.invalidateQueries({ queryKey: ["agendamentos"] })
          queryClient.invalidateQueries({ queryKey: ["agendamentos-assinatura"] })
        } catch (e) {
          const msg = (e as { response?: { data?: { mensagem?: string; message?: string } } })?.response?.data?.mensagem
            || (e as { response?: { data?: { message?: string } } })?.response?.data?.message
            || "Erro ao regenerar horários."
          toast({ title: "Erro ao regenerar horários", description: msg, variant: "destructive" })
          return
        }
      }

      updateMutation.mutate({ id: editAssinatura.id, ...payload })
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
      <div className="flex flex-col sm:flex-row items-start sm:items-center gap-4">
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
          <div className="flex flex-col sm:flex-row gap-3">
            <Select
              value={statusFilter}
              onValueChange={(v) => { setStatusFilter(v as StatusAssinatura | "TODOS"); setPage(0) }}
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
              <div className="overflow-x-auto">
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
                          <TableCell className="font-semibold font-primary text-sm text-foreground" title={as.pacienteNome}>
                            {shortenName(as.pacienteNome)}
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
                            <div className="flex items-center gap-1.5">
                              <span
                                className={`inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-semibold font-primary ${cfg.className}`}
                                title={
                                  as.status === "SUSPENSO" && as.motivoSuspensao
                                    ? `Motivo: ${as.motivoSuspensao}${as.dataPrevistaRetomada ? ` · Retomada prevista: ${format(new Date(as.dataPrevistaRetomada + "T00:00:00"), "dd/MM/yyyy")}` : ""}`
                                    : undefined
                                }
                              >
                                {cfg.label}
                              </span>
                              {as.renovacaoAutomatica && (
                                <span title="Renovação automática ativa" className="text-primary">
                                  <RefreshCw className="h-3.5 w-3.5" />
                                </span>
                              )}
                            </div>
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
                                    <DropdownMenuItem onClick={() => updateMutation.mutate({
                                      id: as.id,
                                      renovacaoAutomatica: !as.renovacaoAutomatica,
                                    })}>
                                      <RefreshCw className="h-4 w-4 mr-2" />
                                      {as.renovacaoAutomatica ? "Desativar renovação auto." : "Ativar renovação auto."}
                                    </DropdownMenuItem>
                                    <DropdownMenuItem onClick={() => openSuspenderDialog(as)}>
                                      <Pause className="h-4 w-4 mr-2" /> Suspender
                                    </DropdownMenuItem>
                                    <DropdownMenuItem
                                      className="text-destructive"
                                      onClick={() => statusMutation.mutate({ id: as.id, status: "CANCELADO" })}
                                    >
                                      <Ban className="h-4 w-4 mr-2" /> Cancelar
                                    </DropdownMenuItem>
                                  </>
                                )}
                                {as.status === "SUSPENSO" && (
                                  <>
                                    <DropdownMenuItem onClick={() => handleReativar(as)}>
                                      <Play className="h-4 w-4 mr-2" /> Reativar
                                    </DropdownMenuItem>
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

      <Dialog open={suspenderOpen} onOpenChange={setSuspenderOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle className="font-primary">Suspender assinatura</DialogTitle>
            <DialogDescription className="font-secondary">
              {suspenderAlvo ? `${suspenderAlvo.pacienteNome} — ${suspenderAlvo.servicoDescricao}` : ""}
            </DialogDescription>
          </DialogHeader>

          <div className="rounded-md border border-amber-300 bg-amber-50 p-3 text-sm font-secondary text-amber-800">
            Vai cancelar todos os agendamentos futuros pendentes desta assinatura. O saldo de
            sessões será preservado e a renovação automática não roda enquanto suspensa.
          </div>

          <div className="space-y-1.5">
            <Label className="font-primary text-sm">Motivo da suspensão *</Label>
            <textarea
              value={motivoSuspensao}
              onChange={(e) => setMotivoSuspensao(e.target.value)}
              placeholder="Ex.: Gravidez, lesão, viagem prolongada..."
              className="flex min-h-[60px] w-full rounded-md border border-input bg-background px-3 py-2 text-sm font-secondary ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
            />
          </div>

          <div className="space-y-1.5">
            <Label className="font-primary text-sm">Data prevista de retomada (opcional)</Label>
            <Input
              type="date"
              value={dataPrevistaRetomada}
              onChange={(e) => setDataPrevistaRetomada(e.target.value)}
              className="font-secondary"
            />
          </div>

          <DialogFooter>
            <Button variant="outline" className="font-primary" onClick={() => setSuspenderOpen(false)}>
              Voltar
            </Button>
            <Button
              className="font-primary bg-amber-600 hover:bg-amber-700 text-white"
              onClick={handleConfirmSuspender}
              disabled={suspenderMutation.isPending}
            >
              {suspenderMutation.isPending ? "Suspendendo..." : "Confirmar suspensão"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
