import { useQuery } from '@tanstack/react-query';
import { apiFetch } from '@/lib/api';

export interface LogAuditoria {
  id?: string;
  usuario: string | null;
  correoUsuario: string | null;
  fechaHora: string;
  accion: string;
  categoria: string | null;
  modulo: string | null;
  entidadAfectada: string | null;
  entidadId: string | null;
  direccionIp: string | null;
  detalle: string | null;
}

export function useGetAuditoria() {
  return useQuery({
    queryKey: ['auditoria'],
    queryFn: () => apiFetch<LogAuditoria[]>('/api/auditoria'),
  });
}
