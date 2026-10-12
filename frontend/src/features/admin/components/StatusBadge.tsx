import type { ProductStatus } from '@/features/admin/types'
import { cn } from '@/lib/utils'

const styles: Record<ProductStatus, string> = {
  ACTIVE: 'bg-green-100 text-green-800 dark:bg-green-950 dark:text-green-300',
  DRAFT: 'bg-amber-100 text-amber-800 dark:bg-amber-950 dark:text-amber-300',
  ARCHIVED: 'bg-muted text-muted-foreground',
}

const labels: Record<ProductStatus, string> = {
  ACTIVE: 'Active',
  DRAFT: 'Draft',
  ARCHIVED: 'Archived',
}

export function StatusBadge({ status }: { status: ProductStatus }) {
  return (
    <span className={cn('inline-flex rounded-full px-2 py-0.5 text-xs font-medium', styles[status])}>
      {labels[status]}
    </span>
  )
}
