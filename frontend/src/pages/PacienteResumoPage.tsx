import { useState, useRef } from "react"
import { useParams, Link } from "react-router-dom"
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query"
import { format } from "date-fns"
import { ptBR } from "date-fns/locale"
import {
  User, Mail, Phone, MapPin, Briefcase, Heart, Shield, Calendar,
  Star, FileText, Upload, Trash2, ExternalLink, ArrowLeft, Clock
} from "lucide-react"
import { getPatient } from "@/api/patients"
import { getAgendamentos } from "@/api/agendamentos"
import { getAssinaturas } from "@/api/assinaturas"
import { getProntuarios, uploadProntuario, deleteProntuario } from "@/api/prontuarios"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Badge } from "@/components/ui/badge"
import { Skeleton } from "@/components/ui/skeleton"
import { useToast } from "@/hooks/use-toast"
import {
  Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter
} from "@/components/ui/dialog"
import {
  Table, TableBody, TableCell, TableHead, TableHeader, TableRow
} from "@/components/ui/table"

function formatPhone(v: string) {
  const d = v.replace(/\D/g, "")
  if (d.length === 11) return `(${d.slice(0, 2)}) ${d.slice(2, 7)}-${d.slice(7)}`
  if (d.length === 10) return `(${d.slice(0, 2)}) ${d.slice(2, 6)}-${d.slice(6)}`
  return v
}

function formatCpf(v?: string) {
  if (!v) return "—"
  const d = v.replace(/\D/g, "")
  if (d.length === 11) return `${d.slice(0, 3)}.${d.slice(3, 6)}.${d.slice(6, 9)}-${d.slice(9)}`
  return v
}

function formatCurrency(v: number) {
  return new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }).format(v)
}

function formatBytes(bytes: number) {
  if (bytes < 1024) return bytes + " B"
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(0) + " KB"
  return (bytes / (1024 * 1024)).toFixed(1) + " MB"
}

const statusAgConfig: Record<string, { label: string; className: string }> = {
  AGENDADO: { label: "Agendado", className: "bg-secondary/40 text-[hsl(202,40%,40%)] border-secondary/50" },
  CONFIRMADO: { label: "Confirmado", className: "bg-primary/15 text-primary border-primary/30" },
  CANCELADO: { label: "Cancelado", className: "bg-destructive/10 text-destructive border-destructive/20" },
  REALIZADO: { label: "Realizado", className: "bg-muted text-muted-foreground border-border" },
  NAO_COMPARECEU: { label: "Faltou", className: "bg-accent/15 text-accent border-accent/30" },
}

const statusAssConfig: Record<string, { label: string; className: string }> = {
  ATIVO: { label: "Ativo", className: "bg-primary/15 text-primary border-primary/30" },
  CANCELADO: { label: "Cancelado", className: "bg-destructive/10 text-destructive border-destructive/20" },
  VENCIDO: { label: "Vencido", className: "bg-accent/15 text-accent border-accent/30" },
  FINALIZADO: { label: "Finalizado", className: "bg-muted text-muted-foreground border-border" },
}

export function PacienteResumoPage() {
  const { id } = useParams<{ id: string }>()
  const queryClient = useQueryClient()
  const { toast } = useToast()
  const fileInputRef = useRef<HTMLInputElement>(null)
  const [uploadOpen, setUploadOpen] = useState(false)
  const [uploadTitulo, setUploadTitulo] = useState("")
  const [uploadDescricao, setUploadDescricao] = useState("")
  const [uploadFile, setUploadFile] = useState<File | null>(null)

  const { data: paciente, isLoading: loadingPaciente } = useQuery({
    queryKey: ["patient", id],
    queryFn: () => getPatient(id!),
    enabled: !!id,
  })

  const { data: agendamentos, isLoading: loadingAg } = useQuery({
    queryKey: ["agendamentos-paciente", id],
    queryFn: () => getAgendamentos({ pacienteId: id, size: 10, sort: "dataHora,desc" }),
    enabled: !!id,
  })

  const { data: assinaturas, isLoading: loadingAss } = useQuery({
    queryKey: ["assinaturas-paciente", id],
    queryFn: () => getAssinaturas({ pacienteId: id, size: 10 }),
    enabled: !!id,
  })

  const { data: prontuarios, isLoading: loadingPront } = useQuery({
    queryKey: ["prontuarios", id],
    queryFn: () => getProntuarios(id!, { size: 50 }),
    enabled: !!id,
  })

  const uploadMutation = useMutation({
    mutationFn: () => uploadProntuario(id!, uploadTitulo, uploadFile!, uploadDescricao || undefined),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["prontuarios", id] })
      toast({ title: "Prontuario anexado", description: "O arquivo foi enviado com sucesso." })
      setUploadOpen(false)
      setUploadTitulo("")
      setUploadDescricao("")
      setUploadFile(null)
    },
    onError: () => {
      toast({ title: "Erro", description: "Falha ao enviar o arquivo. Verifique a configuracao do Supabase Storage.", variant: "destructive" })
    },
  })

  const deleteMutation = useMutation({
    mutationFn: deleteProntuario,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["prontuarios", id] })
      toast({ title: "Prontuario removido" })
    },
  })

  if (loadingPaciente) {
    return (
      <div className="space-y-6 animate-fade-in">
        <Skeleton className="h-8 w-64" />
        <div className="grid gap-6 lg:grid-cols-3">
          <Skeleton className="h-64" />
          <Skeleton className="h-64 lg:col-span-2" />
        </div>
      </div>
    )
  }

  if (!paciente) {
    return <div className="text-center py-20 text-muted-foreground">Paciente nao encontrado.</div>
  }

  const idade = paciente.dataNascimento
    ? Math.floor((Date.now() - new Date(paciente.dataNascimento).getTime()) / (365.25 * 24 * 60 * 60 * 1000))
    : null

  return (
    <div className="space-y-6 animate-fade-in">
      {/* Header */}
      <div className="flex items-center gap-4">
        <Link to="/pacientes">
          <Button variant="ghost" size="icon">
            <ArrowLeft className="h-5 w-5" />
          </Button>
        </Link>
        <div>
          <h1 className="text-2xl font-bold font-primary text-foreground tracking-tight">
            {paciente.nomeCompleto}
          </h1>
          <p className="text-sm text-muted-foreground font-secondary">
            Paciente desde {format(new Date(paciente.createdAt), "MMMM 'de' yyyy", { locale: ptBR })}
          </p>
        </div>
        <div className="ml-auto">
          <Badge variant={paciente.statusAtivo ? "default" : "destructive"} className="font-primary">
            {paciente.statusAtivo ? "Ativo" : "Inativo"}
          </Badge>
        </div>
      </div>

      {/* Dados Pessoais + Info Rápida */}
      <div className="grid gap-6 lg:grid-cols-3">
        {/* Dados Pessoais */}
        <Card className="border border-border/60 shadow-soft">
          <CardHeader className="pb-3">
            <CardTitle className="text-base font-semibold font-primary flex items-center gap-2">
              <User className="h-4 w-4 text-primary" /> Dados Pessoais
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            <div className="flex items-center gap-2 text-sm">
              <Calendar className="h-4 w-4 text-muted-foreground shrink-0" />
              <span className="font-secondary">
                {format(new Date(paciente.dataNascimento), "dd/MM/yyyy")}
                {idade !== null && <span className="text-muted-foreground"> ({idade} anos)</span>}
              </span>
            </div>
            <div className="flex items-center gap-2 text-sm">
              <Shield className="h-4 w-4 text-muted-foreground shrink-0" />
              <span className="font-secondary font-mono text-xs">{formatCpf(paciente.cpf)}</span>
            </div>
            <div className="flex items-center gap-2 text-sm">
              <Mail className="h-4 w-4 text-muted-foreground shrink-0" />
              <span className="font-secondary">{paciente.email}</span>
            </div>
            <div className="flex items-center gap-2 text-sm">
              <Phone className="h-4 w-4 text-muted-foreground shrink-0" />
              <span className="font-secondary">{formatPhone(paciente.telefone)}</span>
            </div>
            {paciente.endereco && (
              <div className="flex items-center gap-2 text-sm">
                <MapPin className="h-4 w-4 text-muted-foreground shrink-0" />
                <span className="font-secondary">{paciente.endereco}</span>
              </div>
            )}
            {paciente.profissao && (
              <div className="flex items-center gap-2 text-sm">
                <Briefcase className="h-4 w-4 text-muted-foreground shrink-0" />
                <span className="font-secondary">{paciente.profissao}</span>
              </div>
            )}
            {paciente.estadoCivil && (
              <div className="flex items-center gap-2 text-sm">
                <Heart className="h-4 w-4 text-muted-foreground shrink-0" />
                <span className="font-secondary">{paciente.estadoCivil}</span>
              </div>
            )}
            {paciente.medicoResponsavel && (
              <div className="flex items-center gap-2 text-sm">
                <User className="h-4 w-4 text-muted-foreground shrink-0" />
                <span className="font-secondary">Dr(a). {paciente.medicoResponsavel}</span>
              </div>
            )}
          </CardContent>
        </Card>

        {/* Assinaturas */}
        <Card className="border border-border/60 shadow-soft lg:col-span-2">
          <CardHeader className="pb-3">
            <CardTitle className="text-base font-semibold font-primary flex items-center gap-2">
              <Star className="h-4 w-4 text-primary" /> Assinaturas
            </CardTitle>
          </CardHeader>
          <CardContent>
            {loadingAss ? (
              <Skeleton className="h-20 w-full" />
            ) : !assinaturas?.content.length ? (
              <p className="text-sm text-muted-foreground font-secondary py-4 text-center">Nenhuma assinatura</p>
            ) : (
              <Table>
                <TableHeader>
                  <TableRow className="border-border/50 hover:bg-transparent">
                    <TableHead className="text-xs font-semibold font-primary text-muted-foreground uppercase">Servico</TableHead>
                    <TableHead className="text-xs font-semibold font-primary text-muted-foreground uppercase">Valor</TableHead>
                    <TableHead className="text-xs font-semibold font-primary text-muted-foreground uppercase">Progresso</TableHead>
                    <TableHead className="text-xs font-semibold font-primary text-muted-foreground uppercase">Vencimento</TableHead>
                    <TableHead className="text-xs font-semibold font-primary text-muted-foreground uppercase">Status</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {assinaturas.content.map((a) => {
                    const cfg = statusAssConfig[a.status] || statusAssConfig.ATIVO
                    const prog = a.sessoesContratadas > 0 ? Math.round((a.sessoesRealizadas / a.sessoesContratadas) * 100) : 0
                    return (
                      <TableRow key={a.id} className="border-border/40">
                        <TableCell className="text-sm font-secondary">{a.servicoDescricao}</TableCell>
                        <TableCell className="text-sm font-secondary font-semibold text-accent">{formatCurrency(a.valor)}</TableCell>
                        <TableCell>
                          <div className="flex items-center gap-2">
                            <div className="flex-1 h-1.5 rounded-full bg-muted overflow-hidden w-16">
                              <div className="h-full bg-primary rounded-full" style={{ width: `${Math.min(prog, 100)}%` }} />
                            </div>
                            <span className="text-xs text-muted-foreground">{a.sessoesRealizadas}/{a.sessoesContratadas}</span>
                          </div>
                        </TableCell>
                        <TableCell className="text-sm font-secondary text-muted-foreground">
                          {a.dataVencimento ? format(new Date(a.dataVencimento), "dd/MM/yyyy") : "—"}
                        </TableCell>
                        <TableCell>
                          <span className={`inline-flex items-center rounded-full border px-2 py-0.5 text-xs font-semibold font-primary ${cfg.className}`}>
                            {cfg.label}
                          </span>
                        </TableCell>
                      </TableRow>
                    )
                  })}
                </TableBody>
              </Table>
            )}
          </CardContent>
        </Card>
      </div>

      {/* Agendamentos */}
      <Card className="border border-border/60 shadow-soft">
        <CardHeader className="pb-3">
          <CardTitle className="text-base font-semibold font-primary flex items-center gap-2">
            <Clock className="h-4 w-4 text-primary" /> Agendamentos Recentes
          </CardTitle>
        </CardHeader>
        <CardContent>
          {loadingAg ? (
            <Skeleton className="h-32 w-full" />
          ) : !agendamentos?.content.length ? (
            <p className="text-sm text-muted-foreground font-secondary py-4 text-center">Nenhum agendamento</p>
          ) : (
            <Table>
              <TableHeader>
                <TableRow className="border-border/50 hover:bg-transparent">
                  <TableHead className="text-xs font-semibold font-primary text-muted-foreground uppercase">Servico</TableHead>
                  <TableHead className="text-xs font-semibold font-primary text-muted-foreground uppercase">Profissional</TableHead>
                  <TableHead className="text-xs font-semibold font-primary text-muted-foreground uppercase">Data/Hora</TableHead>
                  <TableHead className="text-xs font-semibold font-primary text-muted-foreground uppercase">Duracao</TableHead>
                  <TableHead className="text-xs font-semibold font-primary text-muted-foreground uppercase">Status</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {agendamentos.content.map((ag) => {
                  const cfg = statusAgConfig[ag.status] || statusAgConfig.AGENDADO
                  return (
                    <TableRow key={ag.id} className="border-border/40">
                      <TableCell className="text-sm font-secondary">{ag.servicoDescricao}</TableCell>
                      <TableCell className="text-sm font-secondary text-muted-foreground">{ag.profissionalNome}</TableCell>
                      <TableCell className="text-sm font-secondary text-muted-foreground whitespace-nowrap">
                        {format(new Date(ag.dataHora), "dd/MM/yyyy 'as' HH:mm")}
                      </TableCell>
                      <TableCell className="text-sm font-secondary text-muted-foreground">{ag.duracaoMinutos} min</TableCell>
                      <TableCell>
                        <span className={`inline-flex items-center rounded-full border px-2 py-0.5 text-xs font-semibold font-primary ${cfg.className}`}>
                          {cfg.label}
                        </span>
                      </TableCell>
                    </TableRow>
                  )
                })}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>

      {/* Prontuários */}
      <Card className="border border-border/60 shadow-soft">
        <CardHeader className="pb-3 flex flex-row items-center justify-between">
          <CardTitle className="text-base font-semibold font-primary flex items-center gap-2">
            <FileText className="h-4 w-4 text-primary" /> Prontuarios
          </CardTitle>
          <Button size="sm" className="bg-primary text-primary-foreground font-primary" onClick={() => setUploadOpen(true)}>
            <Upload className="h-4 w-4 mr-2" /> Anexar Arquivo
          </Button>
        </CardHeader>
        <CardContent>
          {loadingPront ? (
            <Skeleton className="h-20 w-full" />
          ) : !prontuarios?.content.length ? (
            <p className="text-sm text-muted-foreground font-secondary py-4 text-center">Nenhum prontuario anexado</p>
          ) : (
            <div className="space-y-2">
              {prontuarios.content.map((p) => (
                <div key={p.id} className="flex items-center gap-3 p-3 rounded-lg border border-border/40 hover:bg-muted/20">
                  <FileText className="h-8 w-8 text-primary/60 shrink-0" />
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-semibold font-primary truncate">{p.titulo}</p>
                    <p className="text-xs text-muted-foreground font-secondary">
                      {p.nomeArquivo} — {formatBytes(p.tamanhoBytes)} — {format(new Date(p.createdAt), "dd/MM/yyyy HH:mm")}
                    </p>
                    {p.descricao && (
                      <p className="text-xs text-muted-foreground font-secondary mt-0.5">{p.descricao}</p>
                    )}
                  </div>
                  <a href={p.storageUrl} target="_blank" rel="noopener noreferrer">
                    <Button variant="ghost" size="icon" className="h-8 w-8">
                      <ExternalLink className="h-4 w-4" />
                    </Button>
                  </a>
                  <Button
                    variant="ghost"
                    size="icon"
                    className="h-8 w-8 text-destructive"
                    onClick={() => deleteMutation.mutate(p.id)}
                  >
                    <Trash2 className="h-4 w-4" />
                  </Button>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      {/* Upload Dialog */}
      <Dialog open={uploadOpen} onOpenChange={setUploadOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle className="font-primary">Anexar Prontuario</DialogTitle>
          </DialogHeader>
          <div className="space-y-4">
            <div className="space-y-2">
              <Label className="font-primary">Titulo *</Label>
              <Input
                value={uploadTitulo}
                onChange={(e) => setUploadTitulo(e.target.value)}
                placeholder="Ex: Avaliacao inicial, Laudo medico..."
                className="font-secondary"
              />
            </div>
            <div className="space-y-2">
              <Label className="font-primary">Descricao</Label>
              <textarea
                value={uploadDescricao}
                onChange={(e) => setUploadDescricao(e.target.value)}
                placeholder="Descricao opcional..."
                className="flex min-h-[60px] w-full rounded-md border border-input bg-background px-3 py-2 text-sm font-secondary"
              />
            </div>
            <div className="space-y-2">
              <Label className="font-primary">Arquivo *</Label>
              <Input
                ref={fileInputRef}
                type="file"
                accept=".pdf,.jpg,.jpeg,.png,.doc,.docx"
                onChange={(e) => setUploadFile(e.target.files?.[0] || null)}
                className="font-secondary"
              />
              <p className="text-xs text-muted-foreground">PDF, imagem ou documento. Maximo 10MB.</p>
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" className="font-primary" onClick={() => setUploadOpen(false)}>
              Cancelar
            </Button>
            <Button
              className="bg-primary text-primary-foreground font-primary"
              disabled={!uploadTitulo || !uploadFile || uploadMutation.isPending}
              onClick={() => uploadMutation.mutate()}
            >
              {uploadMutation.isPending ? "Enviando..." : "Enviar"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
