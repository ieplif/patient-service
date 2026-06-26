import { useMemo, useState } from "react"
import { useQuery } from "@tanstack/react-query"
import { addWeeks, startOfWeek, endOfWeek, format, parseISO } from "date-fns"
import { ptBR } from "date-fns/locale"
import { MessageCircle, Copy, Printer } from "lucide-react"
import { getAgendamentos } from "@/api/agendamentos"
import type { Agendamento } from "@/types"
import { shortenName } from "@/lib/names"
import { useToast } from "@/hooks/use-toast"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from "@/components/ui/dialog"

interface Props {
  open: boolean
  onOpenChange: (open: boolean) => void
}

function capitalize(s: string) {
  return s.charAt(0).toUpperCase() + s.slice(1)
}

// É uma sessão de Fisioterapia Pélvica? (para a versão "sem pélvica")
function isPelvica(ag: Agendamento) {
  return ag.servicoDescricao.toLowerCase().includes("pélvica") || ag.servicoDescricao.toLowerCase().includes("pelvica")
}

/**
 * Serviço resumido para o resumo da semana: usa só a atividade (sem o plano após " - ");
 * Pilates Clássico/Funcional viram apenas "Pilates"; demais mantêm a especificação
 * (ex.: "Fisioterapia Pélvica", "Drenagem", "Abdômen 360°").
 */
function servicoResumido(servicoDescricao: string): string {
  const atividade = servicoDescricao.split(" - ")[0].trim()
  if (atividade.toLowerCase().startsWith("pilates")) return "Pilates"
  return atividade
}

/** Monta o texto do resumo (formato WhatsApp, com *negrito*). */
function montarTexto(lista: Agendamento[], periodo: string): string {
  if (lista.length === 0) return `📅 *Agenda da semana* (${periodo})\n\nNenhum agendamento.`
  const linhas: string[] = [`📅 *Agenda da semana* (${periodo})`]
  let diaAtual = ""
  for (const ag of lista) {
    const d = parseISO(ag.dataHora)
    const dia = capitalize(format(d, "EEEE, dd/MM", { locale: ptBR }))
    if (dia !== diaAtual) {
      diaAtual = dia
      linhas.push("")
      linhas.push(`*${dia}*`)
    }
    linhas.push(`${format(d, "HH:mm")} — ${shortenName(ag.pacienteNome)} — ${servicoResumido(ag.servicoDescricao)}`)
  }
  return linhas.join("\n")
}

export function AgendaSemanalDialog({ open, onOpenChange }: Props) {
  const { toast } = useToast()
  const [versao, setVersao] = useState<"completa" | "sem">("completa")

  // Próxima semana (segunda a domingo).
  const { inicio, fim, periodo } = useMemo(() => {
    const base = addWeeks(new Date(), 1)
    const ini = startOfWeek(base, { weekStartsOn: 1 })
    const f = endOfWeek(base, { weekStartsOn: 1 })
    return {
      inicio: format(ini, "yyyy-MM-dd"),
      fim: format(f, "yyyy-MM-dd"),
      periodo: `${format(ini, "dd/MM")} a ${format(f, "dd/MM")}`,
    }
  }, [])

  const { data, isLoading } = useQuery({
    queryKey: ["agenda-semanal", inicio, fim],
    queryFn: () => getAgendamentos({ dataInicio: inicio, dataFim: fim, size: 500, sort: "dataHora,asc" }),
    enabled: open,
  })

  const ativos = useMemo(
    () => (data?.content ?? []).filter((a) => a.status === "AGENDADO" || a.status === "CONFIRMADO"),
    [data]
  )

  const textoCompleta = useMemo(() => montarTexto(ativos, periodo), [ativos, periodo])
  const textoSemPelvica = useMemo(() => montarTexto(ativos.filter((a) => !isPelvica(a)), periodo), [ativos, periodo])
  const textoAtual = versao === "completa" ? textoCompleta : textoSemPelvica

  function compartilharWhatsapp() {
    window.open(`https://wa.me/?text=${encodeURIComponent(textoAtual)}`, "_blank")
  }

  async function copiar() {
    await navigator.clipboard.writeText(textoAtual)
    toast({ title: "Copiado", description: "Resumo copiado para a área de transferência." })
  }

  function imprimir() {
    const win = window.open("", "_blank", "width=720,height=900")
    if (!win) return
    const html = textoAtual
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/\*(.+?)\*/g, "<strong>$1</strong>")
      .replace(/\n/g, "<br>")
    win.document.write(
      `<html><head><title>Agenda da semana</title></head>
       <body style="font-family: Arial, sans-serif; font-size: 14px; line-height: 1.5; padding: 24px;">
       ${html}</body></html>`
    )
    win.document.close()
    win.focus()
    win.print()
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="w-full sm:max-w-lg">
        <DialogHeader>
          <DialogTitle className="font-primary">Agenda da semana</DialogTitle>
          <DialogDescription className="font-secondary">
            Próxima semana ({periodo}) — Agendado e Confirmado. Escolha a versão e compartilhe.
          </DialogDescription>
        </DialogHeader>

        {/* Alternância entre as duas versões */}
        <div className="flex gap-2">
          <Button
            type="button"
            variant={versao === "completa" ? "default" : "outline"}
            size="sm"
            className="font-primary"
            onClick={() => setVersao("completa")}
          >
            Completa
          </Button>
          <Button
            type="button"
            variant={versao === "sem" ? "default" : "outline"}
            size="sm"
            className="font-primary"
            onClick={() => setVersao("sem")}
          >
            Sem Fisioterapia Pélvica
          </Button>
        </div>

        {isLoading ? (
          <Skeleton className="h-64 w-full" />
        ) : (
          <pre className="max-h-72 overflow-y-auto whitespace-pre-wrap rounded-md border border-border/60 bg-muted/30 p-3 text-sm font-secondary">
            {textoAtual.replace(/\*/g, "")}
          </pre>
        )}

        <div className="flex flex-wrap gap-2">
          <Button onClick={compartilharWhatsapp} className="font-primary gap-2 bg-[#25D366] hover:bg-[#1ebe57] text-white">
            <MessageCircle className="h-4 w-4" /> Compartilhar no WhatsApp
          </Button>
          <Button variant="outline" onClick={copiar} className="font-primary gap-2">
            <Copy className="h-4 w-4" /> Copiar
          </Button>
          <Button variant="outline" onClick={imprimir} className="font-primary gap-2">
            <Printer className="h-4 w-4" /> Imprimir / PDF
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  )
}
