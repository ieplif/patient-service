import { useState } from "react"
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query"
import { UserCog, Plus, Pencil, PowerOff } from "lucide-react"
import { getProfissionais, deleteProfissional } from "@/api/profissionais"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
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
import { ProfissionalFormSheet } from "@/components/shared/ProfissionalFormSheet"
import { useToast } from "@/hooks/use-toast"
import type { Profissional } from "@/types"

const PAGE_SIZE = 15

function formatTelefone(value: string) {
  const digits = value.replace(/\D/g, "").slice(0, 11)
  if (digits.length <= 10) {
    return digits.replace(/(\d{2})(\d{4})(\d{0,4})/, "($1) $2-$3").trim()
  }
  return digits.replace(/(\d{2})(\d{5})(\d{0,4})/, "($1) $2-$3").trim()
}

export function ProfissionaisPage() {
  const [page, setPage] = useState(0)
  const [sheetOpen, setSheetOpen] = useState(false)
  const [selected, setSelected] = useState<Profissional | null>(null)
  const { toast } = useToast()
  const queryClient = useQueryClient()

  const { data, isLoading } = useQuery({
    queryKey: ["profissionais", page],
    queryFn: () => getProfissionais({ page, size: PAGE_SIZE, sort: "nome,asc" }),
  })

  const deactivateMutation = useMutation({
    mutationFn: deleteProfissional,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["profissionais"] })
      toast({ title: "Profissional desativado", description: "O profissional foi desativado com sucesso." })
    },
    onError: () => {
      toast({ title: "Erro", description: "Não foi possível desativar o profissional.", variant: "destructive" })
    },
  })

  function handleNew() {
    setSelected(null)
    setSheetOpen(true)
  }

  function handleEdit(prof: Profissional) {
    setSelected(prof)
    setSheetOpen(true)
  }

  return (
    <div className="space-y-5 animate-fade-in">
      <div className="flex flex-col sm:flex-row items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold font-primary text-foreground tracking-tight flex items-center gap-2">
            <UserCog className="h-6 w-6 text-primary" />
            Profissionais
          </h1>
          <p className="text-sm text-muted-foreground font-secondary mt-0.5">
            {data ? `${data.totalElements} profissionais cadastrados` : "Carregando..."}
          </p>
        </div>
        <Button onClick={handleNew} className="font-primary gap-2">
          <Plus className="h-4 w-4" />
          Novo Profissional
        </Button>
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
              <div className="overflow-x-auto">
                <Table>
                  <TableHeader>
                    <TableRow className="border-border/50 hover:bg-transparent">
                      {["Nome", "Atividades", "Status", ""].map((h, i) => (
                      <TableHead key={i} className="text-xs font-semibold font-primary text-muted-foreground uppercase tracking-wide">
                        {h}
                      </TableHead>
                    ))}
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {data?.content.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={4} className="text-center text-muted-foreground font-secondary py-12">
                        Nenhum profissional encontrado
                      </TableCell>
                    </TableRow>
                  ) : (
                    data?.content.map((prof) => (
                      <TableRow key={prof.id} className="border-border/40 hover:bg-muted/20">
                        <TableCell className="font-semibold font-primary text-sm text-foreground">
                          {prof.nome}
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
                        <TableCell className="text-right">
                          <div className="flex items-center justify-end gap-1">
                            <Button
                              size="icon"
                              variant="ghost"
                              className="h-8 w-8 text-muted-foreground hover:text-foreground"
                              onClick={() => handleEdit(prof)}
                              title="Editar"
                            >
                              <Pencil className="h-4 w-4" />
                            </Button>
                            {prof.ativo && (
                              <Button
                                size="icon"
                                variant="ghost"
                                className="h-8 w-8 text-muted-foreground hover:text-destructive"
                                onClick={() => deactivateMutation.mutate(prof.id)}
                                title="Desativar"
                                disabled={deactivateMutation.isPending}
                              >
                                <PowerOff className="h-4 w-4" />
                              </Button>
                            )}
                          </div>
                        </TableCell>
                      </TableRow>
                    ))
                  )}
                </TableBody>
                </Table>
              </div>
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

      <ProfissionalFormSheet
        open={sheetOpen}
        onOpenChange={setSheetOpen}
        profissional={selected}
      />
    </div>
  )
}
