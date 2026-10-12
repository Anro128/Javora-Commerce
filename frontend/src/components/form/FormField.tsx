import type { ComponentProps } from 'react'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'

interface FormFieldProps extends ComponentProps<typeof Input> {
  label: string
  error?: string
  hint?: string
}

export function FormField({ label, error, hint, id, name, ...inputProps }: FormFieldProps) {
  const fieldId = id ?? name
  const messageId = `${fieldId}-message`

  return (
    <div className="flex flex-col gap-2">
      <Label htmlFor={fieldId}>{label}</Label>
      <Input
        id={fieldId}
        name={name}
        aria-invalid={error ? true : undefined}
        aria-describedby={error || hint ? messageId : undefined}
        {...inputProps}
      />
      {error ? (
        <p id={messageId} className="text-destructive text-sm">
          {error}
        </p>
      ) : hint ? (
        <p id={messageId} className="text-muted-foreground text-sm">
          {hint}
        </p>
      ) : null}
    </div>
  )
}
