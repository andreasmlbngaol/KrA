import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { fetchChatHistory } from '../api/chatHistory';
import type { ChatHistoryEntry, ChatHistoryResponse } from '../types/api';
import { Markdown } from '../components/Markdown';
import { formatMinuteLabel, timeOf } from '../utils/datetime';
import { formatInt as num } from '../utils/number';

const LIMIT_OPTIONS = [
  { value: 50, label: '50 terbaru' },
  { value: 100, label: '100 terbaru' },
  { value: 200, label: '200 terbaru' },
  { value: 500, label: '500 terbaru' },
];

const REFRESH_MS = 30_000;

const STATUS_STYLE: Record<string, { label: string; cls: string }> = {
  final: { label: 'Selesai', cls: 'bg-emerald-100 text-emerald-700' },
  error: { label: 'Error', cls: 'bg-red-100 text-red-700' },
  cancelled: { label: 'Dibatalkan', cls: 'bg-amber-100 text-amber-700' },
};

function StatusBadge({ status }: { status: string }) {
  const s = STATUS_STYLE[status] ?? { label: status, cls: 'bg-neutral-100 text-neutral-600' };
  return <span className={`rounded-full px-2 py-0.5 text-xs font-semibold ${s.cls}`}>{s.label}</span>;
}

/** Durasi ms -> teks ringkas (mis. "850 ms", "2,4 dtk"). */
function fmtDuration(ms: number): string {
  if (ms <= 0) return '-';
  if (ms < 1000) return `${ms} ms`;
  return `${(ms / 1000).toFixed(1)} dtk`;
}

/**
 * Satu sesi percakapan: semua turn dengan session id yang sama, dikumpulkan jadi
 * satu kelompok. Ini mencegah turn dari dua user yang chat bersamaan tercampur
 * dalam satu daftar acak.
 */
type ChatSession = {
  key: string;
  sessionId: string;
  nim: string;
  startTime: string; // turn paling awal di sesi ini (ISO UTC)
  lastTime: string; // turn paling akhir di sesi ini (ISO UTC)
  turns: ChatHistoryEntry[]; // urut kronologis (lama -> baru)
  errorCount: number;
};

/**
 * Kelompokkan turn per session id, urutkan turn di dalam sesi secara kronologis,
 * lalu urutkan sesi berdasarkan aktivitas terakhir (terbaru di atas).
 * Timestamp berupa ISO-8601 UTC (berakhiran "Z") sehingga perbandingan leksikal
 * sudah setara dengan perbandingan waktu.
 */
function groupSessions(entries: ChatHistoryEntry[]): ChatSession[] {
  const map = new Map<string, ChatHistoryEntry[]>();
  for (const e of entries) {
    // Turn tanpa session id diperlakukan sebagai sesi tersendiri agar tidak
    // saling menempel.
    const key = e.sessionId || `__no-session-${e.id}`;
    const arr = map.get(key);
    if (arr) arr.push(e);
    else map.set(key, [e]);
  }

  const sessions: ChatSession[] = [];
  for (const [key, arr] of map) {
    const sorted = [...arr].sort((a, b) => a.timestamp.localeCompare(b.timestamp));
    const first = sorted[0];
    const last = sorted[sorted.length - 1];
    sessions.push({
      key,
      sessionId: first.sessionId,
      nim: first.nim,
      startTime: first.timestamp,
      lastTime: last.timestamp,
      turns: sorted,
      errorCount: sorted.filter((t) => t.status === 'error').length,
    });
  }

  sessions.sort((a, b) => b.lastTime.localeCompare(a.lastTime));
  return sessions;
}

/** Detail satu turn (selalu tampil saat kartu sesi dibuka), urut sesuai percakapan. */
function TurnDetail({ entry, index }: { entry: ChatHistoryEntry; index: number }) {
  return (
    <div className="rounded-xl border border-neutral-200 bg-white p-3 shadow-sm">
      <div className="flex flex-wrap items-center gap-x-2.5 gap-y-1 text-xs text-neutral-500">
        <span className="font-semibold text-neutral-700">#{index + 1}</span>
        <StatusBadge status={entry.status} />
        <span>&middot;</span>
        <span>{timeOf(entry.timestamp)}</span>
        <span>&middot;</span>
        <span>{entry.streamed ? 'Streaming' : 'Non-stream'}</span>
        <span>&middot;</span>
        <span>{fmtDuration(entry.durationMs)}</span>
      </div>

      <div className="mt-2 space-y-2.5">
        <div>
          <div className="mb-1 text-[11px] font-semibold uppercase tracking-wide text-neutral-400">Permintaan</div>
          <pre className="whitespace-pre-wrap break-words rounded-lg bg-neutral-50 p-3 text-sm text-neutral-800">{entry.request || '-'}</pre>
        </div>

        {entry.steps.length > 0 && (
          <div>
            <div className="mb-1 text-[11px] font-semibold uppercase tracking-wide text-neutral-400">
              Langkah ({entry.steps.length})
            </div>
            <div className="flex flex-wrap gap-1.5">
              {entry.steps.map((s, i) => (
                <span
                  key={`${entry.id}-step-${i}`}
                  className="rounded-full bg-usu-light px-2 py-0.5 text-xs text-usu-dark"
                >
                  {s}
                </span>
              ))}
            </div>
          </div>
        )}

        <div>
          <div className="mb-1 text-[11px] font-semibold uppercase tracking-wide text-neutral-400">Respons</div>
          {entry.response ? (
            <div className="rounded-lg border border-neutral-100 bg-white p-3">
              <Markdown content={entry.response} />
            </div>
          ) : (
            <div className="rounded-lg bg-neutral-50 p-3 text-sm italic text-neutral-400">
              Tidak ada respons (percakapan dibatalkan sebelum selesai).
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

/** Kartu ringkas per sesi: NIM + waktu mulai + jumlah pesan. Klik untuk buka detail. */
function SessionCard({ session }: { session: ChatSession }) {
  const [open, setOpen] = useState(false);
  return (
    <article className="overflow-hidden rounded-2xl border border-neutral-200 bg-white shadow-sm">
      <button
        type="button"
        onClick={() => setOpen((o) => !o)}
        aria-expanded={open}
        className="flex w-full items-center gap-3 px-4 py-3.5 text-left transition hover:bg-neutral-50"
      >
        <div className="grid h-10 w-10 shrink-0 place-items-center rounded-full bg-usu-light text-sm font-bold text-usu-dark">
          {session.nim ? session.nim.slice(-2) : '?'}
        </div>
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-2">
            <span className="font-semibold text-neutral-900">NIM {session.nim || '-'}</span>
            {session.errorCount > 0 && <StatusBadge status="error" />}
          </div>
          <div className="text-xs text-neutral-500">
            Mulai {formatMinuteLabel(session.startTime)} &middot; {num(session.turns.length)} pesan
          </div>
        </div>
        <span
          className="hidden font-mono text-[11px] text-neutral-400 sm:inline"
          title={`Session ${session.sessionId}`}
        >
          sesi {session.sessionId ? `${session.sessionId.slice(0, 8)}\u2026` : '-'}
        </span>
        <svg
          viewBox="0 0 24 24"
          width="16"
          height="16"
          fill="none"
          stroke="currentColor"
          strokeWidth={2}
          strokeLinecap="round"
          strokeLinejoin="round"
          className={`shrink-0 text-neutral-400 transition-transform ${open ? 'rotate-180' : ''}`}
          aria-hidden="true"
        >
          <path d="m6 9 6 6 6-6" />
        </svg>
      </button>

      {open && (
        <div className="space-y-3 border-t border-neutral-100 bg-neutral-50 px-4 py-4">
          {session.turns.map((t, i) => (
            <TurnDetail key={t.id} entry={t} index={i} />
          ))}
        </div>
      )}
    </article>
  );
}

export function ChatHistoryPage() {
  const [limit, setLimit] = useState(100);
  const [report, setReport] = useState<ChatHistoryResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [lastUpdated, setLastUpdated] = useState<Date | null>(null);

  const load = useCallback(
    async (showSpinner: boolean) => {
      if (showSpinner) setLoading(true);
      try {
        const data = await fetchChatHistory(limit);
        setReport(data);
        setError(null);
        setLastUpdated(new Date());
      } catch {
        setError('Gagal memuat riwayat. Pastikan server berjalan.');
      } finally {
        setLoading(false);
      }
    },
    [limit],
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

  const entries = report?.entries ?? [];
  const sessions = useMemo(() => groupSessions(entries), [entries]);

  return (
    <div className="flex min-h-screen flex-col bg-neutral-50">
      <header className="sticky top-0 z-10 flex items-center justify-between gap-3 bg-gradient-to-r from-usu to-usu-dark px-5 py-3.5 text-white shadow">
        <div className="flex items-center gap-3">
          <span className="font-bold">KRS Agent</span>
          <span className="hidden text-sm text-white/80 sm:inline">Riwayat Chat</span>
        </div>
        <div className="flex items-center gap-1">
          <Link to="/monitoring" className="rounded-lg px-3 py-1.5 text-sm font-medium text-white/90 transition hover:bg-white/15">
            Token
          </Link>
          <Link to="/dashboard" className="rounded-lg px-3 py-1.5 text-sm font-medium text-white/90 transition hover:bg-white/15">
            Dashboard
          </Link>
        </div>
      </header>

      <main className="mx-auto w-full max-w-4xl px-4 py-8 sm:px-5">
        <div className="mb-6 flex flex-wrap items-end justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold text-neutral-900">Riwayat Chat Pengguna</h1>
            <p className="text-sm text-neutral-500">
              Dikelompokkan per sesi (terbaru di atas). Klik kartu untuk melihat detail percakapan.
            </p>
          </div>
          <div className="flex flex-wrap items-center gap-3">
            <label className="flex items-center gap-2 text-sm text-neutral-600">
              <span>Tampil</span>
              <select
                value={limit}
                onChange={(e) => setLimit(Number(e.target.value))}
                className="rounded-lg border border-neutral-300 bg-white px-3 py-1.5 text-sm font-medium text-neutral-800 outline-none focus:border-usu"
              >
                {LIMIT_OPTIONS.map((o) => (
                  <option key={o.value} value={o.value}>{o.label}</option>
                ))}
              </select>
            </label>
            <button
              type="button"
              onClick={() => void load(true)}
              className="rounded-lg bg-usu px-3 py-1.5 text-sm font-medium text-white transition hover:bg-usu-dark"
            >
              Muat ulang
            </button>
          </div>
        </div>

        {report && (
          <div className="mb-5 flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-neutral-500">
            <span>
              <span className="font-medium text-neutral-700">{num(sessions.length)}</span> sesi
              {' '}&middot;{' '}
              <span className="font-medium text-neutral-700">{num(report.count)}</span> pesan
            </span>
            <span className="flex items-center gap-1.5">
              <span className={`inline-block h-2 w-2 rounded-full ${report.enabled ? 'bg-usu' : 'bg-neutral-300'}`} />
              {report.enabled ? 'Penyimpanan aktif' : 'Penyimpanan nonaktif'}
            </span>
            {lastUpdated && <span>Diperbarui {lastUpdated.toLocaleTimeString('id-ID')}</span>}
          </div>
        )}

        {report && !report.enabled && (
          <div className="mb-6 rounded-xl border border-gold-dark/30 bg-gold-light px-4 py-3 text-sm text-neutral-700">
            &#9888; Penyimpanan riwayat nonaktif, jadi riwayat masih kosong. Pastikan konfigurasi
            <code className="mx-1 rounded bg-white px-1">enabled</code> sudah benar lalu restart server.
          </div>
        )}

        {error && (
          <div className="mb-6 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div>
        )}

        {loading && !report ? (
          <div className="flex h-72 items-center justify-center rounded-2xl border border-neutral-200 bg-white text-sm text-neutral-400 shadow-sm">
            Memuat&hellip;
          </div>
        ) : sessions.length === 0 ? (
          <div className="flex h-60 items-center justify-center rounded-2xl border border-neutral-200 bg-white text-sm text-neutral-400 shadow-sm">
            Belum ada percakapan.
          </div>
        ) : (
          <div className="space-y-3">
            {sessions.map((s) => (
              <SessionCard key={s.key} session={s} />
            ))}
          </div>
        )}
      </main>
    </div>
  );
}
