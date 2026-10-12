import { Link } from 'react-router'
import { Button } from '@/components/ui/button'

export function HomePage() {
  return (
    <section className="flex flex-col items-center gap-6 py-16 text-center">
      <h1 className="text-4xl font-bold tracking-tight md:text-5xl">Welcome to Javora</h1>
      <p className="text-muted-foreground max-w-xl text-lg">
        Discover quality products at the best prices, delivered right to your door.
      </p>
      <Button size="lg" asChild>
        <Link to="/products">Start shopping</Link>
      </Button>
    </section>
  )
}
