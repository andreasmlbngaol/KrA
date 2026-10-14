import { useEffect, useRef, useState } from 'react';
import { useLocation } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { ChatPage } from '../pages/ChatPage';

/**
 * Orkestrasi FAB + overlay Chat Assistant.
 *
 * Chat di-render sebagai overlay di ATAS halaman yang sedang aktif (bukan route
 * terpisah), sehingga halaman sebelumnya selalu ter-render di belakang. Dengan
 * begitu, saat overlay hijau/putih mengecil ketika ditutup, halaman sebelumnya
 * langsung tampak tanpa perlu menunggu navigasi.
 *
 * Koreografi:
 *   idle -> collapsing (pill menyusut jadi lingkaran)
 *        -> opening   (hijau membesar, lalu chat putih membesar dari titik FAB)
 *        -> open
 *   open -> closing    (chat putih mengecil, lalu hijau mengecil)
 *        -> expanding  (lingkaran kembali memanjang jadi pill)
 *        -> idle
 */
type Phase = 'idle' | 'collapsing' | 'opening' | 'open' | 'closing' | 'expanding';

const COLLAPSE_MS = 300; // pill <-> lingkaran (samakan dengan duration-300)
const REVEAL_MS = 900; // hijau + putih membesar/mengecil (samakan dengan 0.9s di CSS)

export function ChatLauncher() {
  const { isAuthenticated } = useAuth();
  const { pathname } = useLocation();
  const [phase, setPhase] = useState<Phase>('idle');
  const timers = useRef<number[]>([]);

  const clearTimers = () => {
    timers.current.forEach((t) => window.clearTimeout(t));
    timers.current = [];
  };

  useEffect(() => () => clearTimers(), []);

  // Jika sesi berakhir saat chat terbuka, tutup paksa tanpa animasi.
  useEffect(() => {
    if (!isAuthenticated && phase !== 'idle') {
      clearTimers();
      setPhase('idle');
    }
  }, [isAuthenticated, phase]);

  const prefersReduced = () =>
    typeof window !== 'undefined' &&
    window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  const open = () => {
    if (phase !== 'idle') return;
    if (prefersReduced()) {
      setPhase('open');
      return;
    }
    clearTimers();
    setPhase('collapsing');
    timers.current.push(window.setTimeout(() => setPhase('opening'), COLLAPSE_MS));
    timers.current.push(window.setTimeout(() => setPhase('open'), COLLAPSE_MS + REVEAL_MS));
  };

  const close = () => {
    if (phase !== 'open') return;
    if (prefersReduced()) {
      setPhase('idle');
      return;
    }
    clearTimers();
    setPhase('closing');
    timers.current.push(window.setTimeout(() => setPhase('expanding'), REVEAL_MS));
    timers.current.push(window.setTimeout(() => setPhase('idle'), REVEAL_MS + COLLAPSE_MS));
  };

  if (!isAuthenticated) return null;
  // FAB KRS Agent sengaja disembunyikan di semua halaman monitoring
  // (mis. /monitoring dan /monitoring/chat).
  if (pathname === '/monitoring' || pathname.startsWith('/monitoring/')) return null;

  const overlayActive = phase === 'opening' || phase === 'open' || phase === 'closing';
  const collapsed =
    phase === 'collapsing' || phase === 'opening' || phase === 'open' || phase === 'closing';
  const closing = phase === 'closing';

  return (
    <>
      {/* Overlay hijau yang membesar/mengecil dari titik FAB. */}
      {overlayActive && (
        <div
          className={closing ? 'chat-launch-overlay-out' : 'chat-launch-overlay'}
          aria-hidden="true"
        />
      )}

      {/* Panel chat (di atas hijau). Halaman di belakang tetap ter-render. */}
      {overlayActive && <ChatPage closing={closing} onClose={close} />}

      {/* FAB: tombol AI dengan cincin gradien warna-warni berputar + ikon bintang. */}
      <button
        type="button"
        onClick={open}
        aria-label="Buka KRS Agent (AI)"
        title="KRS Agent"
        className={`ai-fab fixed bottom-6 right-6 z-30 inline-flex items-center justify-center rounded-full bg-usu p-4 font-semibold text-white shadow-xl transition-all duration-300 ease-out hover:bg-usu-dark ${
          collapsed ? '' : 'hover:-translate-y-0.5'
        }`}
      >
        <span className="grid h-6 w-6 place-items-center" aria-hidden="true">
          <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round">
            <path d="M9.94 15.5A2 2 0 0 0 8.5 14.06l-6.13-1.58a.5.5 0 0 1 0-.96L8.5 9.94A2 2 0 0 0 9.94 8.5l1.58-6.13a.5.5 0 0 1 .96 0L14.06 8.5A2 2 0 0 0 15.5 9.94l6.13 1.58a.5.5 0 0 1 0 .96L15.5 14.06a2 2 0 0 0-1.44 1.44l-1.58 6.13a.5.5 0 0 1-.96 0z" />
            <path d="M20 3v4" />
            <path d="M22 5h-4" />
            <path d="M4 17v2" />
            <path d="M5 18H3" />
          </svg>
        </span>
        <span
          className={`overflow-hidden whitespace-nowrap text-sm transition-all duration-300 ease-out ${
            collapsed ? 'ml-0 max-w-0 opacity-0' : 'ml-2 max-w-[140px] opacity-100'
          }`}
        >
          KRS Agent
        </span>
      </button>
    </>
  );
}
