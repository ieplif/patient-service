import { useEffect, useState } from "react"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { UserCog, Pencil } from "lucide-react"
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
import { useToast } from "@/hooks/use-toast"
import { createProfissional, updateProfissional } from "@/api/profissionais"
import { getAtividades } from "@/api/atividades"
import type { Profissional } from "@/types"

interface ProfissionalFormSheetProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  profissional?: Profissional | null
}

interface FormState {
  nome: string
  email: string
  telefone: string
  senha: string
  atividadeIds: string[]
  googleCalendarId: string
}

const emptyForm: FormState = {
  nome: "",
  email: "",
  telefone: "",
  senha: "",
  atividadeIds: [],
  googleCalendarId: "",
}

function formatTelefone(value: string) {
  const digits = value.replace(/\D/g, "").slice(0, 11)
  if (digits.length <= 10) {
    return digits.replace(/(\d{2})(\d{4})(\d{0,4})/, "($1) $2-$3").trim()
  }
  return digits.replace(/(\d{2})(\d{5})(\d{0,4})/, "($1) $2-$3").trim()
}

export function ProfissionalFormSheet({ open, onOpenChange, profissional }: ProfissionalFormSheetProps) {
  const { toast } = useToast()
  const queryClient = useQueryClient()
  const isEdit = !!profissional

  const [form, setForm] = useState<FormState>(emptyForm)

  const { data: atividades = [] } = useQuery({
    queryKey: ["atividades"],
    queryFn: getAtividades,
  })

  useEffect(() => {
    if (profissional) {
      setForm({
        nome: profissional.nome,
        email: profissional.email,
        telefone: formatTelefone(profissional.telefone),
        senha: "",
        atividadeIds: profissional.atividades.map((a) => a.id),
        googleCalendarId: profissional.googleCalendarId ?? "",
      })
    } else {
      setForm(emptyForm)
    }
  }, [profissional, open])

  function set(field: keyof FormState, value: string | string[]) {
    setForm((prev) => ({ ...prev, [field]: value }))
  }

  function toggleAtividade(id: string) {
    setForm((prev) => ({
      ...prev,
      atividadeIds: prev.atividadeIds.includes(id)
        ? prev.atividadeIds.filter((a) => a !== id)
        : [...prev.atividadeIds, id],
    }))
  }

  const mutation = useMutation({
    mutationFn: () => {
      if (isEdit) {
        return updateProfissional(profissional!.id, {
          nome: form.nome,
          telefone: form.telefone.replace(/\D/g, ""),
          atividadeIds: form.atividadeIds,
          googleCalendarId: form.googleCalendarId || undefined,
        })
      }
      return createProfissional({
        nome: form.nome,
        email: form.email,
        telefone: form.telefone.replace(/\D/g, ""),
        senha: form.senha,
        atividadeIds: form.atividadeIds,
        googleCalendarId: form.googleCalendarId || undefined,
      })
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["profissionais"] })
      toast({
        title: isEdit ? "Profissional atualizado" : "Profissional cadastrado",
        description: isEdit
          ? "Os dados foram atualizados com sucesso."
          : "Novo profissional cadastrado com sucesso.",
      })
      onOpenChange(false)
    },
    onError: (err: unknown) => {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        "Verifique os dados e tente novamente."
      toast({ title: "Erro", description: msg, variant: "destructive" })
    },
  })

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (form.atividadeIds.length === 0) {
      toast({ title: "Atenção", description: "Selecione ao menos uma atividade.", variant: "destructive" })
      return
    }
    mutation.mutate()
  }

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent className="w-full sm:max-w-lg overflow-y-auto">
        <SheetHeader className="mb-6">
          <SheetTitle className="flex items-center gap-2 font-primary">
            {isEdit ? <Pencil className="h-5 w-5 text-primary" /> : <UserCog className="h-5 w-5 text-primary" />}
            {isEdit ? "Editar Profissional" : "Novo Profissional"}
          </SheetTitle>
          <SheetDescription className="font-secondary">
            {isEdit ? "Atualize os dados do profissional." : "Preencha os dados para cadastrar um novo profissional."}
          </SheetDescription>
        </SheetHeader>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-1.5">
            <Label htmlFor="nome" className="font-primary text-sm">Nome completo *</Label>
            <Input
              id="nome"
              value={form.nome}
              onChange={(e) => set("nome", e.target.value)}
              placeholder="Nome do profissional"
              required
              minLength={3}
              className="font-secondary"
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-1.5">
              <Label htmlFor="prof-email" className="font-primary text-sm">E-mail *</Label>
              <Input
                id="prof-email"
                type="email"
                value={form.email}
                onChange={(e) => set("email", e.target.value)}
                placeholder="email@humaniza.com"
                required={!isEdit}
                disabled={isEdit}
                className="font-secondary"
              />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="prof-telefone" className="font-primary text-sm">Telefone *</Label>
              <Input
                id="prof-telefone"
                value={form.telefone}
                onChange={(e) => set("telefone", formatTelefone(e.target.value))}
                placeholder="(21) 99999-9999"
                required
                className="font-secondary"
              />
            </div>
          </div>

          {!isEdit && (
            <div className="space-y-1.5">
              <Label htmlFor="prof-senha" className="font-primary text-sm">Senha *</Label>
              <Input
                id="prof-senha"
                type="password"
                value={form.senha}
                onChange={(e) => set("senha", e.target.value)}
                placeholder="Mínimo 6 caracteres"
                required
                minLength={6}
                className="font-secondary"
              />
            </div>
          )}

          <div className="space-y-2">
            <Label className="font-primary text-sm">Atividades *</Label>
            <div className="grid grid-cols-2 gap-2 rounded-md border border-border p-3">
              {atividades.length === 0 ? (
                <p className="col-span-2 text-sm text-muted-foreground font-secondary">Carregando atividades...</p>
              ) : (
                atividades.map((at) => (
                  <label key={at.id} className="flex items-center gap-2 cursor-pointer">
                    <input
                      type="checkbox"
                      checked={form.atividadeIds.includes(at.id)}
                      onChange={() => toggleAtividade(at.id)}
                      className="h-4 w-4 rounded border-border accent-primary"
                    />
                    <span className="text-sm font-secondary">{at.nome}</span>
                  </label>
                ))
              )}
            </div>
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="googleCalendarId" className="font-primary text-sm">Google Calendar ID</Label>
            <Input
              id="googleCalendarId"
              value={form.googleCalendarId}
              onChange={(e) => set("googleCalendarId", e.target.value)}
              placeholder="ID da agenda do Google Calendar"
              className="font-secondary"
            />
          </div>

          <div className="flex gap-2 pt-4">
            <Button
              type="button"
              variant="outline"
              className="flex-1 font-primary"
              onClick={() => onOpenChange(false)}
            >
              Cancelar
            </Button>
            <Button
              type="submit"
              className="flex-1 font-primary"
              disabled={mutation.isPending}
            >
              {mutation.isPending ? "Salvando..." : isEdit ? "Salvar alterações" : "Cadastrar"}
            </Button>
          </div>
        </form>
      </SheetContent>
    </Sheet>
  )
}
