export function FormError({ message }: { message?: string | null }) {
  if (!message) {
    return null
  }
  return (
    <p
      role="alert"
      className="border-destructive/30 bg-destructive/10 text-destructive rounded-md border px-3 py-2 text-sm"
    >
      {message}
    </p>
  )
}
