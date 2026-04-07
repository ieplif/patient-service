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
import type { FormaPagamento } from "@/types"
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
    assinaturaId?: string
    agendamentoId?: string
    valor: number
    formaPagamento: FormaPagamento
    numeroParcelas?: number
    dataVencimento: string
    observacoes?: string
  }) => void
  isPending?: boolean
}

export function PagamentoFormSheet({ open, onOpenChange, onSubmit, isPending }: PagamentoFormSheetProps) {
  const [pacienteId, setPacienteId] = useState("")
  const [assinaturaId, setAssinaturaId] = useState("")
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
      setPacienteId("")
      setAssinaturaId("")
      setAgendamentoId("")
      setValor("")
      setFormaPagamento("")
      setNumeroParcelas("1")
      setDataVencimento(new Date().toISOString().split("T")[0])
      setObservacoes("")
    }
  }, [open])

  function handlePacienteChange(id: string) {
    setPacienteId(id)
    setAssinaturaId("")
    setAgendamentoId("")
  }

  function handleAssinaturaChange(id: string) {
    setAssinaturaId(id === "_none" ? "" : id)
    if (id !== "_none") {
      setAgendamentoId("")
      const assinatura = assinaturasData?.content.find(a => a.id === id)
      if (assinatura) setValor(String(assinatura.valor))
    }
  }

  function handleAgendamentoChange(id: string) {
    setAgendamentoId(id === "_none" ? "" : id)
    if (id !== "_none") {
      setAssinaturaId("")
    }
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!formaPagamento) return
    onSubmit({
      pacienteId,
      assinaturaId: assinaturaId || undefined,
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

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent className="overflow-y-auto">
        <SheetHeader>
          <SheetTitle className="font-primary">Novo Pagamento</SheetTitle>
          <SheetDescription className="font-secondary">
            Registre um novo pagamento de paciente.
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

          {/* Assinatura (opcional) */}
          {pacienteId && (
            <div className="space-y-2">
              <Label className="font-primary">Assinatura</Label>
              <Select value={assinaturaId || "_none"} onValueChange={handleAssinaturaChange}>
                <SelectTrigger className="font-secondary">
                  <SelectValue placeholder="Nenhuma (pagamento avulso)" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="_none" className="font-secondary text-muted-foreground">
                    Nenhuma (pagamento avulso)
                  </SelectItem>
                  {assinaturas.map((a) => (
                    <SelectItem key={a.id} value={a.id} className="font-secondary">
                      {a.servicoDescricao} — R$ {a.valor.toFixed(2)} ({a.status})
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          )}

          {/* Agendamento (opcional) */}
          {pacienteId && !assinaturaId && (
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
                      {ag.servicoNome} — {format(new Date(ag.dataHora), "dd/MM/yyyy 'às' HH:mm", { locale: ptBR })}
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
              <Label className="font-primary">Vencimento *</Label>
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
              {isPending ? "Salvando..." : "Registrar Pagamento"}
            </Button>
          </div>
        </form>
      </SheetContent>
    </Sheet>
  )
}
