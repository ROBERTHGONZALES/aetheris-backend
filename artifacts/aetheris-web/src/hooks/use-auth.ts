import { useMutation, useQuery } from '@tanstack/react-query';
import { apiFetch, setAuthToken, setAuthUser, removeAuthToken, removeAuthUser, getAuthUser } from '@/lib/api';

export interface AuthResponse {
  token: string;
  sesionId: string;
  usuario: string;
  rol: "ADMIN" | "CONTADOR" | "APROBADOR" | "AUDITOR";
}

export function useLogin() {
  return useMutation({
    mutationFn: (data: { correo: string; password: string }) => 
      apiFetch<AuthResponse>('/api/auth/login', {
        method: 'POST',
        body: JSON.stringify(data),
      }),
    onSuccess: (data) => {
      setAuthToken(data.token);
      setAuthUser({ usuario: data.usuario, rol: data.rol, sesionId: data.sesionId });
    },
  });
}

export function useLogout() {
  return () => {
    removeAuthToken();
    removeAuthUser();
    window.location.href = '/login';
  };
}

export function useUser() {
  return getAuthUser() as { usuario: string; rol: "ADMIN" | "CONTADOR" | "APROBADOR" | "AUDITOR"; sesionId: string } | null;
}
