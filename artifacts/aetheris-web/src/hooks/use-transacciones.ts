import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiFetch } from '@/lib/api';

export interface Transaccion {
  id: string;
  tipo: "INGRESO" | "EGRESO";
  monto: number;
  moneda: string;
  fecha: string;
  descripcion: string;
  estadoAprobacion: "PENDIENTE" | "APROBADA" | "RECHAZADA";
  referencia: string;
  sede: { id: string; nombre: string };
  categoria: { id?: string; nombre: string; tipo: string };
}

export function useGetTransacciones(sedeId?: string) {
  return useQuery({
    queryKey: ['transacciones', sedeId],
    queryFn: () => apiFetch<Transaccion[]>(`/api/transacciones${sedeId ? `?sedeId=${sedeId}` : ''}`),
  });
}

export function useGetTransaccionesPendientes() {
  return useQuery({
    queryKey: ['transacciones', 'pendientes'],
    queryFn: () => apiFetch<Transaccion[]>('/api/transacciones/pendientes'),
  });
}

export function useGetTransaccionesPeriodo(inicio: string, fin: string) {
  return useQuery({
    queryKey: ['transacciones', 'periodo', inicio, fin],
    queryFn: () => apiFetch<Transaccion[]>(`/api/transacciones/periodo?inicio=${inicio}&fin=${fin}`),
    enabled: !!inicio && !!fin,
  });
}

export function useCreateTransaccion() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: any) => 
      apiFetch<Transaccion>('/api/transacciones', { method: 'POST', body: JSON.stringify(data) }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['transacciones'] }),
  });
}
