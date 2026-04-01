import { useState, useEffect } from "react"
import { useQuery } from "@tanstack/react-query"
import { Sheet, SheetContent, SheetHeader, SheetTitle, SheetDescription } from "@/components/ui/sheet"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { getPatients } from "@/api/patients"
import { getServicos } from "@/api/servicos"
import type { Assinatura, Servico } from "@/types"

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

const DIAS_SEMANA = [
  { value: 0, label: "Dom" },
  { value: 1, label: "Seg" },
  { value: 2, label: "Ter" },
  { value: 3, label: "Qua" },
  { value: 4, label: "Qui" },
  { value: 5, label: "Sex" },
  { value: 6, label: "Sáb" },
]

function isPilatesService(servico: Servico | undefined | null): boolean {
  return !!servico?.atividadeNome?.toLowerCase().includes("pilates")
}

function isFrequenciaService(servico: Servico | undefined | null): boolean {
  return !!servico?.unidadeServico?.toLowerCase().match(/frequ[eê]ncia/)
}

function calcularAulas(dataInicio: string, validadeDias: number, diasSemana: number[]): number {
  if (!diasSemana.length) return 0
  const inicio = new Date(dataInicio + "T12:00:00")
  let count = 0
  for (let i = 0; i < validadeDias; i++) {
    const d = new Date(inicio)
    d.setDate(d.getDate() + i)
    if (diasSemana.includes(d.getDay())) count++
  }
  return count
}

function addDaysToDate(dateStr: string, days: number): string {
  const d = new Date(dateStr + "T12:00:00")
  d.setDate(d.getDate() + days - 1)
  return d.toISOString().split("T")[0]
}

export function AssinaturaFormSheet({ open, onOpenChange, onSubmit, assinatura, isPending }: AssinaturaFormSheetProps) {
  const [pacienteId, setPacienteId] = useState("")
  const [servicoId, setServicoId] = useState("")
  const [dataInicio, setDataInicio] = useState("")
  const [dataVencimento, setDataVencimento] = useState("")
  const [sessoesContratadas, setSessoesContratadas] = useState("")
  const [valor, setValor] = useState("")
  const [observacoes, setObservacoes] = useState("")
  const [diasSemana, setDiasSemana] = useState<number[]>([])

  const { data: pacientesData, isLoading: loadingPacientes, isError: errorPacientes } = useQuery({
    queryKey: ["patients-all"],
    queryFn: () => getPatients({ page: 0, size: 200 }),
    enabled: open,
    retry: 1,
  })

  const { data: servicosData, isLoading: loadingServicos } = useQuery({
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
      setDiasSemana([])
    } else if (open) {
      setPacienteId("")
      setServicoId("")
      setDataInicio(new Date().toISOString().split("T")[0])
      setDataVencimento("")
      setSessoesContratadas("")
      setValor("")
      setObservacoes("")
      setDiasSemana([])
    }
  }, [open, assinatura])

  const isEditing = !!assinatura
  const selectedServico = servicosData?.find(s => s.id === servicoId) ?? null
  const isPilates = isPilatesService(selectedServico)
  const isFrequencia = isFrequenciaService(selectedServico)
  const showDayPicker = isPilates && isFrequencia
  const sessoesLabel = isPilates ? "Aulas contratadas" : "Sessões contratadas"

  function handleServicoChange(id: string) {
    setServicoId(id)
    if (isEditing) return

    const servico = servicosData?.find(s => s.id === id)
    if (!servico) return

    if (servico.valor != null) {
      setValor(String(servico.valor))
    }

    const pilates = isPilatesService(servico)
    const freq = isFrequenciaService(servico)

    if (pilates && freq) {
      setSessoesContratadas("")
      setDiasSemana([])
    } else {
      setSessoesContratadas(String(servico.quantidade ?? 1))
    }

    if (servico.validadeDias && dataInicio) {
      setDataVencimento(addDaysToDate(dataInicio, servico.validadeDias))
    }
  }

  function handleDataInicioChange(data: string) {
    setDataInicio(data)
    if (!selectedServico?.validadeDias || !data) return

    setDataVencimento(addDaysToDate(data, selectedServico.validadeDias))

    if (showDayPicker && diasSemana.length > 0) {
      setSessoesContratadas(String(calcularAulas(data, selectedServico.validadeDias, diasSemana)))
    }
  }

  function handleToggleDia(dia: number) {
    const novos = diasSemana.includes(dia)
      ? diasSemana.filter(d => d !== dia)
      : [...diasSemana, dia]
    setDiasSemana(novos)

    if (!selectedServico?.validadeDias || !dataInicio) return
    const aulas = calcularAulas(dataInicio, selectedServico.validadeDias, novos)
    setSessoesContratadas(novos.length > 0 ? String(aulas) : "")
  }

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

  const formatCurrency = (v: number) =>
    new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }).format(v)

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
          {/* Paciente */}
          <div className="space-y-2">
            <Label className="font-primary">Paciente *</Label>
            <Select value={pacienteId} onValueChange={setPacienteId} disabled={isEditing}>
              <SelectTrigger className="font-secondary">
                <SelectValue placeholder="Selecione o paciente" />
              </SelectTrigger>
              <SelectContent>
                {loadingPacientes ? (
                  <div className="py-2 px-3 text-sm text-muted-foreground">Carregando...</div>
                ) : errorPacientes ? (
                  <div className="py-2 px-3 text-sm text-destructive">Erro ao carregar pacientes</div>
                ) : !pacientesData?.content?.length ? (
                  <div className="py-2 px-3 text-sm text-muted-foreground">Nenhum paciente cadastrado</div>
                ) : (
                  pacientesData.content.map((p) => (
                    <SelectItem key={p.id} value={p.id} className="font-secondary">
                      {p.nomeCompleto}
                    </SelectItem>
                  ))
                )}
              </SelectContent>
            </Select>
          </div>

          {/* Serviço */}
          <div className="space-y-2">
            <Label className="font-primary">Serviço *</Label>
            <Select value={servicoId} onValueChange={handleServicoChange}>
              <SelectTrigger className="font-secondary">
                <SelectValue placeholder="Selecione o serviço" />
              </SelectTrigger>
              <SelectContent>
                {loadingServicos ? (
                  <div className="py-2 px-3 text-sm text-muted-foreground">Carregando...</div>
                ) : !servicosData?.length ? (
                  <div className="py-2 px-3 text-sm text-muted-foreground">Nenhum serviço encontrado</div>
                ) : (
                  servicosData.map((s) => (
                    <SelectItem key={s.id} value={s.id} className="font-secondary">
                      {s.descricao}{s.valor != null ? ` — ${formatCurrency(s.valor)}` : ""}
                    </SelectItem>
                  ))
                )}
              </SelectContent>
            </Select>
          </div>

          {/* Datas */}
          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-2">
              <Label className="font-primary">Início *</Label>
              <Input
                type="date"
                value={dataInicio}
                onChange={(e) => handleDataInicioChange(e.target.value)}
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

          {/* Seletor de dias da semana para Pilates com frequência */}
          {showDayPicker && (
            <div className="space-y-2">
              <Label className="font-primary">
                Dias da semana
                {selectedServico?.quantidade
                  ? ` (${selectedServico.quantidade}x/semana)`
                  : ""}
                {" "}*
              </Label>
              <div className="flex gap-1.5 flex-wrap">
                {DIAS_SEMANA.map(({ value, label }) => {
                  const ativo = diasSemana.includes(value)
                  return (
                    <button
                      key={value}
                      type="button"
                      onClick={() => handleToggleDia(value)}
                      className={
                        "px-3 py-1.5 rounded-md text-xs font-primary border transition-colors " +
                        (ativo
                          ? "bg-primary text-primary-foreground border-primary"
                          : "bg-background text-foreground border-input hover:bg-muted")
                      }
                    >
                      {label}
                    </button>
                  )
                })}
              </div>
              {diasSemana.length > 0 && selectedServico?.validadeDias && dataInicio && (
                <p className="text-xs text-muted-foreground font-secondary">
                  {sessoesContratadas} aulas no período
                  {selectedServico.planoNome ? ` · ${selectedServico.planoNome}` : ""}
                  {` · ${selectedServico.validadeDias} dias`}
                </p>
              )}
            </div>
          )}

          {/* Sessões / Aulas e Valor */}
          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-2">
              <Label className="font-primary">{sessoesLabel} *</Label>
              <Input
                type="number"
                min="1"
                value={sessoesContratadas}
                onChange={(e) => setSessoesContratadas(e.target.value)}
                required
                placeholder={showDayPicker ? "Selecione os dias" : "1"}
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
                placeholder="0,00"
                className="font-secondary"
              />
            </div>
          </div>

          {/* Observações */}
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
