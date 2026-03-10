import { useState } from "react"
import { useNavigate } from "react-router-dom"
import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import { z } from "zod"
import { Loader2, Heart } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { loginRequest } from "@/api/auth"
import { useAuthStore } from "@/store/authStore"

const loginSchema = z.object({
  email: z.string().email("E-mail inválido"),
  senha: z.string().min(1, "Senha obrigatória"),
})

type LoginFormValues = z.infer<typeof loginSchema>

export function LoginPage() {
  const navigate = useNavigate()
  const { login } = useAuthStore()
  const [error, setError] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
  })

  async function onSubmit(values: LoginFormValues) {
    setError(null)
    try {
      const res = await loginRequest(values.email, values.senha)
      login(res.token, { nome: res.nome, email: res.email, role: res.role })
      navigate("/dashboard")
    } catch {
      setError("Credenciais inválidas. Verifique seu e-mail e senha.")
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-background relative overflow-hidden">
      {/* Subtle background gradient */}
      <div
        className="absolute inset-0 pointer-events-none"
        style={{
          background:
            "linear-gradient(135deg, hsl(140 12% 52% / 0.08) 0%, hsl(202 50% 80% / 0.15) 100%)",
        }}
      />

      {/* Decorative circles */}
      <div className="absolute -top-32 -left-32 h-96 w-96 rounded-full bg-primary/5 blur-3xl pointer-events-none" />
      <div className="absolute -bottom-32 -right-32 h-96 w-96 rounded-full bg-secondary/20 blur-3xl pointer-events-none" />

      <Card className="relative w-full max-w-sm border border-border/60 shadow-medium animate-fade-in">
        <CardHeader className="text-center pb-4">
          {/* Logo */}
          <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-gradient-to-br from-primary to-primary/70 shadow-soft">
            <Heart className="h-7 w-7 text-primary-foreground" />
          </div>
          <CardTitle className="text-2xl font-bold font-primary text-foreground">
            Humaniza
          </CardTitle>
          <CardDescription className="font-secondary text-muted-foreground">
            Acesse o sistema da clínica
          </CardDescription>
        </CardHeader>

        <CardContent>
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <div className="space-y-1.5">
              <Label htmlFor="email" className="font-primary text-sm font-medium text-foreground">
                E-mail
              </Label>
              <Input
                id="email"
                type="email"
                placeholder="voce@exemplo.com"
                className="bg-card border-border/80 focus:border-primary focus:ring-primary/20 font-secondary"
                {...register("email")}
              />
              {errors.email && (
                <p className="text-xs text-destructive font-secondary">{errors.email.message}</p>
              )}
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="senha" className="font-primary text-sm font-medium text-foreground">
                Senha
              </Label>
              <Input
                id="senha"
                type="password"
                placeholder="••••••••"
                className="bg-card border-border/80 focus:border-primary focus:ring-primary/20 font-secondary"
                {...register("senha")}
              />
              {errors.senha && (
                <p className="text-xs text-destructive font-secondary">{errors.senha.message}</p>
              )}
            </div>

            {error && (
              <div className="rounded-lg bg-destructive/10 border border-destructive/20 p-3">
                <p className="text-sm text-destructive text-center font-secondary">{error}</p>
              </div>
            )}

            <Button
              type="submit"
              className="w-full bg-primary hover:bg-primary/90 text-primary-foreground shadow-soft hover:shadow-medium transition-all duration-200 font-primary font-semibold"
              disabled={isSubmitting}
            >
              {isSubmitting ? (
                <>
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  Entrando...
                </>
              ) : (
                "Entrar"
              )}
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  )
}
