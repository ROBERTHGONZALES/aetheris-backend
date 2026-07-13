import { Link, useLocation } from "wouter";
import { useUser, useLogout } from "@/hooks/use-auth";
import { LayoutDashboard, Receipt, CheckSquare, Briefcase, FileSpreadsheet, Building2, ShieldAlert, LogOut, Loader2, BotMessageSquare } from "lucide-react";
import { cn } from "@/lib/utils";
import { AriaChat } from "@/components/aria-chat";

const NavItem = ({ href, icon: Icon, children }: { href: string, icon: any, children: React.ReactNode }) => {
  const [location] = useLocation();
  const isActive = location.startsWith(href);
  
  return (
    <Link href={href}>
      <div className={cn(
        "flex items-center gap-3 rounded-lg px-3 py-2 text-sm transition-all hover:text-primary cursor-pointer",
        isActive ? "bg-sidebar-accent text-sidebar-accent-foreground font-medium" : "text-sidebar-foreground/70"
      )}>
        <Icon className="h-4 w-4" />
        {children}
      </div>
    </Link>
  );
};

export function AppLayout({ children }: { children: React.ReactNode }) {
  const user = useUser();
  const logout = useLogout();

  if (!user) {
    // If somehow reached without user, let it be. App handles redirect.
    return <div className="h-screen w-full flex items-center justify-center"><Loader2 className="animate-spin" /></div>;
  }

  const isAuditor = user.rol === "AUDITOR";
  const isAdmin = user.rol === "ADMIN";
  const isAprobador = user.rol === "APROBADOR" || isAdmin;
  const isContador = user.rol === "CONTADOR" || isAdmin;

  return (
    <div className="flex min-h-screen w-full flex-col bg-background md:flex-row">
      <aside className="fixed inset-y-0 left-0 z-10 hidden w-64 flex-col border-r bg-sidebar md:flex">
        <div className="flex h-14 items-center border-b border-sidebar-border px-6">
          <Link href="/dashboard" className="flex items-center gap-2 font-semibold">
            <div className="h-6 w-6 rounded bg-primary-foreground text-primary flex items-center justify-center font-bold text-xs tracking-tighter">
              AE
            </div>
            <span className="tracking-wide text-sidebar-foreground">AETHERIS</span>
          </Link>
        </div>
        <div className="flex-1 overflow-auto py-4 flex flex-col gap-1 px-3">
          <NavItem href="/dashboard" icon={LayoutDashboard}>Dashboard</NavItem>
          
          {(isContador || isAprobador) && (
            <>
              <div className="mt-4 mb-2 px-4 text-xs font-semibold uppercase tracking-wider text-sidebar-foreground/50">Operaciones</div>
              <NavItem href="/transacciones" icon={Receipt}>Transacciones</NavItem>
              {isAprobador && (
                <NavItem href="/aprobaciones" icon={CheckSquare}>Aprobaciones</NavItem>
              )}
            </>
          )}

          {(isContador || isAdmin) && (
            <>
              <div className="mt-4 mb-2 px-4 text-xs font-semibold uppercase tracking-wider text-sidebar-foreground/50">Finanzas</div>
              <NavItem href="/presupuesto" icon={Briefcase}>Presupuesto</NavItem>
              <NavItem href="/conciliacion" icon={FileSpreadsheet}>Conciliación</NavItem>
            </>
          )}

          {(isAdmin || isAuditor) && (
            <>
              <div className="mt-4 mb-2 px-4 text-xs font-semibold uppercase tracking-wider text-sidebar-foreground/50">Sistema</div>
              {isAdmin && <NavItem href="/sedes" icon={Building2}>Sedes</NavItem>}
              {(isAdmin || isAuditor) && <NavItem href="/auditoria" icon={ShieldAlert}>Auditoría</NavItem>}
            </>
          )}

          <div className="mt-4 mb-2 px-4 text-xs font-semibold uppercase tracking-wider text-sidebar-foreground/50">Inteligencia</div>
          <NavItem href="/aria" icon={BotMessageSquare}>ARIA — Chat IA</NavItem>
        </div>
        
        <div className="mt-auto p-4 border-t border-sidebar-border">
          <div className="flex items-center gap-3 mb-4">
            <div className="h-8 w-8 rounded-full bg-sidebar-accent flex items-center justify-center font-medium text-xs text-sidebar-accent-foreground">
              {user.usuario.substring(0, 2).toUpperCase()}
            </div>
            <div className="flex flex-col">
              <span className="text-sm font-medium text-sidebar-foreground">{user.usuario}</span>
              <span className="text-xs text-sidebar-foreground/60">{user.rol}</span>
            </div>
          </div>
          <button 
            onClick={logout}
            className="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-sm text-sidebar-foreground/70 hover:bg-sidebar-accent hover:text-sidebar-accent-foreground transition-colors"
          >
            <LogOut className="h-4 w-4" />
            Cerrar Sesión
          </button>
        </div>
      </aside>
      
      <main className="flex-1 md:pl-64">
        <div className="p-8">
          {children}
        </div>
      </main>

      <AriaChat />
    </div>
  );
}
