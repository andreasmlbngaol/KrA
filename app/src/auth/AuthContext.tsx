import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import { login as loginRequest, logout as logoutRequest } from '../api/auth';
import { refreshAccessToken, setOnAuthFailure } from '../api/http';
import { clearAccessToken, decodeNim, getAccessToken, setAccessToken } from './tokenStorage';

interface AuthContextValue {
  isAuthenticated: boolean;
  /** True while the initial silent refresh (session restore) is still running. */
  initializing: boolean;
  nim: string | null;
  login: (nim: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [accessToken, setAccessTokenState] = useState<string | null>(() => getAccessToken());
  const [initializing, setInitializing] = useState(true);

  // When a token refresh ultimately fails, drop the session.
  useEffect(() => {
    setOnAuthFailure(() => {
      clearAccessToken();
      setAccessTokenState(null);
    });
    return () => setOnAuthFailure(null);
  }, []);

  // Silent refresh on first load: restore the session from the HttpOnly refresh cookie.
  // The access token lives only in memory, so it is gone after every reload / new tab.
  useEffect(() => {
    let active = true;
    void (async () => {
      try {
        const token = await refreshAccessToken();
        if (active) setAccessTokenState(token);
      } catch {
        // No valid refresh cookie -> stay logged out.
      } finally {
        if (active) setInitializing(false);
      }
    })();
    return () => {
      active = false;
    };
  }, []);

  const login = useCallback(async (nim: string, password: string) => {
    const tokens = await loginRequest(nim, password);
    setAccessToken(tokens.accessToken);
    setAccessTokenState(tokens.accessToken);
  }, []);

  const logout = useCallback(() => {
    void logoutRequest();
    clearAccessToken();
    setAccessTokenState(null);
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      isAuthenticated: !!accessToken,
      initializing,
      nim: accessToken ? decodeNim(accessToken) : null,
      login,
      logout,
    }),
    [accessToken, initializing, login, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within an AuthProvider');
  return ctx;
}
