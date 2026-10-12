const currentYear = new Date().getFullYear()

export function Footer() {
  return (
    <footer className="border-t">
      <div className="text-muted-foreground container mx-auto flex flex-col items-center justify-between gap-2 px-4 py-6 text-sm md:flex-row">
        <p>&copy; {currentYear} Javora Commerce. All rights reserved.</p>
        <p>Secure payments &middot; Fast shipping</p>
      </div>
    </footer>
  )
}
