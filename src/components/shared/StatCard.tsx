import type { LucideIcon } from "lucide-react"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Skeleton } from "@/components/ui/skeleton"

interface StatCardProps {
  title: string
  value?: number | string
  icon: LucideIcon
  isLoading?: boolean
  description?: string
  accent?: "sage" | "blue" | "earth" | "beige"
}

const accentStyles = {
  sage: {
    icon: "bg-gradient-to-br from-primary/15 to-primary/5 text-primary",
    bar: "from-primary to-primary/60",
  },
  blue: {
    icon: "bg-gradient-to-br from-secondary/40 to-secondary/10 text-[hsl(202,40%,50%)]",
    bar: "from-secondary to-secondary/60",
  },
  earth: {
    icon: "bg-gradient-to-br from-accent/20 to-accent/5 text-accent",
    bar: "from-accent to-accent/60",
  },
  beige: {
    icon: "bg-gradient-to-br from-[hsl(var(--rosy-beige))/40] to-[hsl(var(--rosy-beige))/10] text-[hsl(27,20%,50%)]",
    bar: "from-[hsl(var(--rosy-beige))] to-[hsl(var(--rosy-beige))/60]",
  },
}

export function StatCard({
  title,
  value,
  icon: Icon,
  isLoading,
  description,
  accent = "sage",
}: StatCardProps) {
  const styles = accentStyles[accent]

  return (
    <Card className="relative overflow-hidden border border-border/60 shadow-soft hover:shadow-medium transition-shadow duration-300">
      {/* Colored top bar */}
      <div className={`absolute top-0 left-0 right-0 h-1 bg-gradient-to-r ${styles.bar}`} />

      <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2 pt-5">
        <CardTitle className="text-sm font-semibold font-primary text-muted-foreground">
          {title}
        </CardTitle>
        <div className={`flex h-9 w-9 items-center justify-center rounded-xl ${styles.icon}`}>
          <Icon className="h-4 w-4" />
        </div>
      </CardHeader>

      <CardContent>
        {isLoading ? (
          <Skeleton className="h-8 w-28 mt-1" />
        ) : (
          <div className="text-2xl font-bold font-primary text-foreground">{value ?? "—"}</div>
        )}
        {description && (
          <p className="text-xs text-muted-foreground mt-1 font-secondary">{description}</p>
        )}
      </CardContent>
    </Card>
  )
}
