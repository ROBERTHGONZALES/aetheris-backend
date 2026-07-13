import path from 'path';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';
import { defineConfig } from 'vite';

// La URL del backend de Railway (o la de tu proxy local si lo usas)
const BACKEND_URL =
  process.env.VITE_BACKEND_URL ??
  'https://aetheris-production-3f46.up.railway.app';

export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src'),
    },
    dedupe: ['react', 'react-dom'],
  },
  server: {
    port: 5173,
    host: '0.0.0.0',
    // Proxy de desarrollo: el navegador llama a /api/... → Vite reenvía a Railway
    // Como es server-to-server, Spring Security no aplica CORS y no hay 403.
    proxy: {
      '/api': {
        target: BACKEND_URL,
        changeOrigin: true,
        secure: true,
      },
    },
  },
  preview: {
    port: 4173,
    host: '0.0.0.0',
  },
  build: {
    outDir: 'dist',
    emptyOutDir: true,
  },
});
