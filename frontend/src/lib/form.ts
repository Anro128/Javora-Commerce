import type { FieldValues, Path, UseFormSetError } from 'react-hook-form'
import { ApiError } from '@/api/client'

// Copies per-field validation messages from the API onto the matching form fields.
export function applyFieldErrors<T extends FieldValues>(
  error: unknown,
  setError: UseFormSetError<T>,
): boolean {
  if (!(error instanceof ApiError)) {
    return false
  }
  const entries = Object.entries(error.fieldErrors)
  entries.forEach(([field, message]) => setError(field as Path<T>, { type: 'server', message }))
  return entries.length > 0
}

export function errorMessage(error: unknown): string | null {
  if (!error) {
    return null
  }
  return error instanceof Error ? error.message : 'An unexpected error occurred'
}
