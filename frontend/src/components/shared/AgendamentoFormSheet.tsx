import { useEffect, useState } from "react"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { CalendarPlus, Clock } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetDescription,
} from "@/components/ui/sheet"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { useToast } from "@/hooks/use-toast"
import { createAgendamento, updateAgendamento } from "@/api/agendamentos"
import { getPatients } from "@/api/patients"
import { getProfissionais } from "@/api/profissionais"
import { getServicos } from "@/api/servicos"
import type { Agendamento } from "@/types"

interface AgendamentoFormSheetProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  agendamento?: Agendamento | null  // se fornecido, modo reagendar
}

function toLocalDateTimeInputs(iso: string) {
  const d = new Date(iso)
  const date = d.toISOString().slice(0, 10)
  const time = d.toTimeString().slice(0, 5)
  return { date, time }
}

export function AgendamentoFormSheet({ open, onOpenChange, agendamento }: AgendamentoFormSheetProps) {
  const { toast } = useToast()
  const queryClient = useQueryClient()
  const isReagendar = !!agendamento

  const [pacienteId, setPacienteId] = useState("")
  const [profissionalId, setProfissionalId] = useState("")
  const [servicoId, setServicoId] = useState("")
  const [date, setDate] = useState("")
  const [time, setTime] = useState("")
  const [duracaoMinutos, setDuracaoMinutos] = useState("")
  const [observacoes, setObservacoes] = useState("")

  const { data: pacientes } = useQuery({
    queryKey: ["patients-select"],
    queryFn: () => getPatients({ size: 200, sort: "nomeCompleto,asc" }),
    enabled: open && !isReagendar,
  })

  const { data: profissionais } = useQuery({
    queryKey: ["profissionais-select"],
    queryFn: () => getProfissionais({ size: 100, sort: "nome,asc" }),
    enabled: open && !isReagendar,
  })

  const { data: servicos } = useQuery({
    queryKey: ["servicos-select"],
    queryFn: getServicos,
    enabled: open && !isReagendar,
  })

  useEffect(() => {
    if (agendamento) {
      const { date: d, time: t } = toLocalDateTimeInputs(agendamento.dataHora)
      setDate(d)
      setTime(t)
      setDuracaoMinutos(agendamento.duracaoMinutos?.toString() ?? "60")
      setObservacoes(agendamento.observacoes ?? "")
    } else {
      setPacienteId("")
      setProfissionalId("")
      setServicoId("")
      setDate("")
      setTime("")
      setDuracaoMinutos("60")
      setObservacoes("")
    }
  }, [agendamento, open])

  const mutation = useMutation({
    mutationFn: () => {
      const dataHora = `${date}T${time}:00`
      if (isReagendar) {
        return updateAgendamento(agendamento!.id, {
          dataHora,
          duracaoMinutos: duracaoMinutos ? parseInt(duracaoMinutos) : undefined,
          observacoes: observacoes || undefined,
        })
      }
      return createAgendamento({
        pacienteId,
        profissionalId,
        servicoId,
        dataHora,
        duracaoMinutos: duracaoMinutos ? parseInt(duracaoMinutos) : undefined,
        observacoes: observacoes || undefined,
      })
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["agendamentos"] })
      toast({
        title: isReagendar ? "Agendamento reagendado" : "Agendamento criado",
        description: isReagendar
          ? "O horário foi atualizado com sucesso."
          : "Novo agendamento registrado com sucesso.",
      })
      onOpenChange(false)
    },
    onError: (err: unknown) => {
      const data = (err as { response?: { data?: { mensagem?: string; message?: string } } })?.response?.data
      const msg = data?.mensagem || data?.message || "Verifique os dados e tente novamente."
      toast({ title: "Erro", description: msg, variant: "destructive" })
    },
  })

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    mutation.mutate()
  }

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent className="w-full sm:max-w-lg overflow-y-auto">
        <SheetHeader className="mb-6">
          <SheetTitle className="flex items-center gap-2 font-primary">
            {isReagendar
              ? <Clock className="h-5 w-5 text-primary" />
              : <CalendarPlus className="h-5 w-5 text-primary" />}
            {isReagendar ? "Reagendar" : "Novo Agendamento"}
          </SheetTitle>
          <SheetDescription className="font-secondary">
            {isReagendar
              ? `Altere a data e hora de ${agendamento!.pacienteNome}.`
              : "Preencha os dados para criar um novo agendamento."}
          </SheetDescription>
        </SheetHeader>

        <form onSubmit={handleSubmit} className="space-y-4">
          {!isReagendar && (
            <>
              <div className="space-y-1.5">
                <Label className="font-primary text-sm">Paciente *</Label>
                <Select value={pacienteId} onValueChange={setPacienteId} required>
                  <SelectTrigger className="font-secondary text-sm">
                    <SelectValue placeholder="Selecione o paciente" />
                  </SelectTrigger>
                  <SelectContent>
                    {pacientes?.content.map((p) => (
                      <SelectItem key={p.id} value={p.id} className="font-secondary text-sm">
                        {p.nomeCompleto}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-1.5">
                <Label className="font-primary text-sm">Profissional *</Label>
                <Select value={profissionalId} onValueChange={setProfissionalId} required>
                  <SelectTrigger className="font-secondary text-sm">
                    <SelectValue placeholder="Selecione o profissional" />
                  </SelectTrigger>
                  <SelectContent>
                    {profissionais?.content.map((p) => (
                      <SelectItem key={p.id} value={p.id} className="font-secondary text-sm">
                        {p.nome}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-1.5">
                <Label className="font-primary text-sm">Serviço *</Label>
                <Select value={servicoId} onValueChange={setServicoId} required>
                  <SelectTrigger className="font-secondary text-sm">
                    <SelectValue placeholder="Selecione o serviço" />
                  </SelectTrigger>
                  <SelectContent>
                    {servicos?.map((s) => (
                      <SelectItem key={s.id} value={s.id} className="font-secondary text-sm">
                        {s.descricao}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            </>
          )}

          {isReagendar && (
            <div className="rounded-md bg-muted/40 border border-border/50 p-3 space-y-0.5">
              <p className="text-xs font-primary text-muted-foreground uppercase tracking-wide">Serviço</p>
              <p className="text-sm font-secondary text-foreground">{agendamento!.servicoDescricao}</p>
              <p className="text-xs font-primary text-muted-foreground uppercase tracking-wide mt-2">Profissional</p>
              <p className="text-sm font-secondary text-foreground">{agendamento!.profissionalNome}</p>
            </div>
          )}

          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-1.5">
              <Label htmlFor="ag-date" className="font-primary text-sm">Data *</Label>
              <Input
                id="ag-date"
                type="date"
                value={date}
                onChange={(e) => setDate(e.target.value)}
                required
                className="font-secondary"
              />
            </div>
            <div className="space-y-1.5">
              <Label className="font-primary text-sm">Horário *</Label>
              <Select value={time} onValueChange={setTime} required>
                <SelectTrigger className="font-secondary text-sm">
                  <SelectValue placeholder="Hora" />
                </SelectTrigger>
                <SelectContent>
                  {Array.from({ length: 30 }, (_, i) => {
                    const h = Math.floor(i / 2) + 7
                    const m = i % 2 === 0 ? "00" : "30"
                    const val = `${h.toString().padStart(2, "0")}:${m}`
                    return (
                      <SelectItem key={val} value={val} className="font-secondary text-sm">
                        {val}
                      </SelectItem>
                    )
                  })}
                </SelectContent>
              </Select>
            </div>
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="ag-duracao" className="font-primary text-sm">Duração (minutos)</Label>
            <Input
              id="ag-duracao"
              type="number"
              min={15}
              step={15}
              value={duracaoMinutos}
              onChange={(e) => setDuracaoMinutos(e.target.value)}
              placeholder="60"
              className="font-secondary"
            />
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="ag-obs" className="font-primary text-sm">Observações</Label>
            <Input
              id="ag-obs"
              value={observacoes}
              onChange={(e) => setObservacoes(e.target.value)}
              placeholder="Observações sobre o agendamento"
              className="font-secondary"
            />
          </div>

          <div className="flex gap-2 pt-4">
            <Button type="button" variant="outline" className="flex-1 font-primary" onClick={() => onOpenChange(false)}>
              Cancelar
            </Button>
            <Button type="submit" className="flex-1 font-primary" disabled={mutation.isPending}>
              {mutation.isPending ? "Salvando..." : isReagendar ? "Confirmar reagendamento" : "Criar agendamento"}
            </Button>
          </div>
        </form>
      </SheetContent>
    </Sheet>
  )
}
