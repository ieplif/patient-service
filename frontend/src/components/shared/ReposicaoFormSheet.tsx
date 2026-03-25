import { useEffect, useState } from "react"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { RefreshCw } from "lucide-react"
import { format } from "date-fns"
import { ptBR } from "date-fns/locale"
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
import { criarReposicao } from "@/api/agendamentos"
import { getProfissionais } from "@/api/profissionais"
import type { Agendamento } from "@/types"

interface ReposicaoFormSheetProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  agendamento: Agendamento | null
}

export function ReposicaoFormSheet({ open, onOpenChange, agendamento }: ReposicaoFormSheetProps) {
  const { toast } = useToast()
  const queryClient = useQueryClient()

  const [profissionalId, setProfissionalId] = useState("")
  const [date, setDate] = useState("")
  const [time, setTime] = useState("")
  const [duracaoMinutos, setDuracaoMinutos] = useState("")
  const [observacoes, setObservacoes] = useState("")

  const { data: profissionais } = useQuery({
    queryKey: ["profissionais-select"],
    queryFn: () => getProfissionais({ size: 100, sort: "nome,asc" }),
    enabled: open,
  })

  useEffect(() => {
    if (open) {
      setProfissionalId(agendamento?.profissionalId ?? "")
      setDate("")
      setTime("")
      setDuracaoMinutos(agendamento?.duracaoMinutos?.toString() ?? "60")
      setObservacoes("")
    }
  }, [agendamento, open])

  const mutation = useMutation({
    mutationFn: () => {
      if (!agendamento) throw new Error("Agendamento de origem não encontrado")
      const dataHora = `${date}T${time}:00`
      return criarReposicao({
        agendamentoOrigemId: agendamento.id,
        profissionalId,
        dataHora,
        duracaoMinutos: duracaoMinutos ? parseInt(duracaoMinutos) : undefined,
        observacoes: observacoes || undefined,
      })
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["agendamentos"] })
      toast({
        title: "Reposicao agendada com sucesso",
        description: "A aula de reposicao foi agendada.",
      })
      onOpenChange(false)
    },
    onError: (err: unknown) => {
      const data = (err as { response?: { data?: { mensagem?: string; message?: string } } })?.response?.data
      const msg = data?.mensagem || data?.message || "Erro ao agendar reposicao."
      toast({ title: "Erro", description: msg, variant: "destructive" })
    },
  })

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    mutation.mutate()
  }

  if (!agendamento) return null

  const dataLimiteFormatted = agendamento.dataLimiteReposicao
    ? format(new Date(agendamento.dataLimiteReposicao), "dd/MM/yyyy", { locale: ptBR })
    : null

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent className="w-full sm:max-w-lg overflow-y-auto">
        <SheetHeader className="mb-6">
          <SheetTitle className="flex items-center gap-2 font-primary">
            <RefreshCw className="h-5 w-5 text-violet-600" />
            Agendar Reposicao
          </SheetTitle>
          <SheetDescription className="font-secondary">
            Agende uma aula de reposicao para {agendamento.pacienteNome}.
          </SheetDescription>
        </SheetHeader>

        <div className="rounded-md bg-muted/40 border border-border/50 p-3 space-y-0.5 mb-4">
          <p className="text-xs font-primary text-muted-foreground uppercase tracking-wide">Paciente</p>
          <p className="text-sm font-secondary text-foreground">{agendamento.pacienteNome}</p>
          <p className="text-xs font-primary text-muted-foreground uppercase tracking-wide mt-2">Servico</p>
          <p className="text-sm font-secondary text-foreground">{agendamento.servicoDescricao}</p>
          {dataLimiteFormatted && (
            <>
              <p className="text-xs font-primary text-muted-foreground uppercase tracking-wide mt-2">Valido ate</p>
              <p className="text-sm font-secondary text-violet-600 font-medium">{dataLimiteFormatted}</p>
            </>
          )}
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
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

          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-1.5">
              <Label htmlFor="repo-date" className="font-primary text-sm">Data *</Label>
              <Input
                id="repo-date"
                type="date"
                value={date}
                onChange={(e) => setDate(e.target.value)}
                required
                className="font-secondary"
              />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="repo-time" className="font-primary text-sm">Horario *</Label>
              <Input
                id="repo-time"
                type="time"
                value={time}
                onChange={(e) => setTime(e.target.value)}
                required
                className="font-secondary"
              />
            </div>
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="repo-duracao" className="font-primary text-sm">Duracao (minutos)</Label>
            <Input
              id="repo-duracao"
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
            <Label htmlFor="repo-obs" className="font-primary text-sm">Observacoes</Label>
            <Input
              id="repo-obs"
              value={observacoes}
              onChange={(e) => setObservacoes(e.target.value)}
              placeholder="Observacoes sobre a reposicao"
              className="font-secondary"
            />
          </div>

          <div className="flex gap-2 pt-4">
            <Button type="button" variant="outline" className="flex-1 font-primary" onClick={() => onOpenChange(false)}>
              Cancelar
            </Button>
            <Button type="submit" className="flex-1 font-primary bg-violet-600 hover:bg-violet-700" disabled={mutation.isPending}>
              {mutation.isPending ? "Agendando..." : "Confirmar reposicao"}
            </Button>
          </div>
        </form>
      </SheetContent>
    </Sheet>
  )
}
