import { NavLink, Outlet } from 'react-router'
import { cn } from '@/lib/utils'

const accountNavItems = [
  { to: '/account', label: 'Profile', end: true },
  { to: '/account/addresses', label: 'Addresses', end: false },
]

export function AccountLayout() {
  return (
    <div className="grid gap-8 md:grid-cols-[200px_1fr]">
      <aside>
        <h1 className="mb-4 text-2xl font-bold tracking-tight">My account</h1>
        <nav className="flex gap-2 md:flex-col" aria-label="Account sections">
          {accountNavItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className={({ isActive }) =>
                cn(
                  'rounded-md px-3 py-2 text-sm font-medium transition-colors',
                  isActive ? 'bg-primary text-primary-foreground' : 'text-muted-foreground hover:bg-muted',
                )
              }
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
      </aside>
      <div className="min-w-0">
        <Outlet />
      </div>
    </div>
  )
}
