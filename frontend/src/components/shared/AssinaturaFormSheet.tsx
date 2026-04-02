import { useState, useEffect } from "react"
import { useQuery } from "@tanstack/react-query"
import { Sheet, SheetContent, SheetHeader, SheetTitle, SheetDescription } from "@/components/ui/sheet"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { getPatients } from "@/api/patients"
import { getServicos } from "@/api/servicos"
import { getProfissionais } from "@/api/profissionais"
import type { Assinatura, Servico } from "@/types"

export interface HorarioFixo {
  dia: string
  horario: string
}

export interface AgendamentoIndividual {
  dataHora: string
  data: string
  horario: string
}

export interface AssinaturaFormData {
  pacienteId: string
  servicoId: string
  dataInicio: string
  dataVencimento?: string
  sessoesContratadas: number
  valor: number
  observacoes?: string
  profissionalId?: string
  horariosFixos?: HorarioFixo[]
  agendamentosIndividuais?: AgendamentoIndividual[]
}

interface AssinaturaFormSheetProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  onSubmit: (data: AssinaturaFormData) => void
  assinatura?: Assinatura | null
  isPending?: boolean
}

const DIAS_SEMANA = [
  { value: "1", label: "Segunda-feira" },
  { value: "2", label: "Terça-feira" },
  { value: "3", label: "Quarta-feira" },
  { value: "4", label: "Quinta-feira" },
  { value: "5", label: "Sexta-feira" },
  { value: "6", label: "Sábado" },
]

const DIAS_SEMANA_CURTO: Record<string, string> = {
  "1": "Seg", "2": "Ter", "3": "Qua", "4": "Qui", "5": "Sex", "6": "Sáb",
}

const HORARIOS_DISPONIVEIS = Array.from({ length: 28 }, (_, i) => {
  const h = Math.floor(i / 2) + 7 // 07:00 até 20:30
  const m = i % 2 === 0 ? "00" : "30"
  return `${String(h).padStart(2, "0")}:${m}`
})

function isPilatesService(servico: Servico | null): boolean {
  return !!servico?.atividadeNome?.toLowerCase().includes("pilates")
}

function isFrequenciaService(servico: Servico | null): boolean {
  return !!servico?.unidadeServico?.toLowerCase().match(/frequ[eê]ncia/)
}

function calcularAulas(dataInicio: string, validadeDias: number, diasSemana: number[]): number {
  if (!diasSemana.length || !dataInicio) return 0
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
  const [horariosFixos, setHorariosFixos] = useState<HorarioFixo[]>([])
  const [profissionalId, setProfissionalId] = useState("")
  const [agendamentos, setAgendamentos] = useState<AgendamentoIndividual[]>([])

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

  const { data: profissionaisData, isLoading: loadingProfissionais } = useQuery({
    queryKey: ["profissionais-all"],
    queryFn: () => getProfissionais({ page: 0, size: 200 }),
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
      setHorariosFixos([])
      setProfissionalId("")
      setAgendamentos([])
    } else if (open) {
      setPacienteId("")
      setServicoId("")
      setDataInicio(new Date().toISOString().split("T")[0])
      setDataVencimento("")
      setSessoesContratadas("")
      setValor("")
      setObservacoes("")
      setHorariosFixos([])
      setProfissionalId("")
      setAgendamentos([])
    }
  }, [open, assinatura])

  const isEditing = !!assinatura
  const selectedServico = servicosData?.find(s => s.id === servicoId) ?? null
  const isPilates = isPilatesService(selectedServico)
  const isFrequencia = isFrequenciaService(selectedServico)
  const showHorarios = isPilates && isFrequencia
  const showAgendamentosIndividuais = !isEditing && !showHorarios && servicoId && Number(sessoesContratadas) > 0
  const sessoesLabel = isPilates ? "Aulas contratadas" : "Sessões contratadas"

  // Filter professionals by the selected service's activity
  const profissionaisFiltrados = selectedServico?.atividadeId
    ? profissionaisData?.content.filter(p =>
        p.atividades.some(a => a.id === selectedServico.atividadeId)
      )
    : []

  function recalcularAulas(horarios: HorarioFixo[], inicio: string, servico: Servico | null) {
    if (!servico?.validadeDias || !inicio) return
    const diasSelecionados = horarios
      .filter(h => h.dia !== "")
      .map(h => Number(h.dia))
    const aulas = calcularAulas(inicio, servico.validadeDias, diasSelecionados)
    setSessoesContratadas(diasSelecionados.length > 0 ? String(aulas) : "")
  }

  function handleServicoChange(id: string) {
    setServicoId(id)
    setProfissionalId("")
    setAgendamentos([])
    if (isEditing) return

    const servico = servicosData?.find(s => s.id === id)
    if (!servico) return

    if (servico.valor != null) {
      setValor(String(servico.valor))
    }

    const pilates = isPilatesService(servico)
    const freq = isFrequenciaService(servico)

    if (pilates && freq) {
      const qtd = servico.quantidade ?? 1
      setHorariosFixos(Array.from({ length: qtd }, () => ({ dia: "", horario: "" })))
      setSessoesContratadas("")
      setAgendamentos([])
    } else {
      const qtd = servico.quantidade ?? 1
      setSessoesContratadas(String(qtd))
      setHorariosFixos([])
      setAgendamentos(Array.from({ length: qtd }, () => ({ dataHora: "", data: "", horario: "" })))
    }

    if (servico.validadeDias && dataInicio) {
      setDataVencimento(addDaysToDate(dataInicio, servico.validadeDias))
    }
  }

  function handleDataInicioChange(data: string) {
    setDataInicio(data)
    if (!selectedServico?.validadeDias || !data) return

    setDataVencimento(addDaysToDate(data, selectedServico.validadeDias))

    if (showHorarios && horariosFixos.length > 0) {
      recalcularAulas(horariosFixos, data, selectedServico)
    }
  }

  function handleSessoesChange(value: string) {
    setSessoesContratadas(value)
    if (!showHorarios && !isEditing) {
      const n = Math.max(0, Math.min(Number(value) || 0, 52))
      setAgendamentos(prev => {
        if (n === prev.length) return prev
        if (n > prev.length) return [...prev, ...Array.from({ length: n - prev.length }, () => ({ dataHora: "", data: "", horario: "" }))]
        return prev.slice(0, n)
      })
    }
  }

  function handleAgendamentoChange(index: number, field: "data" | "horario", value: string) {
    setAgendamentos(prev => prev.map((a, i) => {
      if (i !== index) return a
      const updated = { ...a, [field]: value }
      updated.dataHora = updated.data && updated.horario ? `${updated.data}T${updated.horario}` : ""
      return updated
    }))
  }

  function handleHorarioChange(index: number, field: "dia" | "horario", value: string) {
    const novos = horariosFixos.map((h, i) =>
      i === index ? { ...h, [field]: value } : h
    )
    setHorariosFixos(novos)

    if (field === "dia") {
      recalcularAulas(novos, dataInicio, selectedServico)
    }
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()

    // Build horários text for observações
    let obsFinais = observacoes
    if (showHorarios && horariosFixos.some(h => h.dia && h.horario)) {
      const horariosTexto = horariosFixos
        .filter(h => h.dia && h.horario)
        .map((h, i) => `Horário ${i + 1}: ${DIAS_SEMANA_CURTO[h.dia] ?? h.dia} às ${h.horario}`)
        .join(" | ")
      obsFinais = obsFinais
        ? `${obsFinais}\n${horariosTexto}`
        : horariosTexto
    }

    onSubmit({
      pacienteId,
      servicoId,
      dataInicio,
      dataVencimento: dataVencimento || undefined,
      sessoesContratadas: Number(sessoesContratadas),
      valor: Number(valor),
      observacoes: obsFinais || undefined,
      profissionalId: profissionalId || undefined,
      horariosFixos: showHorarios
        ? horariosFixos.filter(h => h.dia && h.horario)
        : undefined,
      agendamentosIndividuais: showAgendamentosIndividuais
        ? agendamentos.filter(a => a.dataHora)
        : undefined,
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

          {/* Profissional — shown for Pilates with frequency or non-Pilates with sessions */}
          {(showHorarios || showAgendamentosIndividuais) && (
            <div className="space-y-2">
              <Label className="font-primary">Profissional *</Label>
              <Select value={profissionalId} onValueChange={setProfissionalId}>
                <SelectTrigger className="font-secondary">
                  <SelectValue placeholder="Selecione o profissional" />
                </SelectTrigger>
                <SelectContent>
                  {loadingProfissionais ? (
                    <div className="py-2 px-3 text-sm text-muted-foreground">Carregando...</div>
                  ) : !profissionaisFiltrados?.length ? (
                    <div className="py-2 px-3 text-sm text-muted-foreground">
                      Nenhum profissional encontrado para esta atividade
                    </div>
                  ) : (
                    profissionaisFiltrados.map((p) => (
                      <SelectItem key={p.id} value={p.id} className="font-secondary">
                        {p.nome}
                      </SelectItem>
                    ))
                  )}
                </SelectContent>
              </Select>
            </div>
          )}

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

          {/* Horários fixos for Pilates with frequency */}
          {showHorarios && horariosFixos.length > 0 && (
            <div className="space-y-3">
              <Label className="font-primary">
                Horários fixos ({selectedServico?.quantidade}x/semana) *
              </Label>
              {horariosFixos.map((h, i) => (
                <div key={i} className="flex items-center gap-2">
                  <span className="text-xs text-muted-foreground font-secondary shrink-0 w-6">
                    {i + 1}.
                  </span>
                  <Select
                    value={h.dia}
                    onValueChange={(v) => handleHorarioChange(i, "dia", v)}
                  >
                    <SelectTrigger className="font-secondary flex-1">
                      <SelectValue placeholder="Dia" />
                    </SelectTrigger>
                    <SelectContent>
                      {DIAS_SEMANA.map(d => (
                        <SelectItem key={d.value} value={d.value} className="font-secondary">
                          {d.label}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  <span className="text-xs text-muted-foreground font-secondary">às</span>
                  <Input
                    type="time"
                    value={h.horario}
                    onChange={(e) => handleHorarioChange(i, "horario", e.target.value)}
                    className="font-secondary w-28"
                  />
                </div>
              ))}
              {horariosFixos.some(h => h.dia) && selectedServico?.validadeDias && dataInicio && (
                <p className="text-xs text-muted-foreground font-secondary">
                  {sessoesContratadas} aulas no período
                  {selectedServico.planoNome ? ` · ${selectedServico.planoNome}` : ""}
                  {` · ${selectedServico.validadeDias} dias`}
                </p>
              )}
            </div>
          )}

          {/* Agendamentos individuais for non-Pilates services */}
          {showAgendamentosIndividuais && agendamentos.length > 0 && profissionalId && (
            <div className="space-y-3">
              <Label className="font-primary">
                Agendamentos ({agendamentos.length} {agendamentos.length === 1 ? "sessão" : "sessões"})
              </Label>
              <div className="max-h-[240px] overflow-y-auto space-y-2 pr-1">
                {agendamentos.map((a, i) => (
                  <div key={i} className="flex items-center gap-2">
                    <span className="text-xs text-muted-foreground font-secondary shrink-0 w-6">
                      {i + 1}.
                    </span>
                    <Input
                      type="date"
                      value={a.data}
                      onChange={(e) => handleAgendamentoChange(i, "data", e.target.value)}
                      className="font-secondary flex-1"
                    />
                    <Select value={a.horario} onValueChange={(v) => handleAgendamentoChange(i, "horario", v)}>
                      <SelectTrigger className="font-secondary w-[100px]">
                        <SelectValue placeholder="Hora" />
                      </SelectTrigger>
                      <SelectContent>
                        {HORARIOS_DISPONIVEIS.map(h => (
                          <SelectItem key={h} value={h} className="font-secondary">{h}</SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>
                ))}
              </div>
              <p className="text-xs text-muted-foreground font-secondary">
                {agendamentos.filter(a => a.dataHora).length} de {agendamentos.length} agendados
              </p>
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
                onChange={(e) => handleSessoesChange(e.target.value)}
                required
                placeholder={showHorarios ? "Selecione os dias" : "1"}
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
              disabled={
                !pacienteId || !servicoId || !dataInicio || !sessoesContratadas || !valor
                || ((showHorarios || showAgendamentosIndividuais) && !profissionalId)
                || isPending
              }
            >
              {isPending ? "Salvando..." : isEditing ? "Salvar" : "Criar Assinatura"}
            </Button>
          </div>
        </form>
      </SheetContent>
    </Sheet>
  )
}
