import { zodResolver } from '@hookform/resolvers/zod'
import { Loader2Icon } from 'lucide-react'
import { useForm } from 'react-hook-form'
import { Link, Navigate, useNavigate } from 'react-router'
import { toast } from 'sonner'
import { FormError } from '@/components/form/FormError'
import { FormField } from '@/components/form/FormField'
import { AuthCard } from '@/components/layout/AuthCard'
import { Button } from '@/components/ui/button'
import { useRegister } from '@/features/auth/hooks'
import { registerSchema, type RegisterValues } from '@/features/auth/schemas'
import { useAuthStore } from '@/features/auth/store'
import { applyFieldErrors, errorMessage } from '@/lib/form'

export function RegisterPage() {
  const navigate = useNavigate()
  const status = useAuthStore((state) => state.status)
  const registerAccount = useRegister()

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<RegisterValues>({ resolver: zodResolver(registerSchema) })

  if (status === 'authenticated' && !registerAccount.isPending && !registerAccount.isSuccess) {
    return <Navigate to="/" replace />
  }

  const onSubmit = ({ fullName, email, phone, password }: RegisterValues) => {
    registerAccount.mutate(
      { fullName, email, phone: phone || undefined, password },
      {
        onSuccess: () => {
          toast.success('Account created. Check your inbox to verify your email.')
          navigate('/', { replace: true })
        },
        onError: (error) => {
          applyFieldErrors(error, setError)
        },
      },
    )
  }

  return (
    <AuthCard
      title="Create an account"
      description="Start shopping in less than a minute."
      footer={
        <>
          Already have an account?{' '}
          <Link to="/login" className="text-foreground font-medium underline-offset-4 hover:underline">
            Log in
          </Link>
        </>
      }
    >
      <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4" noValidate>
        <FormError message={errorMessage(registerAccount.error)} />
        <FormField
          label="Full name"
          autoComplete="name"
          error={errors.fullName?.message}
          {...register('fullName')}
        />
        <FormField
          label="Email"
          type="email"
          autoComplete="email"
          error={errors.email?.message}
          {...register('email')}
        />
        <FormField
          label="Phone (optional)"
          type="tel"
          autoComplete="tel"
          error={errors.phone?.message}
          {...register('phone')}
        />
        <FormField
          label="Password"
          type="password"
          autoComplete="new-password"
          hint="At least 8 characters with a letter and a number."
          error={errors.password?.message}
          {...register('password')}
        />
        <FormField
          label="Confirm password"
          type="password"
          autoComplete="new-password"
          error={errors.confirmPassword?.message}
          {...register('confirmPassword')}
        />
        <Button type="submit" disabled={registerAccount.isPending}>
          {registerAccount.isPending ? <Loader2Icon className="animate-spin" /> : null}
          Create account
        </Button>
      </form>
    </AuthCard>
  )
}
