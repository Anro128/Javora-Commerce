import { cn } from '@/lib/utils'

interface ProductImageProps {
  src?: string | null
  alt: string
  className?: string
}

// Shows a neutral tile with the first letter when a product has no photo yet.
export function ProductImage({ src, alt, className }: ProductImageProps) {
  if (src) {
    return <img src={src} alt={alt} loading="lazy" className={cn('size-full object-cover', className)} />
  }
  return (
    <div
      role="img"
      aria-label={alt}
      className={cn(
        'from-muted to-muted/40 text-muted-foreground flex size-full items-center justify-center bg-gradient-to-br text-4xl font-semibold',
        className,
      )}
    >
      {alt.trim().charAt(0).toUpperCase()}
    </div>
  )
}
