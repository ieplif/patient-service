import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom"
import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { AppLayout } from "@/components/shared/AppLayout"
import { LoginPage } from "@/pages/LoginPage"
import { DashboardPage } from "@/pages/DashboardPage"
import { PacientesPage } from "@/pages/PacientesPage"
import { AgendamentosPage } from "@/pages/AgendamentosPage"
import { PagamentosPage } from "@/pages/PagamentosPage"
import { AssinaturasPage } from "@/pages/AssinaturasPage"
import { ProfissionaisPage } from "@/pages/ProfissionaisPage"
import { useAuthStore } from "@/store/authStore"

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      retry: 1,
    },
  },
})

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { token } = useAuthStore()
  if (!token) return <Navigate to="/login" replace />
  return <>{children}</>
}

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route
            element={
              <ProtectedRoute>
                <AppLayout />
              </ProtectedRoute>
            }
          >
            <Route path="/dashboard" element={<DashboardPage />} />
            <Route path="/pacientes" element={<PacientesPage />} />
            <Route path="/agendamentos" element={<AgendamentosPage />} />
            <Route path="/pagamentos" element={<PagamentosPage />} />
            <Route path="/assinaturas" element={<AssinaturasPage />} />
            <Route path="/profissionais" element={<ProfissionaisPage />} />
            <Route path="/" element={<Navigate to="/dashboard" replace />} />
          </Route>
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>
  )
}
