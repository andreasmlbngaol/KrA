import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import type { CSSProperties, ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { fetchTokenUsage } from '../api/dashboard';
import type { TokenUsageReport } from '../types/api';
import { formatInt as num, formatDecimal as num1, formatPercent as pct } from '../utils/number';
import { timeOf } from '../utils/datetime';
import { TokenUsageChart, type MetricKey } from '../components/TokenUsageChart';

const RANGE_OPTIONS = [
  { value: '5m', label: '5 menit terakhir' },
  { value: '15m', label: '15 menit terakhir' },
  { value: '30m', label: '30 menit terakhir' },
  { value: '1h', label: '1 jam terakhir' },
  { value: '6h', label: '6 jam terakhir' },
];

// Warna konsisten antar segmen & grafik.
const C = {
  total: '#13a05b',
  input: '#2563eb',
  output: '#9333ea',
  request: '#f5b800',
  tool: '#dc2626',
};

// Helper style (hindari pola JSX double-brace).
const dot = (color: string): CSSProperties => ({ backgroundColor: color });
const bar = (widthPct: number, color: string): CSSProperties => ({
  width: `${widthPct}%`,
  backgroundColor: color,
});

const TOKEN_METRICS: Array<{ key: MetricKey; label: string; color: string }> = [
  { key: 'totalTokens', label: 'Total token', color: C.total },
  { key: 'inputTokens', label: 'Token input', color: C.input },
  { key: 'outputTokens', label: 'Token output', color: C.output },
];

const ACTIVITY_METRICS: Array<{ key: MetricKey; label: string; color: string }> = [
  { key: 'llmRequests', label: 'Jumlah request', color: C.request },
  { key: 'toolCalls', label: 'Tool calls', color: C.tool },
];

const REFRESH_MS = 60_000;

/** Kartu metrik tunggal. */
function StatCard({
  label,
  value,
  hint,
  accent,
}: {
  label: string;
  value: string;
  hint?: string;
  accent?: string;
}) {
  return (
    <div className="rounded-2xl border border-neutral-200 bg-white p-4 shadow-sm">
      <div className="flex items-center gap-2">
        {accent && <span className="inline-block h-2.5 w-2.5 rounded-full" style={dot(accent)} />}
        <div className="text-sm text-neutral-500">{label}</div>
      </div>
      <div className="mt-1 text-2xl font-bold text-neutral-900">{value}</div>
      {hint && <div className="mt-0.5 text-xs text-neutral-400">{hint}</div>}
    </div>
  );
}

function ChevronIcon({ open }: { open: boolean }) {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={2}
      strokeLinecap="round"
      strokeLinejoin="round"
      className={`h-4 w-4 shrink-0 text-neutral-400 transition-transform ${open ? '' : '-rotate-90'}`}
      aria-hidden="true"
    >
      <path d="M6 9l6 6 6-6" />
    </svg>
  );
}

/** Pembungkus segmen yang bisa disembunyikan (collapsible). */
function Segment({
  title,
  desc,
  children,
  defaultOpen = true,
}: {
  title: string;
  desc?: string;
  children: ReactNode;
  defaultOpen?: boolean;
}) {
  const [open, setOpen] = useState(defaultOpen);
  return (
    <section className="mb-8">
      <button
        type="button"
        onClick={() => setOpen((o) => !o)}
        aria-expanded={open}
        className="group mb-3 flex w-full items-center gap-2 rounded-lg py-1 text-left transition hover:bg-neutral-100/60"
      >
        <ChevronIcon open={open} />
        <div className="flex-1">
          <h2 className="text-lg font-bold text-neutral-900">{title}</h2>
          {desc && <p className="text-sm text-neutral-500">{desc}</p>}
        </div>
        <span className="mr-1 text-xs font-medium text-neutral-400 group-hover:text-neutral-600">
          {open ? 'Sembunyikan' : 'Tampilkan'}
        </span>
      </button>
      {open && children}
    </section>
  );
}

function ReloadIcon({ spinning }: { spinning?: boolean }) {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={2}
      strokeLinecap="round"
      strokeLinejoin="round"
      className={`h-5 w-5 ${spinning ? 'animate-spin' : ''}`}
      aria-hidden="true"
    >
      <path d="M21 12a9 9 0 1 1-2.64-6.36" />
      <path d="M21 3v6h-6" />
    </svg>
  );
}

export function TokenUsagePage() {
  const [range, setRange] = useState('15m');
  const [report, setReport] = useState<TokenUsageReport | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [lastUpdated, setLastUpdated] = useState<Date | null>(null);

  const load = useCallback(
    async (showSpinner: boolean) => {
      if (showSpinner) setLoading(true);
      try {
        const data = await fetchTokenUsage(range);
        setReport(data);
        setError(null);
        setLastUpdated(new Date());
      } catch {
        setError('Gagal memuat data. Pastikan server berjalan.');
      } finally {
        setLoading(false);
      }
    },
    [range],
  );

  useEffect(() => {
    void load(true);
  }, [load]);

  const loadRef = useRef(load);
  loadRef.current = load;
  useEffect(() => {
    const id = window.setInterval(() => void loadRef.current(false), REFRESH_MS);
    return () => window.clearInterval(id);
  }, []);

  const rangeLabel = RANGE_OPTIONS.find((o) => o.value === range)?.label ?? range;
  const series = report?.series ?? [];

  // --- Metrik turunan (dihitung di klien dari report) ---
  const derived = useMemo(() => {
    if (!report) return null;
    const totalTokens = report.totalTokens;
    const inputPct = totalTokens > 0 ? (report.totalInputTokens / totalTokens) * 100 : 0;
    const outputPct = totalTokens > 0 ? (report.totalOutputTokens / totalTokens) * 100 : 0;

    const totalMinutes = report.series.length;
    const activeMinutes = report.series.filter(
      (p) => p.totalTokens > 0 || p.llmRequests > 0 || p.toolCalls > 0,
    ).length;

    const avgPerActiveMin = activeMinutes > 0 ? totalTokens / activeMinutes : 0;
    const reqPerActiveMin = activeMinutes > 0 ? report.totalLlmRequests / activeMinutes : 0;
    const toolsPerReq =
      report.totalLlmRequests > 0 ? report.totalToolCalls / report.totalLlmRequests : 0;

    let peakValue = 0;
    let peakMinute = '';
    for (const p of report.series) {
      if (p.totalTokens > peakValue) {
        peakValue = p.totalTokens;
        peakMinute = p.minute;
      }
    }

    return {
      inputPct,
      outputPct,
      totalMinutes,
      activeMinutes,
      avgPerActiveMin,
      reqPerActiveMin,
      toolsPerReq,
      peakValue,
      peakMinute,
      distinctTools: report.toolBreakdown.length,
    };
  }, [report]);

  return (
    <div className="flex min-h-screen flex-col bg-neutral-50">
      <header className="sticky top-0 z-10 flex items-center justify-between gap-3 bg-gradient-to-r from-usu to-usu-dark px-5 py-3.5 text-white shadow">
        <div className="flex items-center gap-3">
          <span className="font-bold">KRS Agent</span>
          <span className="hidden text-sm text-white/80 sm:inline">Monitoring Token</span>
        </div>
        <div className="flex items-center gap-1">
          <Link
            to="/monitoring/chat"
            className="rounded-lg px-3 py-1.5 text-sm font-medium text-white/90 transition hover:bg-white/15"
          >
            Riwayat Chat
          </Link>
          <Link
            to="/dashboard"
            className="rounded-lg px-3 py-1.5 text-sm font-medium text-white/90 transition hover:bg-white/15"
          >
            &larr; Dashboard
          </Link>
        </div>
      </header>

      <main className="mx-auto w-full max-w-6xl px-4 py-8 sm:px-5">
        <div className="mb-6 flex flex-wrap items-end justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold text-neutral-900">Pemakaian Token Agent</h1>
            <p className="text-sm text-neutral-500">
              Metrik per menit dari pemakaian LLM, disimpan persisten di PostgreSQL.
            </p>
          </div>
          <div className="flex flex-wrap items-center gap-3">
            <label className="flex items-center gap-2 text-sm text-neutral-600">
              <span>Rentang</span>
              <select
                value={range}
                onChange={(e) => setRange(e.target.value)}
                className="rounded-lg border border-neutral-300 bg-white px-3 py-1.5 text-sm font-medium text-neutral-800 outline-none focus:border-usu"
              >
                {RANGE_OPTIONS.map((o) => (
                  <option key={o.value} value={o.value}>{o.label}</option>
                ))}
              </select>
            </label>
            <button
              type="button"
              onClick={() => void load(true)}
              title="Muat ulang"
              aria-label="Muat ulang"
              className="flex h-9 w-9 items-center justify-center rounded-lg bg-usu text-white transition hover:bg-usu-dark"
            >
              <ReloadIcon spinning={loading} />
            </button>
          </div>
        </div>

        {/* Baris meta: jendela waktu + status + terakhir diperbarui */}
        {report && (
          <div className="mb-6 flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-neutral-500">
            <span>
              Jendela: <span className="font-medium text-neutral-700">{timeOf(report.fromMinute)} &ndash; {timeOf(report.toMinute)}</span>
            </span>
            {derived && (
              <span>
                Menit aktif: <span className="font-medium text-neutral-700">{num(derived.activeMinutes)}/{num(derived.totalMinutes)}</span>
              </span>
            )}
            <span className="flex items-center gap-1.5">
              <span className={`inline-block h-2 w-2 rounded-full ${report.enabled ? 'bg-usu' : 'bg-neutral-300'}`} />
              {report.enabled ? 'Penyimpanan aktif' : 'Penyimpanan nonaktif'}
            </span>
            {lastUpdated && <span>Diperbarui {lastUpdated.toLocaleTimeString('id-ID')}</span>}
          </div>
        )}

        {report && !report.enabled && (
          <div className="mb-6 rounded-xl border border-gold-dark/30 bg-gold-light px-4 py-3 text-sm text-neutral-700">
            &#9888; Penyimpanan metrik nonaktif, jadi data masih kosong. Pastikan konfigurasi
            <code className="mx-1 rounded bg-white px-1">enabled</code> sudah benar lalu restart server.
          </div>
        )}

        {error && (
          <div className="mb-6 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
            {error}
          </div>
        )}

        {loading && !report ? (
          <div className="flex h-72 items-center justify-center rounded-2xl border border-neutral-200 bg-white text-sm text-neutral-400 shadow-sm">
            Memuat&hellip;
          </div>
        ) : (
          <>
            {/* ============ SEGMEN 1: TOKEN ============ */}
            <Segment title="Token" desc="Total token beserta rincian input dan output.">
              <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
                <StatCard
                  label="Total token"
                  value={report ? num(report.totalTokens) : '-'}
                  hint={derived ? `${num(derived.avgPerActiveMin)} token / menit aktif` : undefined}
                  accent={C.total}
                />
                <StatCard
                  label="Token input"
                  value={report ? num(report.totalInputTokens) : '-'}
                  hint={derived ? `${pct(derived.inputPct)} dari total` : undefined}
                  accent={C.input}
                />
                <StatCard
                  label="Token output"
                  value={report ? num(report.totalOutputTokens) : '-'}
                  hint={derived ? `${pct(derived.outputPct)} dari total` : undefined}
                  accent={C.output}
                />
              </div>

              {/* Proporsi input vs output */}
              {derived && report && report.totalTokens > 0 && (
                <div className="mt-4 rounded-2xl border border-neutral-200 bg-white p-4 shadow-sm">
                  <div className="mb-2 flex items-center justify-between text-sm">
                    <span className="font-medium text-neutral-700">Distribusi input vs output</span>
                    <span className="text-neutral-500">
                      {pct(derived.inputPct)} &middot; {pct(derived.outputPct)}
                    </span>
                  </div>
                  <div className="flex h-3 w-full overflow-hidden rounded-full bg-neutral-100">
                    <div style={bar(derived.inputPct, C.input)} />
                    <div style={bar(derived.outputPct, C.output)} />
                  </div>
                  <div className="mt-2 flex items-center gap-4 text-xs text-neutral-500">
                    <span className="flex items-center gap-1.5">
                      <span className="inline-block h-2.5 w-2.5 rounded-full" style={dot(C.input)} /> Input
                    </span>
                    <span className="flex items-center gap-1.5">
                      <span className="inline-block h-2.5 w-2.5 rounded-full" style={dot(C.output)} /> Output
                    </span>
                  </div>
                </div>
              )}
            </Segment>

            {/* ============ SEGMEN 2: PERMINTAAN LLM ============ */}
            <Segment title="Permintaan LLM" desc="Aktivitas pemanggilan model bahasa.">
              <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
                <StatCard
                  label="Jumlah request"
                  value={report ? num(report.totalLlmRequests) : '-'}
                  accent={C.request}
                />
                <StatCard
                  label="Rata-rata token / request"
                  value={report ? num(report.avgTokensPerRequest) : '-'}
                />
                <StatCard
                  label="Request / menit aktif"
                  value={derived ? num1(derived.reqPerActiveMin) : '-'}
                />
                <StatCard
                  label="Puncak token / menit"
                  value={derived ? num(derived.peakValue) : '-'}
                  hint={derived && derived.peakMinute ? `pukul ${timeOf(derived.peakMinute)}` : undefined}
                />
              </div>
            </Segment>

            {/* ============ SEGMEN 3: PEMANGGILAN TOOL ============ */}
            <Segment title="Pemanggilan Tool" desc="Seberapa sering agent memakai tool internal.">
              <div className="grid grid-cols-2 gap-4 lg:grid-cols-3">
                <StatCard
                  label="Total tool calls"
                  value={report ? num(report.totalToolCalls) : '-'}
                  accent={C.tool}
                />
                <StatCard
                  label="Jenis tool dipakai"
                  value={derived ? num(derived.distinctTools) : '-'}
                />
                <StatCard
                  label="Tool / request"
                  value={derived ? num1(derived.toolsPerReq) : '-'}
                />
              </div>

              {/* Rincian per tool \u2014 versi ringkas/kecil */}
              {report && report.toolBreakdown.length > 0 && (
                <div className="mt-3 rounded-xl border border-neutral-200 bg-white p-4 shadow-sm">
                  <h3 className="mb-2 text-sm font-semibold text-neutral-700">
                    Rincian per tool &middot; {rangeLabel}
                  </h3>
                  <ul className="divide-y divide-neutral-100">
                    {report.toolBreakdown.map((t) => {
                      const share =
                        report.totalToolCalls > 0 ? (t.count / report.totalToolCalls) * 100 : 0;
                      return (
                        <li key={t.name} className="flex items-center justify-between py-1.5 text-sm">
                          <span className="font-mono text-xs text-neutral-600">{t.name}</span>
                          <span className="text-neutral-500">
                            <span className="font-semibold text-neutral-800">{num(t.count)}&times;</span>
                            <span className="ml-2 text-xs">{pct(share)}</span>
                          </span>
                        </li>
                      );
                    })}
                  </ul>
                </div>
              )}
            </Segment>

            {/* ============ SEGMEN 4: GRAFIK (diperbesar) ============ */}
            <Segment title={`Grafik per menit \u00b7 ${rangeLabel}`} desc="Arahkan kursor ke grafik untuk melihat detail tiap menit.">
              <h3 className="mb-2 text-sm font-semibold uppercase tracking-wide text-neutral-400">Token</h3>
              <div className="mb-6 grid grid-cols-1 gap-5 xl:grid-cols-2">
                {TOKEN_METRICS.map((mtr) => (
                  <div key={mtr.key} className="rounded-2xl border border-neutral-200 bg-white p-5 shadow-sm">
                    <div className="mb-2 flex items-center gap-2">
                      <span className="inline-block h-3 w-3 rounded-full" style={dot(mtr.color)} />
                      <h4 className="font-semibold text-neutral-800">{mtr.label}</h4>
                    </div>
                    <TokenUsageChart points={series} metric={mtr.key} color={mtr.color} heightClass="h-80" />
                  </div>
                ))}
              </div>

              <h3 className="mb-2 text-sm font-semibold uppercase tracking-wide text-neutral-400">Aktivitas</h3>
              <div className="grid grid-cols-1 gap-5 xl:grid-cols-2">
                {ACTIVITY_METRICS.map((mtr) => (
                  <div key={mtr.key} className="rounded-2xl border border-neutral-200 bg-white p-5 shadow-sm">
                    <div className="mb-2 flex items-center gap-2">
                      <span className="inline-block h-3 w-3 rounded-full" style={dot(mtr.color)} />
                      <h4 className="font-semibold text-neutral-800">{mtr.label}</h4>
                    </div>
                    <TokenUsageChart points={series} metric={mtr.key} color={mtr.color} heightClass="h-80" />
                  </div>
                ))}
              </div>
            </Segment>
          </>
        )}
      </main>
    </div>
  );
}
