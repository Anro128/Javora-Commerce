import { LayoutDashboardIcon, LogOutIcon, MenuIcon, ShoppingCartIcon, UserIcon } from 'lucide-react'
import { Link, NavLink, useNavigate } from 'react-router'
import { Button } from '@/components/ui/button'
import { Sheet, SheetContent, SheetHeader, SheetTitle, SheetTrigger } from '@/components/ui/sheet'
import { useLogout } from '@/features/auth/hooks'
import { useAuthStore } from '@/features/auth/store'
import { useCartCount } from '@/features/cart/hooks'
import { cn } from '@/lib/utils'

const navItems = [
  { to: '/', label: 'Home' },
  { to: '/products', label: 'Products' },
]

function navClassName({ isActive }: { isActive: boolean }) {
  return cn(
    'hover:text-foreground text-sm font-medium transition-colors',
    isActive ? 'text-foreground' : 'text-muted-foreground',
  )
}

function UserActions() {
  const navigate = useNavigate()
  const status = useAuthStore((state) => state.status)
  const user = useAuthStore((state) => state.user)
  const logout = useLogout()

  if (status === 'loading') {
    return <div className="h-9 w-24" aria-hidden />
  }

  if (status === 'anonymous' || !user) {
    return (
      <>
        <Button variant="ghost" size="sm" asChild>
          <Link to="/login">Log in</Link>
        </Button>
        <Button size="sm" asChild className="hidden sm:inline-flex">
          <Link to="/register">Sign up</Link>
        </Button>
      </>
    )
  }

  return (
    <>
      {user.role === 'ADMIN' ? (
        <Button variant="ghost" size="icon" asChild>
          <Link to="/admin" aria-label="Admin dashboard">
            <LayoutDashboardIcon />
          </Link>
        </Button>
      ) : null}
      <Button variant="ghost" size="icon" asChild>
        <Link to="/account" aria-label="Account">
          <UserIcon />
        </Link>
      </Button>
      <Button
        variant="ghost"
        size="icon"
        aria-label="Log out"
        disabled={logout.isPending}
        onClick={() => logout.mutate(undefined, { onSettled: () => navigate('/') })}
      >
        <LogOutIcon />
      </Button>
    </>
  )
}

export function Header() {
  const cartCount = useCartCount()

  return (
    <header className="bg-background/95 sticky top-0 z-40 border-b backdrop-blur">
      <div className="container mx-auto flex h-16 items-center gap-6 px-4">
        <Sheet>
          <SheetTrigger asChild>
            <Button variant="ghost" size="icon" className="md:hidden" aria-label="Open menu">
              <MenuIcon />
            </Button>
          </SheetTrigger>
          <SheetContent side="left">
            <SheetHeader>
              <SheetTitle>Javora</SheetTitle>
            </SheetHeader>
            <nav className="flex flex-col gap-4 px-4">
              {navItems.map((item) => (
                <NavLink key={item.to} to={item.to} end className={navClassName}>
                  {item.label}
                </NavLink>
              ))}
            </nav>
          </SheetContent>
        </Sheet>

        <Link to="/" className="text-xl font-bold tracking-tight">
          Javora
        </Link>

        <nav className="hidden items-center gap-6 md:flex">
          {navItems.map((item) => (
            <NavLink key={item.to} to={item.to} end className={navClassName}>
              {item.label}
            </NavLink>
          ))}
        </nav>

        <div className="ml-auto flex items-center gap-1">
          <Button variant="ghost" size="icon" asChild>
            <Link
              to="/cart"
              aria-label={cartCount > 0 ? `Cart, ${cartCount} items` : 'Cart'}
              className="relative"
            >
              <ShoppingCartIcon />
              {cartCount > 0 ? (
                <span className="bg-primary text-primary-foreground absolute -top-1 -right-1 flex min-w-5 items-center justify-center rounded-full px-1 text-[11px] leading-5 font-semibold">
                  {cartCount > 99 ? '99+' : cartCount}
                </span>
              ) : null}
            </Link>
          </Button>
          <UserActions />
        </div>
      </div>
    </header>
  )
}
