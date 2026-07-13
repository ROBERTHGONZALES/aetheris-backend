import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiFetch } from '@/lib/api';

export interface Sede {
  id: string;
  nombre: string;
  codigo: string;
  pais: string;
  moneda: string;
  montoLimiteAprobacion: number;
  estado: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export function useGetSedes() {
  return useQuery({
    queryKey: ['sedes'],
    queryFn: () => apiFetch<Sede[]>('/api/sedes'),
  });
}

export function useGetSede(id: string) {
  return useQuery({
    queryKey: ['sedes', id],
    queryFn: () => apiFetch<Sede>(`/api/sedes/${id}`),
    enabled: !!id,
  });
}

export function useCreateSede() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: Partial<Sede>) => apiFetch<Sede>('/api/sedes', { method: 'POST', body: JSON.stringify(data) }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['sedes'] }),
  });
}

export function useUpdateSede() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: Partial<Sede> }) => 
      apiFetch<Sede>(`/api/sedes/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['sedes'] }),
  });
}

export function useUpdateLimiteSede() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, monto }: { id: string; monto: number }) => 
      apiFetch<Sede>(`/api/sedes/${id}/limite?monto=${monto}`, { method: 'PUT' }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['sedes'] }),
  });
}
