import { useState, useEffect } from "react"
import { useQuery } from "@tanstack/react-query"
import { Sheet, SheetContent, SheetHeader, SheetTitle, SheetDescription } from "@/components/ui/sheet"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { getPatients } from "@/api/patients"
import { getAssinaturas } from "@/api/assinaturas"
import { getAgendamentos } from "@/api/agendamentos"
import type { FormaPagamento, Pagamento } from "@/types"
import { format } from "date-fns"
import { ptBR } from "date-fns/locale"

const formasPagamento: { value: FormaPagamento; label: string }[] = [
  { value: "PIX", label: "Pix" },
  { value: "CARTAO_CREDITO", label: "Cartão Crédito" },
  { value: "CARTAO_DEBITO", label: "Cartão Débito" },
  { value: "DINHEIRO", label: "Dinheiro" },
]

interface PagamentoFormSheetProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  onSubmit: (data: {
    pacienteId: string
    assinaturaIds?: string[]
    agendamentoId?: string
    valor: number
    formaPagamento: FormaPagamento
    numeroParcelas?: number
    dataVencimento: string
    observacoes?: string
  }) => void
  isPending?: boolean
  /** Quando fornecido, abre em modo edição: preenche os campos e altera labels/botões. */
  initialData?: Pagamento | null
}

export function PagamentoFormSheet({ open, onOpenChange, onSubmit, isPending, initialData }: PagamentoFormSheetProps) {
  const isEdit = !!initialData
  const [pacienteId, setPacienteId] = useState("")
  const [assinaturaIds, setAssinaturaIds] = useState<string[]>([])
  const [agendamentoId, setAgendamentoId] = useState("")
  const [valor, setValor] = useState("")
  const [formaPagamento, setFormaPagamento] = useState<FormaPagamento | "">("")
  const [numeroParcelas, setNumeroParcelas] = useState("1")
  const [dataVencimento, setDataVencimento] = useState("")
  const [observacoes, setObservacoes] = useState("")

  const { data: pacientesData } = useQuery({
    queryKey: ["patients-all"],
    queryFn: () => getPatients({ page: 0, size: 200 }),
    enabled: open,
  })

  const { data: assinaturasData } = useQuery({
    queryKey: ["assinaturas-paciente", pacienteId],
    queryFn: () => getAssinaturas({ pacienteId, size: 100, sort: "createdAt,desc" }),
    enabled: open && !!pacienteId,
  })

  const { data: agendamentosData } = useQuery({
    queryKey: ["agendamentos-paciente-pagamento", pacienteId],
    queryFn: () => getAgendamentos({ pacienteId, size: 100, sort: "dataHora,desc" }),
    enabled: open && !!pacienteId,
  })

  useEffect(() => {
    if (open) {
      if (initialData) {
        // Modo edição: preenche os campos com os dados existentes
        setPacienteId(initialData.pacienteId)
        setAssinaturaIds(initialData.assinaturaIds ?? [])
        setAgendamentoId(initialData.agendamentoId ?? "")
        setValor(String(initialData.valor))
        setFormaPagamento(initialData.formaPagamento)
        setNumeroParcelas(String(initialData.numeroParcelas ?? 1))
        setDataVencimento(initialData.dataVencimento.split("T")[0])
        setObservacoes(initialData.observacoes ?? "")
      } else {
        // Modo criação: zera tudo
        setPacienteId("")
        setAssinaturaIds([])
        setAgendamentoId("")
        setValor("")
        setFormaPagamento("")
        setNumeroParcelas("1")
        setDataVencimento(new Date().toISOString().split("T")[0])
        setObservacoes("")
      }
    }
  }, [open, initialData])

  function handlePacienteChange(id: string) {
    setPacienteId(id)
    setAssinaturaIds([])
    setAgendamentoId("")
  }

  function toggleAssinatura(id: string) {
    setAgendamentoId("")
    setAssinaturaIds((prev) => {
      const next = prev.includes(id) ? prev.filter(x => x !== id) : [...prev, id]
      // recalcula valor como soma das assinaturas selecionadas
      const allAssinaturas = assinaturasData?.content ?? []
      const soma = allAssinaturas
        .filter(a => next.includes(a.id))
        .reduce((acc, a) => acc + a.valor, 0)
      setValor(soma > 0 ? String(soma) : "")
      return next
    })
  }

  function handleAgendamentoChange(id: string) {
    setAgendamentoId(id === "_none" ? "" : id)
    if (id !== "_none") {
      setAssinaturaIds([])
    }
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!formaPagamento) return
    // Confirmação extra para vencimentos muito retroativos
    if (dataVencimento) {
      const venc = new Date(dataVencimento + "T00:00:00")
      const dias = Math.floor((Date.now() - venc.getTime()) / (1000 * 60 * 60 * 24))
      if (dias > 30) {
        const ok = window.confirm(
          `Está lançando um pagamento com vencimento de ${dataVencimento} (${dias} dias atrás). Confirmar?`
        )
        if (!ok) return
      }
    }
    onSubmit({
      pacienteId,
      assinaturaIds: assinaturaIds.length > 0 ? assinaturaIds : undefined,
      agendamentoId: agendamentoId || undefined,
      valor: Number(valor),
      formaPagamento,
      numeroParcelas: Number(numeroParcelas) > 1 ? Number(numeroParcelas) : undefined,
      dataVencimento,
      observacoes: observacoes || undefined,
    })
  }

  const maxParcelas = formaPagamento === "CARTAO_CREDITO" ? 12 : 2
  const assinaturas = assinaturasData?.content ?? []
  const agendamentos = agendamentosData?.content ?? []
  const temAssinaturaSelecionada = assinaturaIds.length > 0

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent className="overflow-y-auto">
        <SheetHeader>
          <SheetTitle className="font-primary">{isEdit ? "Editar Pagamento" : "Novo Pagamento"}</SheetTitle>
          <SheetDescription className="font-secondary">
            {isEdit
              ? "Atualize os dados do pagamento. Mudar valor, parcelas ou vencimento regenera as parcelas."
              : "Registre um novo pagamento de paciente."}
          </SheetDescription>
        </SheetHeader>

        <form onSubmit={handleSubmit} className="space-y-4 mt-6">
          <div className="space-y-2">
            <Label className="font-primary">Paciente *</Label>
            <Select value={pacienteId} onValueChange={handlePacienteChange}>
              <SelectTrigger className="font-secondary">
                <SelectValue placeholder="Selecione o paciente" />
              </SelectTrigger>
              <SelectContent>
                {pacientesData?.content.map((p) => (
                  <SelectItem key={p.id} value={p.id} className="font-secondary">
                    {p.nomeCompleto}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          {/* Assinaturas (seleção múltipla) */}
          {pacienteId && assinaturas.length > 0 && (
            <div className="space-y-2">
              <Label className="font-primary">
                Assinaturas
                {assinaturaIds.length > 0 && (
                  <span className="ml-2 text-xs text-muted-foreground font-secondary">
                    ({assinaturaIds.length} selecionada{assinaturaIds.length > 1 ? "s" : ""})
                  </span>
                )}
              </Label>
              <div className="space-y-1.5">
                {assinaturas.map((a) => {
                  const selecionada = assinaturaIds.includes(a.id)
                  return (
                    <div
                      key={a.id}
                      onClick={() => toggleAssinatura(a.id)}
                      className={`flex items-center gap-3 p-2.5 rounded-md border cursor-pointer transition-colors font-secondary text-sm ${
                        selecionada
                          ? "border-primary bg-primary/5 text-foreground"
                          : "border-border hover:bg-muted/30 text-muted-foreground"
                      }`}
                    >
                      <div className={`h-4 w-4 rounded border flex-shrink-0 flex items-center justify-center text-xs ${
                        selecionada ? "bg-primary border-primary text-white" : "border-muted-foreground"
                      }`}>
                        {selecionada && "✓"}
                      </div>
                      <span>
                        {a.servicoDescricao} — <strong>R$ {a.valor.toFixed(2)}</strong>{" "}
                        <span className="text-xs">({a.status})</span>
                      </span>
                    </div>
                  )
                })}
              </div>
            </div>
          )}

          {/* Agendamento (opcional) */}
          {pacienteId && !temAssinaturaSelecionada && (
            <div className="space-y-2">
              <Label className="font-primary">Agendamento</Label>
              <Select value={agendamentoId || "_none"} onValueChange={handleAgendamentoChange}>
                <SelectTrigger className="font-secondary">
                  <SelectValue placeholder="Nenhum (pagamento avulso)" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="_none" className="font-secondary text-muted-foreground">
                    Nenhum (pagamento avulso)
                  </SelectItem>
                  {agendamentos.map((ag) => (
                    <SelectItem key={ag.id} value={ag.id} className="font-secondary">
                      {ag.servicoDescricao} — {format(new Date(ag.dataHora), "dd/MM/yyyy 'às' HH:mm", { locale: ptBR })}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          )}

          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-2">
              <Label className="font-primary">Valor (R$) *</Label>
              <Input
                type="number"
                min="0.01"
                step="0.01"
                value={valor}
                onChange={(e) => setValor(e.target.value)}
                required
                placeholder="384,00"
                className="font-secondary"
              />
            </div>
            <div className="space-y-2">
              <Label className="font-primary">Forma *</Label>
              <Select value={formaPagamento} onValueChange={(v) => {
                setFormaPagamento(v as FormaPagamento)
                const max = v === "CARTAO_CREDITO" ? 12 : 2
                if (Number(numeroParcelas) > max) setNumeroParcelas(String(max))
              }}>
                <SelectTrigger className="font-secondary">
                  <SelectValue placeholder="Selecione" />
                </SelectTrigger>
                <SelectContent>
                  {formasPagamento.map((f) => (
                    <SelectItem key={f.value} value={f.value} className="font-secondary">
                      {f.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <Label className="font-primary">Vencimento *</Label>
                {dataVencimento && new Date(dataVencimento + "T00:00:00") < new Date(new Date().toDateString()) && (
                  <span className="rounded-full bg-amber-100 text-amber-800 border border-amber-300 px-2 py-0.5 text-[10px] font-primary font-semibold uppercase tracking-wide">
                    Retroativo
                  </span>
                )}
              </div>
              <Input
                type="date"
                value={dataVencimento}
                onChange={(e) => setDataVencimento(e.target.value)}
                required
                className="font-secondary"
              />
            </div>
            {formaPagamento && (
              <div className="space-y-2">
                <Label className="font-primary">Parcelas (até {maxParcelas}x)</Label>
                <Input
                  type="number"
                  min="1"
                  max={maxParcelas}
                  value={numeroParcelas}
                  onChange={(e) => {
                    const v = Math.min(Number(e.target.value) || 1, maxParcelas)
                    setNumeroParcelas(String(v))
                  }}
                  className="font-secondary"
                />
              </div>
            )}
          </div>

          <div className="space-y-2">
            <Label className="font-primary">Observações</Label>
            <textarea
              value={observacoes}
              onChange={(e) => setObservacoes(e.target.value)}
              placeholder="Observações opcionais..."
              className="flex min-h-[60px] w-full rounded-md border border-input bg-background px-3 py-2 text-sm font-secondary ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
            />
          </div>

          <div className="flex gap-3 pt-4">
            <Button
              type="button"
              variant="outline"
              className="flex-1 font-primary"
              onClick={() => onOpenChange(false)}
            >
              Cancelar
            </Button>
            <Button
              type="submit"
              className="flex-1 bg-primary text-primary-foreground font-primary"
              disabled={!pacienteId || !valor || !formaPagamento || !dataVencimento || isPending}
            >
              {isPending ? "Salvando..." : isEdit ? "Salvar alterações" : "Registrar Pagamento"}
            </Button>
          </div>
        </form>
      </SheetContent>
    </Sheet>
  )
}
