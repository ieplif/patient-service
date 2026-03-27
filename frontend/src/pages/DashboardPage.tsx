import { useQuery } from "@tanstack/react-query"
import { Users, Calendar, CreditCard, TrendingUp, Star } from "lucide-react"
import { format, startOfMonth } from "date-fns"
import { ptBR } from "date-fns/locale"
import { StatCard } from "@/components/shared/StatCard"
import { Badge } from "@/components/ui/badge"
import { Skeleton } from "@/components/ui/skeleton"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { getPatients } from "@/api/patients"
import { getAgendamentos } from "@/api/agendamentos"
import { getPagamentos } from "@/api/pagamentos"
import { getAssinaturas } from "@/api/assinaturas"
import { useAuthStore } from "@/store/authStore"
import type { StatusAgendamento } from "@/types"

const today = format(new Date(), "yyyy-MM-dd")
const monthStart = format(startOfMonth(new Date()), "yyyy-MM-dd")

const statusConfig: Record<StatusAgendamento, { label: string; className: string }> = {
  AGENDADO: {
    label: "Agendado",
    className: "bg-secondary/40 text-[hsl(202,40%,40%)] border-secondary/50",
  },
  CONFIRMADO: {
    label: "Confirmado",
    className: "bg-primary/15 text-primary border-primary/30",
  },
  CANCELADO: {
    label: "Cancelado",
    className: "bg-destructive/10 text-destructive border-destructive/20",
  },
  REALIZADO: {
    label: "Realizado",
    className: "bg-[hsl(var(--greenish-gray))/30] text-muted-foreground border-border",
  },
  NAO_COMPARECEU: {
    label: "Faltou",
    className: "bg-accent/15 text-accent border-accent/30",
  },
}

function formatCurrency(value: number) {
  return new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }).format(value)
}

function formatDateTime(value: string) {
  return format(new Date(value), "dd/MM/yyyy 'às' HH:mm", { locale: ptBR })
}

function TableSkeleton({ cols }: { cols: number }) {
  return (
    <div className="space-y-2">
      {Array.from({ length: 5 }).map((_, i) => (
        <div key={i} className="flex gap-4">
          {Array.from({ length: cols }).map((_, j) => (
            <Skeleton key={j} className="h-9 flex-1" />
          ))}
        </div>
      ))}
    </div>
  )
}

export function DashboardPage() {
  const { user } = useAuthStore()
  const isProfissional = user?.role === "ROLE_PROFISSIONAL"

  const { data: patientsData, isLoading: loadingPatients } = useQuery({
    queryKey: ["patients-count"],
    queryFn: () => getPatients({ size: 1 }),
  })

  const { data: agendamentosHoje, isLoading: loadingAgendamentosHoje } = useQuery({
    queryKey: ["agendamentos-hoje"],
    queryFn: () => getAgendamentos({ dataInicio: today, dataFim: today, size: 1 }),
  })

  const { data: pagamentosPendentes, isLoading: loadingPagPendentes } = useQuery({
    queryKey: ["pagamentos-pendentes-count"],
    queryFn: () => getPagamentos({ status: "PENDENTE", size: 1 }),
    enabled: !isProfissional,
  })

  const { data: pagamentosMes, isLoading: loadingReceitaMes } = useQuery({
    queryKey: ["receita-mes"],
    queryFn: () =>
      getPagamentos({ status: "PAGO", inicio: monthStart, fim: today, size: 1000 }),
    enabled: !isProfissional,
  })

  const { data: assinaturasAtivas, isLoading: loadingAssinaturas } = useQuery({
    queryKey: ["assinaturas-ativas-count"],
    queryFn: () => getAssinaturas({ status: "ATIVO", size: 1 }),
    enabled: !isProfissional,
  })

  const { data: proximosAgendamentos, isLoading: loadingProximos } = useQuery({
    queryKey: ["proximos-agendamentos"],
    queryFn: () => getAgendamentos({ dataInicio: today, sort: "dataHora,asc", size: 5 }),
  })

  const { data: pagamentosPendentesLista, isLoading: loadingPagLista } = useQuery({
    queryKey: ["pagamentos-pendentes-lista"],
    queryFn: () => getPagamentos({ status: "PENDENTE", size: 5 }),
    enabled: !isProfissional,
  })

  const receitaMes = pagamentosMes?.content.reduce((sum, p) => sum + p.valor, 0) ?? 0

  return (
    <div className="space-y-6 animate-fade-in">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold font-primary text-foreground tracking-tight">
          Dashboard
        </h1>
        <p className="text-sm text-muted-foreground font-secondary mt-0.5">
          Visao geral da Clinica Humaniza —{" "}
          {format(new Date(), "EEEE, dd 'de' MMMM 'de' yyyy", { locale: ptBR })}
        </p>
      </div>

      {/* Stat Cards */}
      <div className={`grid grid-cols-1 gap-4 sm:grid-cols-2 ${isProfissional ? "lg:grid-cols-2" : "lg:grid-cols-5"}`}>
        <StatCard
          title="Pacientes Ativos"
          value={patientsData?.totalElements}
          icon={Users}
          isLoading={loadingPatients}
          accent="sage"
        />
        <StatCard
          title="Agendamentos Hoje"
          value={agendamentosHoje?.totalElements}
          icon={Calendar}
          isLoading={loadingAgendamentosHoje}
          accent="blue"
        />
        {!isProfissional && (
          <StatCard
            title="Assinaturas Ativas"
            value={assinaturasAtivas?.totalElements}
            icon={Star}
            isLoading={loadingAssinaturas}
            accent="sage"
          />
        )}
        {!isProfissional && (
          <StatCard
            title="Pgtos Pendentes"
            value={pagamentosPendentes?.totalElements}
            icon={CreditCard}
            isLoading={loadingPagPendentes}
            accent="earth"
          />
        )}
        {!isProfissional && (
          <StatCard
            title="Receita do Mes"
            value={loadingReceitaMes ? undefined : formatCurrency(receitaMes)}
            icon={TrendingUp}
            isLoading={loadingReceitaMes}
            accent="beige"
          />
        )}
      </div>

      {/* Tables */}
      <div className={`grid gap-6 ${isProfissional ? "lg:grid-cols-1" : "lg:grid-cols-2"}`}>
        {/* Proximos Agendamentos */}
        <Card className="border border-border/60 shadow-soft">
          <CardHeader className="pb-3">
            <CardTitle className="text-base font-semibold font-primary text-foreground flex items-center gap-2">
              <span className="inline-block h-2 w-2 rounded-full bg-primary" />
              Proximos Agendamentos
            </CardTitle>
          </CardHeader>
          <CardContent>
            {loadingProximos ? (
              <TableSkeleton cols={4} />
            ) : (
              <div className="overflow-x-auto">
                <Table>
                  <TableHeader>
                    <TableRow className="border-border/50 hover:bg-transparent">
                      <TableHead className="text-xs font-semibold font-primary text-muted-foreground uppercase tracking-wide">
                        Paciente
                      </TableHead>
                      <TableHead className="text-xs font-semibold font-primary text-muted-foreground uppercase tracking-wide">
                        Profissional
                      </TableHead>
                      <TableHead className="text-xs font-semibold font-primary text-muted-foreground uppercase tracking-wide">
                        Data/Hora
                      </TableHead>
                      <TableHead className="text-xs font-semibold font-primary text-muted-foreground uppercase tracking-wide">
                        Status
                      </TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {proximosAgendamentos?.content.length === 0 ? (
                      <TableRow>
                        <TableCell
                          colSpan={4}
                          className="text-center text-muted-foreground font-secondary py-8"
                        >
                          Nenhum agendamento futuro
                        </TableCell>
                      </TableRow>
                    ) : (
                      proximosAgendamentos?.content.map((ag) => {
                        const cfg = statusConfig[ag.status]
                        return (
                          <TableRow key={ag.id} className="border-border/40 hover:bg-muted/20">
                            <TableCell className="font-semibold font-primary text-sm text-foreground">
                              {ag.pacienteNome}
                            </TableCell>
                            <TableCell className="text-sm font-secondary text-muted-foreground">
                              {ag.profissionalNome}
                            </TableCell>
                            <TableCell className="text-sm font-secondary text-muted-foreground whitespace-nowrap">
                              {formatDateTime(ag.dataHora)}
                            </TableCell>
                            <TableCell>
                              <span
                                className={`inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-semibold font-primary ${cfg.className}`}
                              >
                                {cfg.label}
                              </span>
                            </TableCell>
                          </TableRow>
                        )
                      })
                    )}
                  </TableBody>
                </Table>
              </div>
            )}
          </CardContent>
        </Card>

        {/* Pagamentos Pendentes — oculto para profissionais */}
        {!isProfissional && <Card className="border border-border/60 shadow-soft">
          <CardHeader className="pb-3">
            <CardTitle className="text-base font-semibold font-primary text-foreground flex items-center gap-2">
              <span className="inline-block h-2 w-2 rounded-full bg-accent" />
              Pagamentos Pendentes
            </CardTitle>
          </CardHeader>
          <CardContent>
            {loadingPagLista ? (
              <TableSkeleton cols={4} />
            ) : (
              <div className="overflow-x-auto">
                <Table>
                  <TableHeader>
                    <TableRow className="border-border/50 hover:bg-transparent">
                      <TableHead className="text-xs font-semibold font-primary text-muted-foreground uppercase tracking-wide">
                        Paciente
                      </TableHead>
                      <TableHead className="text-xs font-semibold font-primary text-muted-foreground uppercase tracking-wide">
                        Valor
                      </TableHead>
                      <TableHead className="text-xs font-semibold font-primary text-muted-foreground uppercase tracking-wide">
                        Vencimento
                      </TableHead>
                      <TableHead className="text-xs font-semibold font-primary text-muted-foreground uppercase tracking-wide">
                        Forma
                      </TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {pagamentosPendentesLista?.content.length === 0 ? (
                      <TableRow>
                        <TableCell
                          colSpan={4}
                          className="text-center text-muted-foreground font-secondary py-8"
                        >
                          Nenhum pagamento pendente
                        </TableCell>
                      </TableRow>
                    ) : (
                      pagamentosPendentesLista?.content.map((pag) => (
                        <TableRow key={pag.id} className="border-border/40 hover:bg-muted/20">
                          <TableCell className="font-semibold font-primary text-sm text-foreground">
                            {pag.pacienteNome}
                          </TableCell>
                          <TableCell className="text-sm font-secondary font-semibold text-accent">
                            {formatCurrency(pag.valor)}
                          </TableCell>
                          <TableCell className="text-sm font-secondary text-muted-foreground">
                            {format(new Date(pag.dataVencimento), "dd/MM/yyyy")}
                          </TableCell>
                          <TableCell>
                            <Badge
                              variant="outline"
                              className="text-xs font-primary border-border/60 text-muted-foreground"
                            >
                              {pag.formaPagamento ?? "—"}
                            </Badge>
                          </TableCell>
                        </TableRow>
                      ))
                    )}
                  </TableBody>
                </Table>
              </div>
            )}
          </CardContent>
        </Card>}
      </div>
    </div>
  )
}
