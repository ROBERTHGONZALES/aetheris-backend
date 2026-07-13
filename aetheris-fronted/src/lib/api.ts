import { QueryClient } from '@tanstack/react-query';

// The Aetheris backend (Spring Boot on Railway) is a separate, already-deployed
// service — it is not part of this workspace. CORS on the backend allows
// *.replit.dev / *.repl.co origins, so the browser can call it directly.
export const BACKEND_URL =
  import.meta.env.VITE_BACKEND_URL ??
  'https://aetheris-production-3f46.up.railway.app';

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
});

export const getAuthToken = () => localStorage.getItem('aetheris_token');
export const setAuthToken = (token: string) => localStorage.setItem('aetheris_token', token);
export const removeAuthToken = () => localStorage.removeItem('aetheris_token');

export const getAuthUser = () => {
  const data = localStorage.getItem('aetheris_user');
  return data ? JSON.parse(data) : null;
};
export const setAuthUser = (user: any) => localStorage.setItem('aetheris_user', JSON.stringify(user));
export const removeAuthUser = () => localStorage.removeItem('aetheris_user');

class ApiError extends Error {
  status: number;
  constructor(message: string, status: number) {
    super(message);
    this.status = status;
  }
}

export async function apiFetch<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
  const token = getAuthToken();
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string> | undefined),
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const url = endpoint.startsWith('/api') ? `${BACKEND_URL}${endpoint}` : endpoint;

  const response = await fetch(url, {
    ...options,
    headers,
  });

  if (!response.ok) {
    let errorMessage = 'An error occurred';
    try {
      const errorData = await response.json();
      errorMessage = errorData.error || errorData.message || errorMessage;
    } catch (e) {
      errorMessage = response.statusText;
    }
    
    if (response.status === 401) {
      removeAuthToken();
      removeAuthUser();
      window.dispatchEvent(new Event('auth-unauthorized'));
    }
    
    throw new ApiError(errorMessage, response.status);
  }

  // Handle empty responses
  if (response.status === 204) return {} as T;
  
  const text = await response.text();
  if (!text) return {} as T;
  
  return JSON.parse(text) as T;
}
