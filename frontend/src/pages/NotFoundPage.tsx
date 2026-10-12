import { Link } from 'react-router'
import { Button } from '@/components/ui/button'

export function NotFoundPage() {
  return (
    <section className="flex flex-col items-center gap-4 py-16 text-center">
      <p className="text-muted-foreground text-6xl font-bold">404</p>
      <h1 className="text-2xl font-semibold">Page not found</h1>
      <p className="text-muted-foreground">The page you are looking for does not exist or has been moved.</p>
      <Button variant="outline" asChild>
        <Link to="/">Back to home</Link>
      </Button>
    </section>
  )
}
