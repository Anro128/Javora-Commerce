import { zodResolver } from '@hookform/resolvers/zod'
import { Loader2Icon } from 'lucide-react'
import { useForm } from 'react-hook-form'
import { Link, Navigate, useLocation, useNavigate } from 'react-router'
import { FormError } from '@/components/form/FormError'
import { FormField } from '@/components/form/FormField'
import { AuthCard } from '@/components/layout/AuthCard'
import { Button } from '@/components/ui/button'
import { useLogin } from '@/features/auth/hooks'
import { loginSchema, type LoginValues } from '@/features/auth/schemas'
import { useAuthStore } from '@/features/auth/store'
import { errorMessage } from '@/lib/form'

interface LocationState {
  from?: { pathname: string; search?: string }
}

export function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const status = useAuthStore((state) => state.status)
  const login = useLogin()
  const from = (location.state as LocationState | null)?.from
  const redirectTo = from ? `${from.pathname}${from.search ?? ''}` : '/'

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginValues>({ resolver: zodResolver(loginSchema) })

  if (status === 'authenticated' && !login.isPending) {
    return <Navigate to={redirectTo} replace />
  }

  const onSubmit = (values: LoginValues) => {
    login.mutate(values, { onSuccess: () => navigate(redirectTo, { replace: true }) })
  }

  return (
    <AuthCard
      title="Log in"
      description="Welcome back. Enter your details to continue."
      footer={
        <>
          New to Javora?{' '}
          <Link to="/register" className="text-foreground font-medium underline-offset-4 hover:underline">
            Create an account
          </Link>
        </>
      }
    >
      <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4" noValidate>
        <FormError message={errorMessage(login.error)} />
        <FormField
          label="Email"
          type="email"
          autoComplete="email"
          error={errors.email?.message}
          {...register('email')}
        />
        <FormField
          label="Password"
          type="password"
          autoComplete="current-password"
          error={errors.password?.message}
          {...register('password')}
        />
        <div className="flex justify-end">
          <Link
            to="/forgot-password"
            className="text-muted-foreground text-sm underline-offset-4 hover:underline"
          >
            Forgot password?
          </Link>
        </div>
        <Button type="submit" disabled={login.isPending}>
          {login.isPending ? <Loader2Icon className="animate-spin" /> : null}
          Log in
        </Button>
      </form>
    </AuthCard>
  )
}
