import { Loader2Icon } from 'lucide-react'

export function PageSpinner() {
  return (
    <div className="flex min-h-[40vh] items-center justify-center" role="status" aria-label="Loading">
      <Loader2Icon className="text-muted-foreground size-8 animate-spin" />
    </div>
  )
}
