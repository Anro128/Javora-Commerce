import { MenuIcon, ShoppingCartIcon, UserIcon } from 'lucide-react'
import { Link, NavLink } from 'react-router'
import { Button } from '@/components/ui/button'
import { Sheet, SheetContent, SheetHeader, SheetTitle, SheetTrigger } from '@/components/ui/sheet'
import { cn } from '@/lib/utils'

const navItems = [
  { to: '/', label: 'Home' },
  { to: '/products', label: 'Products' },
]

function navClassName({ isActive }: { isActive: boolean }) {
  return cn(
    'text-sm font-medium transition-colors hover:text-foreground',
    isActive ? 'text-foreground' : 'text-muted-foreground',
  )
}

export function Header() {
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
            <Link to="/cart" aria-label="Cart">
              <ShoppingCartIcon />
            </Link>
          </Button>
          <Button variant="ghost" size="icon" asChild>
            <Link to="/account" aria-label="Account">
              <UserIcon />
            </Link>
          </Button>
        </div>
      </div>
    </header>
  )
}
