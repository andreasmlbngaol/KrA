import { useEffect } from 'react';
import type { ReactNode } from 'react';

export type ConfirmDialogProps = {
  open: boolean;
  title: string;
  description?: ReactNode;
  confirmLabel?: string;
  cancelLabel?: string;
  /** 'danger' memberi aksen merah untuk aksi destruktif (mis. hapus). */
  tone?: 'default' | 'danger';
  /** Saat true, tombol dinonaktifkan & label konfirmasi berganti jadi "Memproses...". */
  busy?: boolean;
  onConfirm: () => void;
  onCancel: () => void;
};

/**
 * Dialog konfirmasi modal yang reusable, menggantikan window.confirm bawaan browser.
 * Mendukung penutupan via tombol Batal, klik backdrop, atau tombol Escape.
 */
export function ConfirmDialog({
  open,
  title,
  description,
  confirmLabel = 'Konfirmasi',
  cancelLabel = 'Batal',
  tone = 'default',
  busy = false,
  onConfirm,
  onCancel,
}: ConfirmDialogProps) {
  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && !busy) onCancel();
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [open, busy, onCancel]);

  if (!open) return null;

  const isDanger = tone === 'danger';
  const accentBorder = isDanger ? 'border-red-500' : 'border-usu';
  const iconWrap = isDanger ? 'bg-red-50 text-red-600' : 'bg-usu-light text-usu-dark';
  const confirmBtn = isDanger ? 'bg-red-600 hover:bg-red-700' : 'bg-usu hover:bg-usu-dark';

  return (
    <div
      className="fixed inset-0 z-60 flex items-center justify-center bg-black/40 px-4"
      role="dialog"
      aria-modal="true"
      onClick={() => {
        if (!busy) onCancel();
      }}
    >
      <div
        className={`w-full max-w-sm rounded-2xl border-t-4 ${accentBorder} bg-white p-6 shadow-xl`}
        onClick={(e) => e.stopPropagation()}
      >
        <div className={`mx-auto mb-3 flex h-12 w-12 items-center justify-center rounded-full ${iconWrap}`}>
          <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" strokeWidth={1.8} strokeLinecap="round" strokeLinejoin="round">
            <path d="M12 9v4M12 17h.01M10.3 3.9 1.8 18a2 2 0 0 0 1.7 3h17a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0Z" />
          </svg>
        </div>
        <h3 className="text-center text-lg font-bold text-neutral-900">{title}</h3>
        {description && <p className="mt-2 text-center text-sm text-neutral-500">{description}</p>}
        <div className="mt-6 flex gap-3">
          <button
            type="button"
            onClick={onCancel}
            disabled={busy}
            className="flex-1 rounded-lg border border-neutral-300 px-4 py-2.5 font-semibold text-neutral-700 transition hover:bg-neutral-50 disabled:opacity-50"
          >
            {cancelLabel}
          </button>
          <button
            type="button"
            onClick={onConfirm}
            disabled={busy}
            className={`flex-1 rounded-lg px-4 py-2.5 font-semibold text-white shadow-xs transition disabled:opacity-50 ${confirmBtn}`}
          >
            {busy ? 'Memproses...' : confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
}
