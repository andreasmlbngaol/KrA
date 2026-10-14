import axios from 'axios';
import type { LoginRequest, TokenResponse } from '../types/api';
import { API_BASE_URL } from './http';

/**
 * Public client for auth endpoints (no Authorization header / interceptors).
 * withCredentials lets login store the HttpOnly refresh cookie and logout clear it.
 */
const publicClient = axios.create({ baseURL: API_BASE_URL, withCredentials: true });

export async function login(nim: string, password: string): Promise<TokenResponse> {
  const body: LoginRequest = { nim, password };
  const { data } = await publicClient.post<TokenResponse>('/auth/login', body);
  return data;
}

/** Clears the server-side refresh cookie. Best-effort: ignore network/HTTP errors. */
export async function logout(): Promise<void> {
  try {
    await publicClient.post('/auth/logout');
  } catch {
    /* abaikan: sesi lokal tetap dibersihkan */
  }
}
