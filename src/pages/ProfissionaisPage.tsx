import { useState } from "react"
import { useQuery } from "@tanstack/react-query"
import { UserCog } from "lucide-react"
import { getProfissionais } from "@/api/profissionais"
import { Badge } from "@/components/ui/badge"
import { Skeleton } from "@/components/ui/skeleton"
import { Card, CardContent } from "@/components/ui/card"
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

export function ProfissionaisPage() {
  const [page, setPage] = useState(0)

  const { data, isLoading } = useQuery({
    queryKey: ["profissionais", page],
    queryFn: () =>
      getProfissionais({ page, size: PAGE_SIZE, sort: "nome,asc" }),
  })

  return (
    <div className="space-y-5 animate-fade-in">
      <div>
        <h1 className="text-2xl font-bold font-primary text-foreground tracking-tight flex items-center gap-2">
          <UserCog className="h-6 w-6 text-primary" />
          Profissionais
        </h1>
        <p className="text-sm text-muted-foreground font-secondary mt-0.5">
          {data ? `${data.totalElements} profissionais cadastrados` : "Carregando..."}
        </p>
      </div>

      <Card className="border border-border/60 shadow-soft">
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
                    {["Nome", "E-mail", "Telefone", "Atividades", "Status"].map((h) => (
                      <TableHead key={h} className="text-xs font-semibold font-primary text-muted-foreground uppercase tracking-wide">
                        {h}
                      </TableHead>
                    ))}
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {data?.content.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={5} className="text-center text-muted-foreground font-secondary py-12">
                        Nenhum profissional encontrado
                      </TableCell>
                    </TableRow>
                  ) : (
                    data?.content.map((prof) => (
                      <TableRow key={prof.id} className="border-border/40 hover:bg-muted/20">
                        <TableCell className="font-semibold font-primary text-sm text-foreground">
                          {prof.nome}
                        </TableCell>
                        <TableCell className="text-sm font-secondary text-muted-foreground">
                          {prof.email}
                        </TableCell>
                        <TableCell className="text-sm font-secondary text-muted-foreground">
                          {prof.telefone}
                        </TableCell>
                        <TableCell>
                          <div className="flex flex-wrap gap-1">
                            {prof.atividades.map((at) => (
                              <Badge
                                key={at.id}
                                variant="outline"
                                className="text-xs font-primary border-primary/30 text-primary bg-primary/8"
                              >
                                {at.nome}
                              </Badge>
                            ))}
                          </div>
                        </TableCell>
                        <TableCell>
                          <Badge
                            variant="outline"
                            className={
                              prof.ativo
                                ? "bg-primary/15 text-primary border-primary/30 text-xs font-primary"
                                : "bg-muted text-muted-foreground border-border text-xs font-primary"
                            }
                          >
                            {prof.ativo ? "Ativo" : "Inativo"}
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
