import { useState } from "react"
import { useQuery } from "@tanstack/react-query"
import { CreditCard } from "lucide-react"
import { format } from "date-fns"
import { getPagamentos } from "@/api/pagamentos"
import type { StatusPagamento, FormaPagamento } from "@/types"
import { Skeleton } from "@/components/ui/skeleton"
import { Card, CardContent, CardHeader } from "@/components/ui/card"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { Badge } from "@/components/ui/badge"
import { Pagination } from "@/components/shared/Pagination"

const PAGE_SIZE = 15

const statusConfig: Record<StatusPagamento, { label: string; className: string }> = {
  PENDENTE: { label: "Pendente", className: "bg-accent/15 text-accent border-accent/30" },
  PARCIALMENTE_PAGO: { label: "Parcial", className: "bg-secondary/40 text-[hsl(202,40%,40%)] border-secondary/50" },
  PAGO: { label: "Pago", className: "bg-primary/15 text-primary border-primary/30" },
  CANCELADO: { label: "Cancelado", className: "bg-destructive/10 text-destructive border-destructive/20" },
  REEMBOLSADO: { label: "Reembolsado", className: "bg-muted text-muted-foreground border-border" },
}

const formaPagamentoLabel: Record<FormaPagamento, string> = {
  PIX: "Pix",
  CARTAO_CREDITO: "Cartão Crédito",
  CARTAO_DEBITO: "Cartão Débito",
  DINHEIRO: "Dinheiro",
}

function formatCurrency(v: number) {
  return new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }).format(v)
}

const statusOptions: { value: StatusPagamento | "TODOS"; label: string }[] = [
  { value: "TODOS", label: "Todos os status" },
  { value: "PENDENTE", label: "Pendente" },
  { value: "PARCIALMENTE_PAGO", label: "Parcialmente pago" },
  { value: "PAGO", label: "Pago" },
  { value: "CANCELADO", label: "Cancelado" },
  { value: "REEMBOLSADO", label: "Reembolsado" },
]

export function PagamentosPage() {
  const [page, setPage] = useState(0)
  const [statusFilter, setStatusFilter] = useState<StatusPagamento | "TODOS">("TODOS")

  const { data, isLoading } = useQuery({
    queryKey: ["pagamentos", page, statusFilter],
    queryFn: () =>
      getPagamentos({
        page,
        size: PAGE_SIZE,
        sort: "createdAt,desc",
        status: statusFilter !== "TODOS" ? statusFilter : undefined,
      }),
  })

  return (
    <div className="space-y-5 animate-fade-in">
      <div>
        <h1 className="text-2xl font-bold font-primary text-foreground tracking-tight flex items-center gap-2">
          <CreditCard className="h-6 w-6 text-primary" />
          Pagamentos
        </h1>
        <p className="text-sm text-muted-foreground font-secondary mt-0.5">
          {data ? `${data.totalElements} pagamentos` : "Carregando..."}
        </p>
      </div>

      <Card className="border border-border/60 shadow-soft">
        <CardHeader className="pb-3">
          <Select
            value={statusFilter}
            onValueChange={(v) => { setStatusFilter(v as StatusPagamento | "TODOS"); setPage(0) }}
          >
            <SelectTrigger className="w-52 bg-background border-border/70 font-secondary text-sm">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {statusOptions.map((o) => (
                <SelectItem key={o.value} value={o.value} className="font-secondary text-sm">
                  {o.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </CardHeader>

        <CardContent className="p-0">
          {isLoading ? (
            <div className="space-y-2 p-6">
              {Array.from({ length: 8 }).map((_, i) => (
                <Skeleton key={i} className="h-11 w-full" />
              ))}
            </div>
          ) : (
            <>
              <Table>
                <TableHeader>
                  <TableRow className="border-border/50 hover:bg-transparent">
                    {["Paciente", "Valor", "Forma", "Vencimento", "Parcelas", "Status"].map((h) => (
                      <TableHead key={h} className="text-xs font-semibold font-primary text-muted-foreground uppercase tracking-wide">
                        {h}
                      </TableHead>
                    ))}
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {data?.content.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={6} className="text-center text-muted-foreground font-secondary py-12">
                        Nenhum pagamento encontrado
                      </TableCell>
                    </TableRow>
                  ) : (
                    data?.content.map((pag) => {
                      const cfg = statusConfig[pag.status]
                      return (
                        <TableRow key={pag.id} className="border-border/40 hover:bg-muted/20">
                          <TableCell className="font-semibold font-primary text-sm text-foreground">
                            {pag.pacienteNome}
                          </TableCell>
                          <TableCell className="text-sm font-secondary font-semibold text-accent">
                            {formatCurrency(pag.valor)}
                          </TableCell>
                          <TableCell>
                            <Badge variant="outline" className="text-xs font-primary border-border/60 text-muted-foreground">
                              {formaPagamentoLabel[pag.formaPagamento] ?? pag.formaPagamento}
                            </Badge>
                          </TableCell>
                          <TableCell className="text-sm font-secondary text-muted-foreground">
                            {format(new Date(pag.dataVencimento), "dd/MM/yyyy")}
                          </TableCell>
                          <TableCell className="text-sm font-secondary text-muted-foreground text-center">
                            {pag.numeroParcelas}x
                          </TableCell>
                          <TableCell>
                            <span className={`inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-semibold font-primary ${cfg.className}`}>
                              {cfg.label}
                            </span>
                          </TableCell>
                        </TableRow>
                      )
                    })
                  )}
                </TableBody>
              </Table>
              <Pagination
                page={page}
                totalPages={data?.totalPages ?? 0}
                totalElements={data?.totalElements ?? 0}
                size={PAGE_SIZE}
                onPageChange={setPage}
              />
            </>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
