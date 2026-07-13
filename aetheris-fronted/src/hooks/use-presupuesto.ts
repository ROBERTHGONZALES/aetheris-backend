import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiFetch } from '@/lib/api';

export interface Presupuesto {
  id: string;
  periodo: string;
  montoPresupuestado: number;
  montoEjecutado: number;
  porcentajeEjecucion: number;
  categoria: { nombre: string; tipo: string };
}

// GET /api/presupuesto requires sedeId -- the backend has no optional/omitted-sede listing.
export function useGetPresupuestos(sedeId: string | undefined) {
  return useQuery({
    queryKey: ['presupuestos', sedeId],
    queryFn: () => apiFetch<Presupuesto[]>(`/api/presupuesto?sedeId=${encodeURIComponent(sedeId!)}`),
    enabled: !!sedeId,
  });
}

// GET /api/presupuesto/alerta -- partidas that crossed the alert threshold, across all sedes.
export function useGetPresupuestosEnAlerta() {
  return useQuery({
    queryKey: ['presupuestos', 'alerta'],
    queryFn: () => apiFetch<Presupuesto[]>('/api/presupuesto/alerta'),
  });
}

export function useCreatePresupuesto() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: any) =>
      apiFetch<Presupuesto>('/api/presupuesto', { method: 'POST', body: JSON.stringify(data) }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['presupuestos'] }),
  });
}
