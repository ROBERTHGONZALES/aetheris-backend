import { useEffect } from "react";
import { Route, Switch, Router as WouterRouter, useLocation } from 'wouter';
import { QueryClientProvider } from '@tanstack/react-query';
import { Toaster } from '@/components/ui/toaster';
import { TooltipProvider } from '@/components/ui/tooltip';
import { queryClient, getAuthToken } from '@/lib/api';

import NotFound from '@/pages/not-found';
import Login from '@/pages/login';
import Dashboard from '@/pages/dashboard';
import Transacciones from '@/pages/transacciones';
import Aprobaciones from '@/pages/aprobaciones';
import Presupuesto from '@/pages/presupuesto';
import Conciliacion from '@/pages/conciliacion';
import Sedes from '@/pages/sedes';
import Auditoria from '@/pages/auditoria';
import AriaPage from '@/pages/aria';

function ProtectedRoute({ component: Component, path }: { component: any, path: string }) {
  const [location, setLocation] = useLocation();
  const token = getAuthToken();

  useEffect(() => {
    if (!token && location !== '/login') {
      setLocation('/login');
    }
  }, [token, location, setLocation]);

  if (!token) return null;
  
  return <Component />;
}

function Router() {
  return (
    <Switch>
      <Route path="/login" component={Login} />
      <Route path="/" component={() => {
        const [, setLocation] = useLocation();
        useEffect(() => { setLocation('/dashboard') }, []);
        return null;
      }} />
      <Route path="/dashboard"><ProtectedRoute path="/dashboard" component={Dashboard} /></Route>
      <Route path="/transacciones"><ProtectedRoute path="/transacciones" component={Transacciones} /></Route>
      <Route path="/aprobaciones"><ProtectedRoute path="/aprobaciones" component={Aprobaciones} /></Route>
      <Route path="/presupuesto"><ProtectedRoute path="/presupuesto" component={Presupuesto} /></Route>
      <Route path="/conciliacion"><ProtectedRoute path="/conciliacion" component={Conciliacion} /></Route>
      <Route path="/sedes"><ProtectedRoute path="/sedes" component={Sedes} /></Route>
      <Route path="/auditoria"><ProtectedRoute path="/auditoria" component={Auditoria} /></Route>
      <Route path="/aria"><ProtectedRoute path="/aria" component={AriaPage} /></Route>
      <Route component={NotFound} />
    </Switch>
  );
}

function GlobalAuthGuard() {
  const [, setLocation] = useLocation();

  useEffect(() => {
    const handleUnauthorized = () => {
      setLocation('/login');
    };
    window.addEventListener('auth-unauthorized', handleUnauthorized);
    return () => window.removeEventListener('auth-unauthorized', handleUnauthorized);
  }, [setLocation]);

  return null;
}

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <TooltipProvider>
        <WouterRouter base={import.meta.env.BASE_URL.replace(/\/$/, '')}>
          <GlobalAuthGuard />
          <Router />
        </WouterRouter>
        <Toaster />
      </TooltipProvider>
    </QueryClientProvider>
  );
}

export default App;
