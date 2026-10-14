/**
 * In-memory access-token store.
 *
 * The access token is kept ONLY in memory (never localStorage), so an XSS payload cannot
 * read it and it disappears on reload. The long-lived refresh token lives in an HttpOnly
 * cookie that JavaScript cannot read at all; the browser sends it automatically to
 * /api/auth/refresh. On a fresh load the access token is gone, so AuthContext performs a
 * silent refresh to restore the session from the cookie.
 */
let accessToken: string | null = null;

export function getAccessToken(): string | null {
  return accessToken;
}

export function setAccessToken(token: string | null): void {
  accessToken = token;
}

export function clearAccessToken(): void {
  accessToken = null;
}

/** Decode the `nim` claim from a JWT access token (no signature verification). */
export function decodeNim(token: string): string | null {
  try {
    const payload = token.split('.')[1];
    if (!payload) return null;
    const normalized = payload.replace(/-/g, '+').replace(/_/g, '/');
    const json = JSON.parse(atob(normalized)) as { nim?: string };
    return json.nim ?? null;
  } catch {
    return null;
  }
}
