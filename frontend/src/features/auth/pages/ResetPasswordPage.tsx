import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation } from '@tanstack/react-query'
import { Loader2Icon } from 'lucide-react'
import { useForm } from 'react-hook-form'
import { Link, useNavigate, useSearchParams } from 'react-router'
import { toast } from 'sonner'
import { FormError } from '@/components/form/FormError'
import { FormField } from '@/components/form/FormField'
import { AuthCard } from '@/components/layout/AuthCard'
import { Button } from '@/components/ui/button'
import { resetPassword } from '@/features/auth/api'
import { resetPasswordSchema, type ResetPasswordValues } from '@/features/auth/schemas'
import { errorMessage } from '@/lib/form'

export function ResetPasswordPage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token')

  const reset = useMutation({
    mutationFn: (newPassword: string) => resetPassword(token ?? '', newPassword),
    onSuccess: () => {
      toast.success('Your password has been reset. You can now log in.')
      navigate('/login', { replace: true })
    },
  })

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<ResetPasswordValues>({ resolver: zodResolver(resetPasswordSchema) })

  if (!token) {
    return (
      <AuthCard
        title="Invalid reset link"
        footer={
          <Link
            to="/forgot-password"
            className="text-foreground font-medium underline-offset-4 hover:underline"
          >
            Request a new link
          </Link>
        }
      >
        <p className="text-muted-foreground text-sm">This password reset link is missing its token.</p>
      </AuthCard>
    )
  }

  return (
    <AuthCard title="Choose a new password" description="Pick a strong password you have not used before.">
      <form
        onSubmit={handleSubmit(({ newPassword }) => reset.mutate(newPassword))}
        className="flex flex-col gap-4"
        noValidate
      >
        <FormError message={errorMessage(reset.error)} />
        <FormField
          label="New password"
          type="password"
          autoComplete="new-password"
          hint="At least 8 characters with a letter and a number."
          error={errors.newPassword?.message}
          {...register('newPassword')}
        />
        <FormField
          label="Confirm new password"
          type="password"
          autoComplete="new-password"
          error={errors.confirmPassword?.message}
          {...register('confirmPassword')}
        />
        <Button type="submit" disabled={reset.isPending}>
          {reset.isPending ? <Loader2Icon className="animate-spin" /> : null}
          Reset password
        </Button>
      </form>
    </AuthCard>
  )
}
