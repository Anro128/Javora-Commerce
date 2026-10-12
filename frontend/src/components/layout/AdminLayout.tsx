import { Link, NavLink, Outlet } from 'react-router'
import { adminNavItems } from '@/components/layout/adminNav'
import { AdminSidebar } from '@/components/layout/AdminSidebar'
import { cn } from '@/lib/utils'

function AdminMobileNav() {
  return (
    <nav
      className="flex items-center gap-1 overflow-x-auto border-b px-3 py-2 md:hidden"
      aria-label="Admin sections"
    >
      <Link to="/" className="text-muted-foreground mr-2 shrink-0 text-xs underline-offset-4 hover:underline">
        Shop
      </Link>
      {adminNavItems.map(({ to, label }) => (
        <NavLink
          key={to}
          to={to}
          end
          className={({ isActive }) =>
            cn(
              'shrink-0 rounded-md px-3 py-1.5 text-sm font-medium',
              isActive ? 'bg-primary text-primary-foreground' : 'text-muted-foreground',
            )
          }
        >
          {label}
        </NavLink>
      ))}
    </nav>
  )
}

export function AdminLayout() {
  return (
    <div className="flex min-h-svh flex-col md:flex-row">
      <AdminSidebar />
      <div className="flex min-w-0 flex-1 flex-col">
        <AdminMobileNav />
        <main className="flex-1 overflow-x-hidden p-4 md:p-6">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
