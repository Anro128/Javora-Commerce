import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation } from '@tanstack/react-query'
import { Loader2Icon } from 'lucide-react'
import { useForm } from 'react-hook-form'
import { Link } from 'react-router'
import { FormError } from '@/components/form/FormError'
import { FormField } from '@/components/form/FormField'
import { AuthCard } from '@/components/layout/AuthCard'
import { Button } from '@/components/ui/button'
import { forgotPassword } from '@/features/auth/api'
import { forgotPasswordSchema, type ForgotPasswordValues } from '@/features/auth/schemas'
import { errorMessage } from '@/lib/form'

export function ForgotPasswordPage() {
  const request = useMutation({ mutationFn: (email: string) => forgotPassword(email) })

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<ForgotPasswordValues>({ resolver: zodResolver(forgotPasswordSchema) })

  const backToLogin = (
    <Link to="/login" className="text-foreground font-medium underline-offset-4 hover:underline">
      Back to log in
    </Link>
  )

  if (request.isSuccess) {
    return (
      <AuthCard title="Check your email" footer={backToLogin}>
        <p className="text-muted-foreground text-sm">
          If an account exists for that email, we have sent a link to reset your password. The link is valid
          for one hour.
        </p>
      </AuthCard>
    )
  }

  return (
    <AuthCard
      title="Forgot your password?"
      description="Enter your email and we will send you a link to reset it."
      footer={backToLogin}
    >
      <form
        onSubmit={handleSubmit(({ email }) => request.mutate(email))}
        className="flex flex-col gap-4"
        noValidate
      >
        <FormError message={errorMessage(request.error)} />
        <FormField
          label="Email"
          type="email"
          autoComplete="email"
          error={errors.email?.message}
          {...register('email')}
        />
        <Button type="submit" disabled={request.isPending}>
          {request.isPending ? <Loader2Icon className="animate-spin" /> : null}
          Send reset link
        </Button>
      </form>
    </AuthCard>
  )
}
