import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation } from '@tanstack/react-query'
import { Loader2Icon, MailWarningIcon } from 'lucide-react'
import { useForm } from 'react-hook-form'
import { toast } from 'sonner'
import { FormError } from '@/components/form/FormError'
import { FormField } from '@/components/form/FormField'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { useUpdateProfile } from '@/features/account/hooks'
import { changePassword, resendVerification } from '@/features/auth/api'
import {
  changePasswordSchema,
  profileSchema,
  type ChangePasswordValues,
  type ProfileValues,
} from '@/features/auth/schemas'
import { useAuthStore } from '@/features/auth/store'
import { applyFieldErrors, errorMessage } from '@/lib/form'

function ProfileForm() {
  const user = useAuthStore((state) => state.user)
  const updateProfile = useUpdateProfile()

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<ProfileValues>({
    resolver: zodResolver(profileSchema),
    defaultValues: { fullName: user?.fullName ?? '', phone: user?.phone ?? '' },
  })

  const onSubmit = ({ fullName, phone }: ProfileValues) => {
    updateProfile.mutate(
      { fullName, phone: phone || undefined },
      {
        onSuccess: () => toast.success('Profile updated'),
        onError: (error) => applyFieldErrors(error, setError),
      },
    )
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Profile</CardTitle>
        <CardDescription>Your personal details used for orders and delivery.</CardDescription>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit(onSubmit)} className="flex max-w-md flex-col gap-4" noValidate>
          <FormError message={errorMessage(updateProfile.error)} />
          <FormField label="Email" value={user?.email ?? ''} disabled readOnly />
          <FormField label="Full name" error={errors.fullName?.message} {...register('fullName')} />
          <FormField label="Phone" type="tel" error={errors.phone?.message} {...register('phone')} />
          <Button type="submit" disabled={updateProfile.isPending} className="self-start">
            {updateProfile.isPending ? <Loader2Icon className="animate-spin" /> : null}
            Save changes
          </Button>
        </form>
      </CardContent>
    </Card>
  )
}

function ChangePasswordForm() {
  const change = useMutation({
    mutationFn: (values: ChangePasswordValues) => changePassword(values.currentPassword, values.newPassword),
  })

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<ChangePasswordValues>({ resolver: zodResolver(changePasswordSchema) })

  const onSubmit = (values: ChangePasswordValues) => {
    change.mutate(values, {
      onSuccess: () => {
        toast.success('Password changed. Other devices have been signed out.')
        reset()
      },
    })
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Change password</CardTitle>
        <CardDescription>Changing your password signs you out of all other devices.</CardDescription>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit(onSubmit)} className="flex max-w-md flex-col gap-4" noValidate>
          <FormError message={errorMessage(change.error)} />
          <FormField
            label="Current password"
            type="password"
            autoComplete="current-password"
            error={errors.currentPassword?.message}
            {...register('currentPassword')}
          />
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
          <Button type="submit" disabled={change.isPending} className="self-start">
            {change.isPending ? <Loader2Icon className="animate-spin" /> : null}
            Change password
          </Button>
        </form>
      </CardContent>
    </Card>
  )
}

function VerificationBanner() {
  const user = useAuthStore((state) => state.user)
  const resend = useMutation({
    mutationFn: () => resendVerification(),
    onSuccess: () => toast.success('Verification email sent. Please check your inbox.'),
    onError: (error) => toast.error(errorMessage(error) ?? 'Could not send the email'),
  })

  if (!user || user.emailVerified) {
    return null
  }

  return (
    <div className="flex flex-col gap-3 rounded-lg border border-amber-300 bg-amber-50 p-4 text-amber-900 sm:flex-row sm:items-center sm:justify-between dark:border-amber-700 dark:bg-amber-950 dark:text-amber-100">
      <div className="flex items-start gap-3">
        <MailWarningIcon className="mt-0.5 size-5 shrink-0" />
        <p className="text-sm">Your email address is not verified yet. Verify it to secure your account.</p>
      </div>
      <Button size="sm" variant="outline" disabled={resend.isPending} onClick={() => resend.mutate()}>
        Resend email
      </Button>
    </div>
  )
}

export function ProfilePage() {
  return (
    <div className="flex flex-col gap-6">
      <VerificationBanner />
      <ProfileForm />
      <ChangePasswordForm />
    </div>
  )
}
