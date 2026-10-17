import { useEffect, useState } from 'react';
import type { ReactNode } from 'react';
import { Link, useLocation } from 'react-router-dom';
import {
  IconArrowLeft,
  IconChat,
  IconClose,
  IconEvaluation,
  IconMenu,
  IconOverview,
  IconSolver,
  IconToken,
  IconTool,
} from './icons';

type NavItem = {
  to: string;
  label: string;
  icon: (props: { size?: number }) => ReactNode;
  end?: boolean;
};

const NAV: NavItem[] = [
  { to: '/monitoring', label: 'Ringkasan', icon: IconOverview, end: true },
  { to: '/monitoring/chat', label: 'Riwayat', icon: IconChat },
  { to: '/monitoring/token', label: 'Token', icon: IconToken },
  { to: '/monitoring/tools', label: 'Tool', icon: IconTool },
  { to: '/monitoring/solver', label: 'Solver', icon: IconSolver },
  { to: '/monitoring/evaluation', label: 'Evaluasi', icon: IconEvaluation },
];

function isActive(pathname: string, item: NavItem): boolean {
  return item.end ? pathname === item.to : pathname.startsWith(item.to);
}

function Brand({ compact = false }: { compact?: boolean }) {
  return (
    <div className="flex items-center gap-2.5">
      <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-2xl bg-usu text-white">
        <svg viewBox="0 0 24 24" width="19" height="19" fill="none" stroke="currentColor" strokeWidth={1.8} strokeLinecap="round" strokeLinejoin="round">
          <path d="M22 10 12 5 2 10l10 5 10-5Z" />
          <path d="M6 12v5c3 1.8 9 1.8 12 0v-5" />
        </svg>
      </span>
      {!compact && (
        <div className="leading-tight">
          <div className="text-[13px] font-semibold tracking-wide text-neutral-900">KRS Agent</div>
          <div className="text-[10.5px] uppercase tracking-[0.16em] text-neutral-400">Monitoring</div>
        </div>
      )}
    </div>
  );
}

/**
 * Material Design 3 style navigation rail: a fixed 88px column with icon + label,
 * an active indicator pill, and a modal drawer below the medium breakpoint.
 */
export function MonitoringLayout({ children }: { children: ReactNode }) {
  const { pathname } = useLocation();
  const [drawerOpen, setDrawerOpen] = useState(false);

  useEffect(() => {
    setDrawerOpen(false);
  }, [pathname]);

  useEffect(() => {
    if (!drawerOpen) return;
    const previous = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.body.style.overflow = previous;
    };
  }, [drawerOpen]);

  return (
    <div className="min-h-screen bg-[#f6f8f7] text-neutral-900">
      {/* Top app bar (compact + medium screens) */}
      <header className="sticky top-0 z-30 flex h-14 items-center gap-2 border-b border-neutral-200/80 bg-[#f6f8f7]/90 px-3 backdrop-blur-sm md:hidden">
        <button
          type="button"
          onClick={() => setDrawerOpen(true)}
          aria-label="Buka menu monitoring"
          className="flex h-10 w-10 items-center justify-center rounded-full text-neutral-600 transition hover:bg-neutral-200/60"
        >
          <IconMenu size={22} />
        </button>
        <Brand />
      </header>

      {/* Navigation rail (expanded screens) */}
      <aside className="fixed inset-y-0 left-0 z-30 hidden w-[88px] flex-col items-center border-r border-neutral-200/80 bg-white py-5 md:flex">
        <Link to="/monitoring" aria-label="Ringkasan monitoring" className="mb-6">
          <Brand compact />
        </Link>
        <nav className="flex w-full flex-1 flex-col items-center gap-1">
          {NAV.map((item) => {
            const active = isActive(pathname, item);
            const Icon = item.icon;
            return (
              <Link
                key={item.to}
                to={item.to}
                aria-current={active ? 'page' : undefined}
                className="group flex w-full flex-col items-center gap-1 py-1.5 outline-hidden"
              >
                <span
                  className={`flex h-8 w-14 items-center justify-center rounded-full transition ${
                    active
                      ? 'bg-usu-light text-usu-dark'
                      : 'text-neutral-500 group-hover:bg-neutral-100 group-hover:text-neutral-800'
                  }`}
                >
                  <Icon size={21} />
                </span>
                <span
                  className={`text-[10.5px] font-medium leading-none ${
                    active ? 'text-usu-dark' : 'text-neutral-500 group-hover:text-neutral-700'
                  }`}
                >
                  {item.label}
                </span>
              </Link>
            );
          })}
        </nav>
        <Link
          to="/dashboard"
          title="Kembali ke portal mahasiswa"
          className="mt-4 flex w-14 flex-col items-center gap-1 rounded-full py-2 text-neutral-400 transition hover:bg-neutral-100 hover:text-neutral-700"
        >
          <IconArrowLeft size={18} />
          <span className="text-[10px] font-medium">Portal</span>
        </Link>
      </aside>

      {/* Modal drawer (compact screens) */}
      {drawerOpen && (
        <div className="fixed inset-0 z-40 md:hidden" role="dialog" aria-modal="true">
          <div className="absolute inset-0 bg-neutral-900/40" onClick={() => setDrawerOpen(false)} />
          <aside className="absolute inset-y-0 left-0 flex w-[280px] max-w-[84%] flex-col bg-white shadow-xl">
            <div className="flex h-16 items-center justify-between border-b border-neutral-100 px-4">
              <Brand />
              <button
                type="button"
                onClick={() => setDrawerOpen(false)}
                aria-label="Tutup menu"
                className="flex h-9 w-9 items-center justify-center rounded-full text-neutral-500 transition hover:bg-neutral-100"
              >
                <IconClose size={20} />
              </button>
            </div>
            <nav className="flex-1 overflow-y-auto py-3">
              {NAV.map((item) => {
                const active = isActive(pathname, item);
                const Icon = item.icon;
                return (
                  <Link
                    key={item.to}
                    to={item.to}
                    aria-current={active ? 'page' : undefined}
                    className={`mx-2 flex items-center gap-3 rounded-full px-4 py-2.5 text-sm transition ${
                      active
                        ? 'bg-usu-light font-semibold text-usu-dark'
                        : 'text-neutral-600 hover:bg-neutral-50'
                    }`}
                  >
                    <Icon size={20} />
                    {item.label}
                  </Link>
                );
              })}
            </nav>
            <Link
              to="/dashboard"
              className="m-3 flex items-center gap-3 rounded-full px-4 py-2.5 text-sm text-neutral-500 transition hover:bg-neutral-50"
            >
              <IconArrowLeft size={18} />
              Kembali ke portal
            </Link>
          </aside>
        </div>
      )}

      <main className="md:pl-[88px]">
        <div className="mx-auto w-full max-w-[1400px] px-4 pb-16 pt-6 sm:px-6 md:pt-8 lg:px-8">
          {children}
        </div>
      </main>
    </div>
  );
}
