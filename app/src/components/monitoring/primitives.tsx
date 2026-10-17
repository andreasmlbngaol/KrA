import type { ReactNode } from 'react';

export type Tone = 'neutral' | 'success' | 'warning' | 'danger' | 'info';

const TONE_CLASS: Record<Tone, string> = {
  neutral: 'bg-neutral-100 text-neutral-600 ring-neutral-200',
  success: 'bg-emerald-50 text-emerald-700 ring-emerald-200',
  warning: 'bg-amber-50 text-amber-700 ring-amber-200',
  danger: 'bg-red-50 text-red-700 ring-red-200',
  info: 'bg-sky-50 text-sky-700 ring-sky-200',
};

const ACCENT_CLASS: Record<Tone, string> = {
  neutral: 'bg-neutral-400',
  success: 'bg-emerald-500',
  warning: 'bg-amber-500',
  danger: 'bg-red-500',
  info: 'bg-sky-500',
};

export function Badge({ tone = 'neutral', children }: { tone?: Tone; children: ReactNode }) {
  return (
    <span
      className={`inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-[11px] font-semibold ring-1 ring-inset ${TONE_CLASS[tone]}`}
    >
      {children}
    </span>
  );
}

const STATUS: Record<string, { label: string; tone: Tone }> = {
  final: { label: 'Selesai', tone: 'success' },
  error: { label: 'Error', tone: 'danger' },
  cancelled: { label: 'Dibatalkan', tone: 'warning' },
  OK: { label: 'OK', tone: 'success' },
  ERROR: { label: 'Error', tone: 'danger' },
  FEASIBLE: { label: 'Layak', tone: 'success' },
  INFEASIBLE: { label: 'Tidak layak', tone: 'danger' },
};

export function StatusBadge({ status }: { status: string }) {
  const s = STATUS[status] ?? { label: status, tone: 'neutral' as Tone };
  return <Badge tone={s.tone}>{s.label}</Badge>;
}

export function Card({
  title,
  description,
  action,
  children,
  className = '',
  bodyClassName = '',
}: {
  title?: ReactNode;
  description?: ReactNode;
  action?: ReactNode;
  children: ReactNode;
  className?: string;
  bodyClassName?: string;
}) {
  return (
    <section
      className={`rounded-2xl border border-neutral-200/90 bg-white shadow-[0_1px_2px_rgba(16,24,40,0.04)] ${className}`}
    >
      {(title || action) && (
        <header className="flex flex-wrap items-start justify-between gap-3 border-b border-neutral-100 px-5 py-4">
          <div className="min-w-0">
            {title && <h2 className="text-[15px] font-semibold text-neutral-900">{title}</h2>}
            {description && <p className="mt-0.5 text-xs text-neutral-500">{description}</p>}
          </div>
          {action}
        </header>
      )}
      <div className={`px-5 py-4 ${bodyClassName}`}>{children}</div>
    </section>
  );
}

export function StatTile({
  label,
  value,
  hint,
  icon,
  tone = 'neutral',
}: {
  label: string;
  value: ReactNode;
  hint?: ReactNode;
  icon?: ReactNode;
  tone?: Tone;
}) {
  return (
    <div className="rounded-2xl border border-neutral-200/90 bg-white p-4 shadow-[0_1px_2px_rgba(16,24,40,0.04)]">
      <div className="flex items-start justify-between gap-2">
        <span className="text-xs font-medium text-neutral-500">{label}</span>
        {icon && (
          <span className={`flex h-8 w-8 shrink-0 items-center justify-center rounded-xl ${TONE_CLASS[tone]} ring-0`}>
            {icon}
          </span>
        )}
      </div>
      <div className="mt-2 text-2xl font-semibold tracking-tight text-neutral-900">{value}</div>
      {hint && <div className="mt-0.5 text-[11px] text-neutral-400">{hint}</div>}
    </div>
  );
}

export function ProgressBar({
  value,
  tone = 'success',
  className = '',
}: {
  value: number;
  tone?: Tone;
  className?: string;
}) {
  const pct = Math.max(0, Math.min(100, value));
  return (
    <div className={`h-1.5 w-full overflow-hidden rounded-full bg-neutral-100 ${className}`}>
      <div className={`h-full rounded-full transition-[width] duration-500 ${ACCENT_CLASS[tone]}`} style={{ width: `${pct}%` }} />
    </div>
  );
}

export function EmptyState({
  title,
  description,
  icon,
}: {
  title: string;
  description?: string;
  icon?: ReactNode;
}) {
  return (
    <div className="flex flex-col items-center justify-center gap-2 px-4 py-12 text-center">
      {icon && <div className="text-neutral-300">{icon}</div>}
      <p className="text-sm font-medium text-neutral-600">{title}</p>
      {description && <p className="max-w-sm text-xs text-neutral-400">{description}</p>}
    </div>
  );
}

export function PageHeader({
  eyebrow,
  title,
  description,
  actions,
}: {
  eyebrow?: string;
  title: string;
  description?: string;
  actions?: ReactNode;
}) {
  return (
    <div className="mb-6 flex flex-wrap items-end justify-between gap-4">
      <div className="min-w-0">
        {eyebrow && (
          <div className="mb-1 text-[11px] font-semibold uppercase tracking-[0.14em] text-neutral-400">
            {eyebrow}
          </div>
        )}
        <h1 className="text-[26px] font-semibold leading-tight tracking-tight text-neutral-900">
          {title}
        </h1>
        {description && <p className="mt-1 max-w-2xl text-sm text-neutral-500">{description}</p>}
      </div>
      {actions && <div className="flex flex-wrap items-center gap-2">{actions}</div>}
    </div>
  );
}

export function ToolbarButton({
  onClick,
  children,
  title,
  disabled,
}: {
  onClick?: () => void;
  children: ReactNode;
  title?: string;
  disabled?: boolean;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      title={title}
      aria-label={title}
      disabled={disabled}
      className="inline-flex h-9 items-center gap-2 rounded-full border border-neutral-200 bg-white px-3.5 text-xs font-semibold text-neutral-700 transition hover:border-neutral-300 hover:bg-neutral-50 disabled:cursor-not-allowed disabled:opacity-50"
    >
      {children}
    </button>
  );
}

export function Select({
  value,
  onChange,
  options,
  label,
}: {
  value: string | number;
  onChange: (value: string) => void;
  options: Array<{ value: string | number; label: string }>;
  label?: string;
}) {
  return (
    <label className="inline-flex items-center gap-2 text-xs font-medium text-neutral-500">
      {label && <span>{label}</span>}
      <select
        value={value}
        onChange={(e) => onChange(e.target.value)}
        className="h-9 rounded-full border border-neutral-200 bg-white px-3 pr-7 text-xs font-semibold text-neutral-700 outline-none transition focus:border-usu"
      >
        {options.map((o) => (
          <option key={o.value} value={o.value}>
            {o.label}
          </option>
        ))}
      </select>
    </label>
  );
}

export function Field({ label, value }: { label: string; value: ReactNode }) {
  return (
    <div>
      <div className="text-[11px] font-semibold uppercase tracking-wide text-neutral-400">{label}</div>
      <div className="mt-0.5 text-sm text-neutral-800">{value}</div>
    </div>
  );
}
