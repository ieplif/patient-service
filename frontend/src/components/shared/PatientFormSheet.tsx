import { useEffect, useState } from "react"
import { useMutation, useQueryClient } from "@tanstack/react-query"
import { UserPlus, Pencil } from "lucide-react"
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
import { createPatient, updatePatient } from "@/api/patients"
import type { Patient } from "@/types"

interface PatientFormSheetProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  patient?: Patient | null
}

interface FormState {
  nomeCompleto: string
  email: string
  cpf: string
  dataNascimento: string
  telefone: string
  endereco: string
  profissao: string
  estadoCivil: string
  consentimentoLgpd: boolean
}

const emptyForm: FormState = {
  nomeCompleto: "",
  email: "",
  cpf: "",
  dataNascimento: "",
  telefone: "",
  endereco: "",
  profissao: "",
  estadoCivil: "",
  consentimentoLgpd: false,
}

function formatCpf(value: string) {
  return value
    .replace(/\D/g, "")
    .slice(0, 11)
    .replace(/(\d{3})(\d)/, "$1.$2")
    .replace(/(\d{3})(\d)/, "$1.$2")
    .replace(/(\d{3})(\d{1,2})$/, "$1-$2")
}

function formatTelefone(value: string) {
  const digits = value.replace(/\D/g, "").slice(0, 11)
  if (digits.length <= 10) {
    return digits.replace(/(\d{2})(\d{4})(\d{0,4})/, "($1) $2-$3").trim()
  }
  return digits.replace(/(\d{2})(\d{5})(\d{0,4})/, "($1) $2-$3").trim()
}

export function PatientFormSheet({ open, onOpenChange, patient }: PatientFormSheetProps) {
  const { toast } = useToast()
  const queryClient = useQueryClient()
  const isEdit = !!patient

  const [form, setForm] = useState<FormState>(emptyForm)

  useEffect(() => {
    if (patient) {
      setForm({
        nomeCompleto: patient.nomeCompleto,
        email: patient.email,
        cpf: "",
        dataNascimento: patient.dataNascimento,
        telefone: formatTelefone(patient.telefone),
        endereco: patient.endereco ?? "",
        profissao: patient.profissao ?? "",
        estadoCivil: patient.estadoCivil ?? "",
        consentimentoLgpd: patient.consentimentoLgpd ?? false,
      })
    } else {
      setForm(emptyForm)
    }
  }, [patient, open])

  function set(field: keyof FormState, value: string | boolean) {
    setForm((prev) => ({ ...prev, [field]: value }))
  }

  const mutation = useMutation({
    mutationFn: () => {
      if (isEdit) {
        return updatePatient(patient!.id, {
          nomeCompleto: form.nomeCompleto,
          telefone: form.telefone.replace(/\D/g, ""),
          endereco: form.endereco || undefined,
          profissao: form.profissao || undefined,
          estadoCivil: form.estadoCivil || undefined,
          consentimentoLgpd: form.consentimentoLgpd,
        })
      }
      return createPatient({
        nomeCompleto: form.nomeCompleto,
        email: form.email,
        cpf: form.cpf.replace(/\D/g, ""),
        dataNascimento: form.dataNascimento,
        telefone: form.telefone.replace(/\D/g, ""),
        endereco: form.endereco || undefined,
        profissao: form.profissao || undefined,
        estadoCivil: form.estadoCivil || undefined,
        consentimentoLgpd: form.consentimentoLgpd,
      })
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["patients"] })
      toast({
        title: isEdit ? "Paciente atualizado" : "Paciente cadastrado",
        description: isEdit
          ? "Os dados foram atualizados com sucesso."
          : "Novo paciente adicionado com sucesso.",
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
    mutation.mutate()
  }

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent className="w-full sm:max-w-lg overflow-y-auto">
        <SheetHeader className="mb-6">
          <SheetTitle className="flex items-center gap-2 font-primary">
            {isEdit ? <Pencil className="h-5 w-5 text-primary" /> : <UserPlus className="h-5 w-5 text-primary" />}
            {isEdit ? "Editar Paciente" : "Novo Paciente"}
          </SheetTitle>
          <SheetDescription className="font-secondary">
            {isEdit ? "Atualize os dados do paciente." : "Preencha os dados para cadastrar um novo paciente."}
          </SheetDescription>
        </SheetHeader>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-1.5">
            <Label htmlFor="nomeCompleto" className="font-primary text-sm">Nome completo *</Label>
            <Input
              id="nomeCompleto"
              value={form.nomeCompleto}
              onChange={(e) => set("nomeCompleto", e.target.value)}
              placeholder="Nome completo do paciente"
              required
              minLength={3}
              className="font-secondary"
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-1.5">
              <Label htmlFor="email" className="font-primary text-sm">E-mail *</Label>
              <Input
                id="email"
                type="email"
                value={form.email}
                onChange={(e) => set("email", e.target.value)}
                placeholder="email@exemplo.com"
                required={!isEdit}
                disabled={isEdit}
                className="font-secondary"
              />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="telefone" className="font-primary text-sm">Telefone *</Label>
              <Input
                id="telefone"
                value={form.telefone}
                onChange={(e) => set("telefone", formatTelefone(e.target.value))}
                placeholder="(11) 99999-9999"
                required
                className="font-secondary"
              />
            </div>
          </div>

          {!isEdit && (
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1.5">
                <Label htmlFor="cpf" className="font-primary text-sm">CPF *</Label>
                <Input
                  id="cpf"
                  value={form.cpf}
                  onChange={(e) => set("cpf", formatCpf(e.target.value))}
                  placeholder="000.000.000-00"
                  required
                  className="font-secondary"
                />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="dataNascimento" className="font-primary text-sm">Nascimento *</Label>
                <Input
                  id="dataNascimento"
                  type="date"
                  value={form.dataNascimento}
                  onChange={(e) => set("dataNascimento", e.target.value)}
                  required
                  className="font-secondary"
                />
              </div>
            </div>
          )}

          <div className="space-y-1.5">
            <Label htmlFor="endereco" className="font-primary text-sm">Endereço</Label>
            <Input
              id="endereco"
              value={form.endereco}
              onChange={(e) => set("endereco", e.target.value)}
              placeholder="Rua, número, bairro"
              className="font-secondary"
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-1.5">
              <Label htmlFor="profissao" className="font-primary text-sm">Profissão</Label>
              <Input
                id="profissao"
                value={form.profissao}
                onChange={(e) => set("profissao", e.target.value)}
                placeholder="Ex: Professora"
                className="font-secondary"
              />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="estadoCivil" className="font-primary text-sm">Estado civil</Label>
              <Input
                id="estadoCivil"
                value={form.estadoCivil}
                onChange={(e) => set("estadoCivil", e.target.value)}
                placeholder="Ex: Casada"
                className="font-secondary"
              />
            </div>
          </div>

          <div className="flex items-center gap-2 pt-1">
            <input
              id="lgpd"
              type="checkbox"
              checked={form.consentimentoLgpd}
              onChange={(e) => set("consentimentoLgpd", e.target.checked)}
              className="h-4 w-4 rounded border-border accent-primary"
            />
            <Label htmlFor="lgpd" className="font-secondary text-sm cursor-pointer">
              Consentimento LGPD obtido
            </Label>
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
