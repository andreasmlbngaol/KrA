import { useEffect, useState } from 'react';
import type { ReactNode } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { truncate } from '../utils/string';

interface NavItem {
  label: string;
  to?: string;
  icon: ReactNode;
}

const sv = (d: ReactNode): ReactNode => (
  <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth={1.7} strokeLinecap="round" strokeLinejoin="round">{d}</svg>
);

const NAV: NavItem[] = [
  { label: 'Beranda', to: '/dashboard', icon: sv(<><path d="M3 11 12 4l9 7" /><path d="M5 10v10h14V10" /></>) },
  { label: 'Kelola KRS', to: '/kelola-krs', icon: sv(<><path d="M9 5h6l1 2h3v12H5V7h3z" /><path d="M9 12h6" /></>) },
  { label: 'Informasi KHS', to: '/khs', icon: sv(<><path d="M6 3h9l3 3v15H6z" /><path d="M9 12h6M9 16h6" /></>) },
];

interface PortalLayoutProps {
  children: ReactNode;
  periodLabel?: string;
  studentName?: string;
}

export function PortalLayout({ children, periodLabel, studentName }: PortalLayoutProps) {
  const { nim, logout } = useAuth();
  const { pathname } = useLocation();
  const [menuOpen, setMenuOpen] = useState(false);
  const today = new Intl.DateTimeFormat('id-ID', {
    weekday: 'long',
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  }).format(new Date());

  // Tutup drawer setiap kali berpindah halaman.
  useEffect(() => {
    setMenuOpen(false);
  }, [pathname]);

  // Kunci scroll body selama drawer mobile terbuka.
  useEffect(() => {
    if (!menuOpen) return;
    const prev = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.body.style.overflow = prev;
    };
  }, [menuOpen]);

  // Render daftar menu; dipakai sidebar desktop maupun drawer mobile.
  const navItems = (onNavigate?: () => void): ReactNode =>
    NAV.map((item) => {
      const active = item.to ? pathname === item.to : false;
      const cls = `flex items-center gap-3 border-l-[3px] px-5 py-2.5 text-[13.5px] transition ${active ? 'border-usu bg-usu-light font-semibold text-usu-dark' : 'border-transparent text-neutral-600 hover:bg-neutral-50'}`;
      return item.to ? (
        <Link key={item.label} to={item.to} className={cls} onClick={onNavigate}>{item.icon}<span>{item.label}</span></Link>
      ) : (
        <span key={item.label} className={`${cls} cursor-default text-neutral-400`}>{item.icon}<span>{item.label}</span></span>
      );
    });

  return (
    <div className="min-h-screen bg-neutral-100">
      <header className="sticky top-0 z-20 flex h-16 items-center justify-between gap-2 bg-gradient-to-r from-usu to-usu-dark px-3 text-white shadow sm:gap-3 sm:px-5">
        <div className="flex min-w-0 items-center gap-2 sm:gap-3">
          <button
            type="button"
            onClick={() => setMenuOpen(true)}
            aria-label="Buka menu"
            className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg transition hover:bg-white/15 md:hidden"
          >
            <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round"><path d="M4 7h16M4 12h16M4 17h16" /></svg>
          </button>
          <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-white text-usu">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth={1.8} strokeLinecap="round" strokeLinejoin="round"><path d="M22 10 12 5 2 10l10 5 10-5Z" /><path d="M6 12v5c3 1.8 9 1.8 12 0v-5" /></svg>
          </span>
          <span className="truncate text-sm font-bold tracking-wide sm:text-base">SATU MAHASISWA</span>
        </div>
        <div className="hidden text-center text-xs leading-tight lg:block">
          <div>{periodLabel ?? 'Portal Mahasiswa'}</div>
          <div className="font-semibold">{today}</div>
        </div>
        <div className="flex shrink-0 items-center gap-2 sm:gap-3">
          <div className="flex items-center gap-2 rounded-full bg-white py-1 pl-1.5 pr-1.5 text-neutral-700 shadow-sm sm:pr-3">
            <span className="flex h-7 w-7 items-center justify-center rounded-full bg-neutral-300 text-white">
              <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth={1.8}><circle cx="12" cy="8" r="4" /><path d="M4 21c0-4 4-6 8-6s8 2 8 6" /></svg>
            </span>
            <span className="hidden text-xs font-semibold leading-tight sm:block">
              {studentName ? truncate(studentName) : 'Mahasiswa'}
              <span className="block font-normal text-neutral-500">{nim}</span>
            </span>
          </div>
          <button type="button" onClick={logout} className="rounded-lg px-2.5 py-1.5 text-sm font-medium text-white/90 transition hover:bg-white/15 sm:px-3">Keluar</button>
        </div>
      </header>

      {/* Drawer navigasi untuk mobile (sidebar disembunyikan di < md). */}
      {menuOpen && (
        <div className="fixed inset-0 z-40 md:hidden" role="dialog" aria-modal="true">
          <div className="absolute inset-0 bg-black/40" onClick={() => setMenuOpen(false)} />
          <aside className="absolute left-0 top-0 flex h-full w-72 max-w-[82%] flex-col overflow-y-auto bg-white shadow-xl">
            <div className="flex h-16 shrink-0 items-center justify-between border-b border-neutral-200 px-5">
              <span className="font-bold text-usu-dark">Menu</span>
              <button
                type="button"
                onClick={() => setMenuOpen(false)}
                aria-label="Tutup menu"
                className="flex h-8 w-8 items-center justify-center rounded-lg text-neutral-500 transition hover:bg-neutral-100"
              >
                <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round"><path d="M6 6l12 12M18 6 6 18" /></svg>
              </button>
            </div>
            <nav className="flex flex-col py-2">{navItems(() => setMenuOpen(false))}</nav>
          </aside>
        </div>
      )}

      <div className="flex">
        <aside className="sticky top-16 hidden h-[calc(100vh-4rem)] w-60 shrink-0 self-start overflow-y-auto border-r border-neutral-200 bg-white py-2 md:block">
          <nav className="flex flex-col">{navItems()}</nav>
        </aside>

        <main className="min-w-0 flex-1 px-4 pb-28 pt-5 sm:px-5 md:px-8 md:pt-6">
          <div className="mx-auto w-full max-w-none">{children}</div>
        </main>
      </div>
    </div>
  );
}
