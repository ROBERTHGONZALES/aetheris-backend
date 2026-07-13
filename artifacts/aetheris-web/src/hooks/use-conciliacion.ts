import { useMutation } from '@tanstack/react-query';
import { apiFetch } from '@/lib/api';

export interface MovimientoBancario {
  fecha: string;
  monto: number;
  referencia: string;
  descripcionBanco: string;
}

export function useIniciarConciliacion() {
  return useMutation({
    mutationFn: ({ cuentaId, periodo }: { cuentaId: string; periodo: string }) =>
      apiFetch<{ id: string; estado: string }>(`/api/conciliacion?cuentaId=${cuentaId}&periodo=${periodo}`, { method: 'POST' }),
  });
}

export function useImportarMovimientos() {
  return useMutation({
    mutationFn: ({ id, movimientos }: { id: string; movimientos: MovimientoBancario[] }) =>
      apiFetch(`/api/conciliacion/${id}/movimientos`, { method: 'POST', body: JSON.stringify(movimientos) }),
  });
}

export function useCruzarConciliacion() {
  return useMutation({
    mutationFn: (id: string) =>
      apiFetch(`/api/conciliacion/${id}/cruce`, { method: 'POST' }),
  });
}
