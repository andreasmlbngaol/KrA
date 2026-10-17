import { useState } from 'react';
import type { FormEvent } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import axios from 'axios';
import { useAuth } from '../auth/AuthContext';

export function LoginPage() {
  const { isAuthenticated, initializing, login } = useAuth();
  const navigate = useNavigate();
  const [nim, setNim] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  // Avoid flashing the login form while the initial silent refresh is still running.
  if (initializing) {
    return null;
  }
  if (isAuthenticated) {
    return <Navigate to="/dashboard" replace />;
  }

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError(null);
    setLoading(true);
    try {
      await login(nim.trim(), password);
      navigate('/dashboard', { replace: true });
    } catch (err) {
      if (axios.isAxiosError(err)) {
        const data = err.response?.data;
        setError(typeof data === 'string' && data ? data : 'NIM atau password salah.');
      } else {
        setError('Terjadi kesalahan. Coba lagi.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-linear-to-br from-usu to-usu-dark px-4">
      <form
        onSubmit={handleSubmit}
        className="w-full max-w-sm rounded-2xl border-t-4 border-gold bg-white p-7 shadow-xl"
      >
        <div className="mb-6 text-center">
          <h1 className="text-2xl font-extrabold text-usu">KRS Assistant</h1>
          <span className="mx-auto mt-2 block h-1 w-12 rounded-full bg-gold"></span>
          <p className="mt-2 text-sm text-neutral-500">Ilmu Komputer USU</p>
        </div>

        <label className="mb-4 block">
          <span className="mb-1 block text-sm font-medium text-neutral-700">NIM</span>
          <input
            className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:border-usu focus:outline-hidden focus:ring-2 focus:ring-usu/30"
            type="text"
            inputMode="numeric"
            autoComplete="username"
            placeholder="Contoh: 221401001"
            value={nim}
            onChange={(e) => setNim(e.target.value)}
            required
          />
        </label>

        <label className="mb-4 block">
          <span className="mb-1 block text-sm font-medium text-neutral-700">Password</span>
          <input
            className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:border-usu focus:outline-hidden focus:ring-2 focus:ring-usu/30"
            type="password"
            autoComplete="current-password"
            placeholder="Password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
        </label>

        {error && (
          <div className="mb-4 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
            {error}
          </div>
        )}

        <button
          className="w-full rounded-lg bg-usu px-4 py-2.5 font-semibold text-white transition hover:bg-usu-dark disabled:cursor-not-allowed disabled:opacity-60"
          type="submit"
          disabled={loading}
        >
          {loading ? 'Memproses...' : 'Masuk'}
        </button>

        <p className="mt-4 text-center text-xs text-neutral-400">
          Saat ini password sama dengan NIM (mode pengembangan).
        </p>
      </form>
    </div>
  );
}
