import { useState } from "react"
import { useQuery } from "@tanstack/react-query"
import { Search, Users } from "lucide-react"
import { format } from "date-fns"
import { ptBR } from "date-fns/locale"
import { getPatients } from "@/api/patients"
import { Input } from "@/components/ui/input"
import { Badge } from "@/components/ui/badge"
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
import { Pagination } from "@/components/shared/Pagination"

const PAGE_SIZE = 15

export function PacientesPage() {
  const [page, setPage] = useState(0)
  const [search, setSearch] = useState("")
  const [debouncedSearch, setDebouncedSearch] = useState("")

  function handleSearch(value: string) {
    setSearch(value)
    clearTimeout((window as unknown as { _st?: ReturnType<typeof setTimeout> })._st)
    ;(window as unknown as { _st?: ReturnType<typeof setTimeout> })._st = setTimeout(() => {
      setDebouncedSearch(value)
      setPage(0)
    }, 400)
  }

  const { data, isLoading } = useQuery({
    queryKey: ["patients", page, debouncedSearch],
    queryFn: () =>
      getPatients({
        page,
        size: PAGE_SIZE,
        sort: "nomeCompleto,asc",
        nome: debouncedSearch || undefined,
      }),
  })

  return (
    <div className="space-y-5 animate-fade-in">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold font-primary text-foreground tracking-tight flex items-center gap-2">
            <Users className="h-6 w-6 text-primary" />
            Pacientes
          </h1>
          <p className="text-sm text-muted-foreground font-secondary mt-0.5">
            {data ? `${data.totalElements} pacientes cadastrados` : "Carregando..."}
          </p>
        </div>
      </div>

      <Card className="border border-border/60 shadow-soft">
        <CardHeader className="pb-3">
          <div className="relative max-w-sm">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
            <Input
              placeholder="Buscar por nome..."
              value={search}
              onChange={(e) => handleSearch(e.target.value)}
              className="pl-9 bg-background border-border/70 font-secondary"
            />
          </div>
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
                    {["Nome", "E-mail", "Telefone", "Nascimento", "Status"].map((h) => (
                      <TableHead
                        key={h}
                        className="text-xs font-semibold font-primary text-muted-foreground uppercase tracking-wide"
                      >
                        {h}
                      </TableHead>
                    ))}
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {data?.content.length === 0 ? (
                    <TableRow>
                      <TableCell
                        colSpan={5}
                        className="text-center text-muted-foreground font-secondary py-12"
                      >
                        Nenhum paciente encontrado
                      </TableCell>
                    </TableRow>
                  ) : (
                    data?.content.map((p) => (
                      <TableRow key={p.id} className="border-border/40 hover:bg-muted/20">
                        <TableCell className="font-semibold font-primary text-sm text-foreground">
                          {p.nomeCompleto}
                        </TableCell>
                        <TableCell className="text-sm font-secondary text-muted-foreground">
                          {p.email}
                        </TableCell>
                        <TableCell className="text-sm font-secondary text-muted-foreground">
                          {p.telefone}
                        </TableCell>
                        <TableCell className="text-sm font-secondary text-muted-foreground">
                          {format(new Date(p.dataNascimento), "dd/MM/yyyy", { locale: ptBR })}
                        </TableCell>
                        <TableCell>
                          <Badge
                            className={
                              p.statusAtivo
                                ? "bg-primary/15 text-primary border-primary/30 text-xs font-primary"
                                : "bg-muted text-muted-foreground border-border text-xs font-primary"
                            }
                            variant="outline"
                          >
                            {p.statusAtivo ? "Ativo" : "Inativo"}
                          </Badge>
                        </TableCell>
                      </TableRow>
                    ))
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
