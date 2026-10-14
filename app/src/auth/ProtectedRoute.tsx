import { Navigate } from 'react-router-dom';
import type { ReactNode } from 'react';
import { useAuth } from './AuthContext';

/** Middleware: blocks access to children unless the user is authenticated. */
export function ProtectedRoute({ children }: { children: ReactNode }) {
  const { isAuthenticated, initializing } = useAuth();

  // Wait for the initial silent refresh before deciding, so reloading a protected page
  // does not bounce the user to /login while the session is still being restored.
  if (initializing) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-neutral-50">
        <span className="h-8 w-8 animate-spin rounded-full border-4 border-usu border-t-transparent" />
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }
  return <>{children}</>;
}
