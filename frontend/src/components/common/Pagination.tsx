import { ChevronLeftIcon, ChevronRightIcon } from 'lucide-react'
import { Button } from '@/components/ui/button'

interface PaginationProps {
  page: number
  totalPages: number
  onPageChange: (page: number) => void
}

// Pages are zero based in the API and shown one based to people.
export function Pagination({ page, totalPages, onPageChange }: PaginationProps) {
  if (totalPages <= 1) {
    return null
  }

  return (
    <nav className="flex items-center justify-center gap-4" aria-label="Pagination">
      <Button variant="outline" size="sm" disabled={page <= 0} onClick={() => onPageChange(page - 1)}>
        <ChevronLeftIcon /> Previous
      </Button>
      <span className="text-muted-foreground text-sm">
        Page {page + 1} of {totalPages}
      </span>
      <Button
        variant="outline"
        size="sm"
        disabled={page >= totalPages - 1}
        onClick={() => onPageChange(page + 1)}
      >
        Next <ChevronRightIcon />
      </Button>
    </nav>
  )
}
