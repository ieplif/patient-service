import { useState } from "react"
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query"
import { Clock, Plus, Trash2 } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Label } from "@/components/ui/label"
import { Skeleton } from "@/components/ui/skeleton"
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
import { getHorariosByProfissional, createHorario, deleteHorario } from "@/api/horarios"
import type { Profissional, HorarioDisponivel } from "@/types"

interface HorarioSheetProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  profissional: Profissional | null
}

const DIAS_SEMANA: { value: string; label: string; short: string }[] = [
  { value: "MONDAY", label: "Segunda-feira", short: "Seg" },
  { value: "TUESDAY", label: "Terca-feira", short: "Ter" },
  { value: "WEDNESDAY", label: "Quarta-feira", short: "Qua" },
  { value: "THURSDAY", label: "Quinta-feira", short: "Qui" },
  { value: "FRIDAY", label: "Sexta-feira", short: "Sex" },
  { value: "SATURDAY", label: "Sabado", short: "Sab" },
]

const HORAS: string[] = []
for (let h = 6; h <= 21; h++) {
  HORAS.push(`${String(h).padStart(2, "0")}:00`)
  if (h < 21) HORAS.push(`${String(h).padStart(2, "0")}:30`)
}

function diaLabel(dia: string): string {
  return DIAS_SEMANA.find((d) => d.value === dia)?.label ?? dia
}


function diaOrder(dia: string): number {
  const idx = DIAS_SEMANA.findIndex((d) => d.value === dia)
  return idx >= 0 ? idx : 99
}

export function HorarioSheet({ open, onOpenChange, profissional }: HorarioSheetProps) {
  const { toast } = useToast()
  const queryClient = useQueryClient()

  const [newDia, setNewDia] = useState("MONDAY")
  const [newInicio, setNewInicio] = useState("08:00")
  const [newFim, setNewFim] = useState("17:00")

  const { data: horarios, isLoading } = useQuery({
    queryKey: ["horarios", profissional?.id],
    queryFn: () => getHorariosByProfissional(profissional!.id),
    enabled: !!profissional?.id && open,
  })

  const createMutation = useMutation({
    mutationFn: () =>
      createHorario({
        profissionalId: profissional!.id,
        diaSemana: newDia,
        horaInicio: newInicio,
        horaFim: newFim,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["horarios", profissional?.id] })
      toast({ title: "Horario adicionado", description: `${diaLabel(newDia)} ${newInicio} - ${newFim}` })
    },
    onError: (err: unknown) => {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        "Verifique os dados e tente novamente."
      toast({ title: "Erro", description: msg, variant: "destructive" })
    },
  })

  const deleteMutation = useMutation({
    mutationFn: deleteHorario,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["horarios", profissional?.id] })
      toast({ title: "Horario removido" })
    },
    onError: () => {
      toast({ title: "Erro", description: "Nao foi possivel remover o horario.", variant: "destructive" })
    },
  })

  const sorted = [...(horarios ?? [])].sort((a, b) => {
    const da = diaOrder(a.diaSemana)
    const db = diaOrder(b.diaSemana)
    if (da !== db) return da - db
    return a.horaInicio.localeCompare(b.horaInicio)
  })

  // Group by day for visual clarity
  const byDay = DIAS_SEMANA.map((dia) => ({
    ...dia,
    slots: sorted.filter((h) => h.diaSemana === dia.value),
  })).filter((d) => d.slots.length > 0)

  function formatTime(t: string) {
    return t.slice(0, 5)
  }

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent className="w-full sm:max-w-lg overflow-y-auto">
        <SheetHeader className="mb-6">
          <SheetTitle className="flex items-center gap-2 font-primary">
            <Clock className="h-5 w-5 text-primary" />
            Horarios Disponiveis
          </SheetTitle>
          <SheetDescription className="font-secondary">
            Grade semanal de {profissional?.nome ?? "profissional"}
          </SheetDescription>
        </SheetHeader>

        {/* Current schedule */}
        {isLoading ? (
          <div className="space-y-2">
            {Array.from({ length: 5 }).map((_, i) => (
              <Skeleton key={i} className="h-10 w-full" />
            ))}
          </div>
        ) : byDay.length === 0 ? (
          <p className="text-sm text-muted-foreground font-secondary py-4 text-center">
            Nenhum horario cadastrado
          </p>
        ) : (
          <div className="space-y-3 mb-6">
            {byDay.map((dia) => (
              <div key={dia.value} className="rounded-lg border border-border/50 p-3">
                <p className="text-sm font-semibold font-primary text-foreground mb-2">
                  {dia.label}
                </p>
                <div className="space-y-1.5">
                  {dia.slots.map((slot) => (
                    <div
                      key={slot.id}
                      className="flex items-center justify-between bg-muted/30 rounded-md px-3 py-1.5"
                    >
                      <span className="text-sm font-secondary text-foreground">
                        {formatTime(slot.horaInicio)} — {formatTime(slot.horaFim)}
                      </span>
                      <Button
                        variant="ghost"
                        size="icon"
                        className="h-7 w-7 text-muted-foreground hover:text-destructive"
                        onClick={() => deleteMutation.mutate(slot.id)}
                        disabled={deleteMutation.isPending}
                      >
                        <Trash2 className="h-3.5 w-3.5" />
                      </Button>
                    </div>
                  ))}
                </div>
              </div>
            ))}
          </div>
        )}

        {/* Add new slot */}
        <div className="border-t border-border/50 pt-4">
          <p className="text-sm font-semibold font-primary mb-3">Adicionar horario</p>
          <div className="space-y-3">
            <div className="space-y-1.5">
              <Label className="font-primary text-sm">Dia da semana</Label>
              <Select value={newDia} onValueChange={setNewDia}>
                <SelectTrigger className="font-secondary">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {DIAS_SEMANA.map((d) => (
                    <SelectItem key={d.value} value={d.value} className="font-secondary">
                      {d.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1.5">
                <Label className="font-primary text-sm">Inicio</Label>
                <Select value={newInicio} onValueChange={setNewInicio}>
                  <SelectTrigger className="font-secondary">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {HORAS.map((h) => (
                      <SelectItem key={h} value={h} className="font-secondary">
                        {h}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-1.5">
                <Label className="font-primary text-sm">Fim</Label>
                <Select value={newFim} onValueChange={setNewFim}>
                  <SelectTrigger className="font-secondary">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {HORAS.map((h) => (
                      <SelectItem key={h} value={h} className="font-secondary">
                        {h}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            </div>

            <Button
              className="w-full font-primary gap-2"
              onClick={() => createMutation.mutate()}
              disabled={createMutation.isPending || newInicio >= newFim}
            >
              <Plus className="h-4 w-4" />
              {createMutation.isPending ? "Adicionando..." : "Adicionar"}
            </Button>
            {newInicio >= newFim && (
              <p className="text-xs text-destructive font-secondary">
                Horario de inicio deve ser anterior ao fim
              </p>
            )}
          </div>
        </div>
      </SheetContent>
    </Sheet>
  )
}
