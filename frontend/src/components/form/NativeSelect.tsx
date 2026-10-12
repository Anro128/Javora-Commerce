import type { ComponentProps } from 'react'
import { Label } from '@/components/ui/label'
import { cn } from '@/lib/utils'

export function NativeSelect({ className, ...props }: ComponentProps<'select'>) {
  return (
    <select
      className={cn(
        'border-input bg-background h-9 w-full rounded-md border px-3 text-sm shadow-xs outline-none',
        'focus-visible:border-ring focus-visible:ring-ring/50 focus-visible:ring-[3px]',
        'disabled:cursor-not-allowed disabled:opacity-50',
        className,
      )}
      {...props}
    />
  )
}

interface SelectFieldProps extends ComponentProps<'select'> {
  label: string
  error?: string
}

export function SelectField({ label, error, id, name, children, ...props }: SelectFieldProps) {
  const fieldId = id ?? name
  return (
    <div className="flex flex-col gap-2">
      <Label htmlFor={fieldId}>{label}</Label>
      <NativeSelect id={fieldId} name={name} aria-invalid={error ? true : undefined} {...props}>
        {children}
      </NativeSelect>
      {error ? <p className="text-destructive text-sm">{error}</p> : null}
    </div>
  )
}
