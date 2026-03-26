import { useState, useEffect } from "react"
import { useQuery } from "@tanstack/react-query"
import { Sheet, SheetContent, SheetHeader, SheetTitle, SheetDescription } from "@/components/ui/sheet"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { getPatients } from "@/api/patients"
import { getServicos } from "@/api/servicos"
import type { Assinatura } from "@/types"

interface AssinaturaFormSheetProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  onSubmit: (data: {
    pacienteId: string
    servicoId: string
    dataInicio: string
    dataVencimento?: string
    sessoesContratadas: number
    valor: number
    observacoes?: string
  }) => void
  assinatura?: Assinatura | null
  isPending?: boolean
}

export function AssinaturaFormSheet({ open, onOpenChange, onSubmit, assinatura, isPending }: AssinaturaFormSheetProps) {
  const [pacienteId, setPacienteId] = useState("")
  const [servicoId, setServicoId] = useState("")
  const [dataInicio, setDataInicio] = useState("")
  const [dataVencimento, setDataVencimento] = useState("")
  const [sessoesContratadas, setSessoesContratadas] = useState("")
  const [valor, setValor] = useState("")
  const [observacoes, setObservacoes] = useState("")

  const { data: pacientesData } = useQuery({
    queryKey: ["patients-all"],
    queryFn: () => getPatients({ page: 0, size: 200 }),
    enabled: open,
  })

  const { data: servicosData } = useQuery({
    queryKey: ["servicos-all"],
    queryFn: () => getServicos(),
    enabled: open,
  })

  useEffect(() => {
    if (open && assinatura) {
      setPacienteId(assinatura.pacienteId)
      setServicoId(assinatura.servicoId)
      setDataInicio(assinatura.dataInicio)
      setDataVencimento(assinatura.dataVencimento || "")
      setSessoesContratadas(String(assinatura.sessoesContratadas))
      setValor(String(assinatura.valor))
      setObservacoes(assinatura.observacoes || "")
    } else if (open) {
      setPacienteId("")
      setServicoId("")
      setDataInicio(new Date().toISOString().split("T")[0])
      setDataVencimento("")
      setSessoesContratadas("")
      setValor("")
      setObservacoes("")
    }
  }, [open, assinatura])

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    onSubmit({
      pacienteId,
      servicoId,
      dataInicio,
      dataVencimento: dataVencimento || undefined,
      sessoesContratadas: Number(sessoesContratadas),
      valor: Number(valor),
      observacoes: observacoes || undefined,
    })
  }

  const isEditing = !!assinatura

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent className="overflow-y-auto">
        <SheetHeader>
          <SheetTitle className="font-primary">
            {isEditing ? "Editar Assinatura" : "Nova Assinatura"}
          </SheetTitle>
          <SheetDescription className="font-secondary">
            {isEditing
              ? "Atualize os dados da assinatura."
              : "Preencha os dados para criar uma nova assinatura."}
          </SheetDescription>
        </SheetHeader>

        <form onSubmit={handleSubmit} className="space-y-4 mt-6">
          <div className="space-y-2">
            <Label className="font-primary">Paciente *</Label>
            <Select value={pacienteId} onValueChange={setPacienteId} disabled={isEditing}>
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

          <div className="space-y-2">
            <Label className="font-primary">Serviço *</Label>
            <Select value={servicoId} onValueChange={setServicoId}>
              <SelectTrigger className="font-secondary">
                <SelectValue placeholder="Selecione o serviço" />
              </SelectTrigger>
              <SelectContent>
                {servicosData?.map((s) => (
                  <SelectItem key={s.id} value={s.id} className="font-secondary">
                    {s.descricao} — {new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }).format(s.valor)}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-2">
              <Label className="font-primary">Início *</Label>
              <Input
                type="date"
                value={dataInicio}
                onChange={(e) => setDataInicio(e.target.value)}
                required
                className="font-secondary"
              />
            </div>
            <div className="space-y-2">
              <Label className="font-primary">Vencimento</Label>
              <Input
                type="date"
                value={dataVencimento}
                onChange={(e) => setDataVencimento(e.target.value)}
                className="font-secondary"
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-2">
              <Label className="font-primary">Sessões contratadas *</Label>
              <Input
                type="number"
                min="1"
                value={sessoesContratadas}
                onChange={(e) => setSessoesContratadas(e.target.value)}
                required
                placeholder="12"
                className="font-secondary"
              />
            </div>
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
              disabled={!pacienteId || !servicoId || !dataInicio || !sessoesContratadas || !valor || isPending}
            >
              {isPending ? "Salvando..." : isEditing ? "Salvar" : "Criar Assinatura"}
            </Button>
          </div>
        </form>
      </SheetContent>
    </Sheet>
  )
}
