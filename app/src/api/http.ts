import axios from 'axios';
import type { AxiosError, InternalAxiosRequestConfig } from 'axios';
import type { TokenResponse } from '../types/api';
import { clearAccessToken, getAccessToken, setAccessToken } from '../auth/tokenStorage';

export const API_BASE_URL: string =
  (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? '/api';

/** Main client: carries the in-memory access token and auto-refreshes on 401. */
export const http = axios.create({ baseURL: API_BASE_URL });

/**
 * Bare client used ONLY for the refresh call (no interceptors -> no recursion).
 * withCredentials lets the browser send the HttpOnly refresh cookie to /auth/refresh.
 */
const refreshClient = axios.create({ baseURL: API_BASE_URL, withCredentials: true });

type RetriableConfig = InternalAxiosRequestConfig & { _retry?: boolean };

// --- Attach the in-memory access token to every request ---
http.interceptors.request.use((config) => {
  const token = getAccessToken();
  if (token) {
    config.headers.set('Authorization', `Bearer ${token}`);
  }
  return config;
});

// Lets AuthContext react (e.g. redirect to /login) when the session is dead.
let onAuthFailure: (() => void) | null = null;
export function setOnAuthFailure(callback: (() => void) | null): void {
  onAuthFailure = callback;
}

// --- Single-flight token refresh ---
// Dipakai BERSAMA oleh interceptor axios DAN streaming chat (fetch mentah di
// chat.ts). Semua pemanggil yang berbarengan menunggu satu proses refresh yang
// sama, sehingga tidak ada dua panggilan /auth/refresh yang berlomba.
let refreshPromise: Promise<string> | null = null;

/**
 * Menyegarkan access token. Refresh token TIDAK dikirim dari JavaScript: ia berada di
 * cookie HttpOnly yang otomatis dikirim browser ke /auth/refresh. Mengembalikan access token
 * baru, atau melempar bila refresh gagal (mis. cookie tidak ada / sudah kadaluarsa). Bila
 * gagal, access token dibersihkan dan onAuthFailure dipanggil (redirect ke login).
 */
export async function refreshAccessToken(): Promise<string> {
  // Sudah ada refresh berjalan: ikut menunggu hasil yang sama.
  if (refreshPromise) return refreshPromise;

  refreshPromise = (async () => {
    try {
      const { data } = await refreshClient.post<TokenResponse>('/auth/refresh');
      setAccessToken(data.accessToken);
      return data.accessToken;
    } catch (refreshError) {
      clearAccessToken();
      onAuthFailure?.();
      throw refreshError;
    } finally {
      refreshPromise = null;
    }
  })();

  return refreshPromise;
}

http.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const original = error.config as RetriableConfig | undefined;
    const status = error.response?.status;

    if (!original || status !== 401 || original._retry) {
      return Promise.reject(error);
    }

    original._retry = true;
    try {
      const newToken = await refreshAccessToken();
      original.headers.set('Authorization', `Bearer ${newToken}`);
      return http(original);
    } catch (refreshError) {
      return Promise.reject(refreshError);
    }
  },
);
