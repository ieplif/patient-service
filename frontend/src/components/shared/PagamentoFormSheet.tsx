import { useState, useEffect } from "react"
import { useQuery } from "@tanstack/react-query"
import { Sheet, SheetContent, SheetHeader, SheetTitle, SheetDescription } from "@/components/ui/sheet"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { getPatients } from "@/api/patients"
import type { FormaPagamento } from "@/types"

const formasPagamento: { value: FormaPagamento; label: string }[] = [
  { value: "PIX", label: "Pix" },
  { value: "CARTAO_CREDITO", label: "Cartao Credito" },
  { value: "CARTAO_DEBITO", label: "Cartao Debito" },
  { value: "DINHEIRO", label: "Dinheiro" },
]

interface PagamentoFormSheetProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  onSubmit: (data: {
    pacienteId: string
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

  useEffect(() => {
    if (open) {
      setPacienteId("")
      setValor("")
      setFormaPagamento("")
      setNumeroParcelas("1")
      setDataVencimento(new Date().toISOString().split("T")[0])
      setObservacoes("")
    }
  }, [open])

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!formaPagamento) return
    onSubmit({
      pacienteId,
      valor: Number(valor),
      formaPagamento,
      numeroParcelas: Number(numeroParcelas) > 1 ? Number(numeroParcelas) : undefined,
      dataVencimento,
      observacoes: observacoes || undefined,
    })
  }

  const showParcelas = formaPagamento === "CARTAO_CREDITO"

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
            <Select value={pacienteId} onValueChange={setPacienteId}>
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
              <Select value={formaPagamento} onValueChange={(v) => setFormaPagamento(v as FormaPagamento)}>
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
            {showParcelas && (
              <div className="space-y-2">
                <Label className="font-primary">Parcelas</Label>
                <Input
                  type="number"
                  min="1"
                  max="12"
                  value={numeroParcelas}
                  onChange={(e) => setNumeroParcelas(e.target.value)}
                  className="font-secondary"
                />
              </div>
            )}
          </div>

          <div className="space-y-2">
            <Label className="font-primary">Observacoes</Label>
            <textarea
              value={observacoes}
              onChange={(e) => setObservacoes(e.target.value)}
              placeholder="Observacoes opcionais..."
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
