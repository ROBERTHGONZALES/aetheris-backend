import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiFetch } from '@/lib/api';

export interface AprobacionPendiente {
  id: string;
  estado: string;
  montoLimite: number;
  fechaSolicitud: string;
  observacion: string;
  transaccion: {
    id: string;
    monto: number;
    descripcion: string;
    sede: { nombre: string };
  };
}

export function useGetAprobacionesPendientes() {
  return useQuery({
    queryKey: ['aprobaciones', 'pendientes'],
    queryFn: () => apiFetch<AprobacionPendiente[]>('/api/aprobaciones/pendientes'),
  });
}

export function useAprobarTransaccion() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, observacion }: { id: string; observacion: string }) =>
      apiFetch(`/api/aprobaciones/${id}/aprobar`, { method: 'PUT', body: JSON.stringify({ observacion }) }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['aprobaciones'] });
      qc.invalidateQueries({ queryKey: ['transacciones'] });
    },
  });
}

export function useRechazarTransaccion() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, observacion }: { id: string; observacion: string }) =>
      apiFetch(`/api/aprobaciones/${id}/rechazar`, { method: 'PUT', body: JSON.stringify({ observacion }) }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['aprobaciones'] });
      qc.invalidateQueries({ queryKey: ['transacciones'] });
    },
  });
}
