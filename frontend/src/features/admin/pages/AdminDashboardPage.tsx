import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'

const placeholderStats = ['Total revenue', 'Orders', 'Customers', 'Products']

export function AdminDashboardPage() {
  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-2xl font-bold tracking-tight">Dashboard</h1>
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {placeholderStats.map((label) => (
          <Card key={label}>
            <CardHeader>
              <CardTitle className="text-muted-foreground text-sm font-medium">{label}</CardTitle>
            </CardHeader>
            <CardContent className="text-2xl font-bold">&mdash;</CardContent>
          </Card>
        ))}
      </div>
    </div>
  )
}
