import { useEffect, useMemo, useState } from 'react';
import type { ChatHistoryEntry } from '../types/api';
import { fetchChatHistory } from '../api/monitoring';
import { MonitoringLayout } from '../components/monitoring/MonitoringLayout';
import {
  Badge,
  Card,
  EmptyState,
  PageHeader,
  Select,
  StatusBadge,
  ToolbarButton,
} from '../components/monitoring/primitives';
import {
  IconArrowLeft,
  IconChat,
  IconClock,
  IconRefresh,
  IconSearch,
  IconToken,
} from '../components/monitoring/icons';
import { useResource } from '../components/monitoring/useResource';
import { Markdown } from '../components/Markdown';
import { formatDateTime, formatDuration, relativeTime, timeOf } from '../utils/datetime';
import { formatInt as num } from '../utils/number';

const REFRESH_MS = 30_000;

const LIMIT_OPTIONS = [
  { value: 50, label: '50 giliran' },
  { value: 100, label: '100 giliran' },
  { value: 200, label: '200 giliran' },
  { value: 500, label: '500 giliran' },
];

const STATUS_OPTIONS = [
  { value: 'all', label: 'Semua status' },
  { value: 'final', label: 'Selesai' },
  { value: 'error', label: 'Error' },
  { value: 'cancelled', label: 'Dibatalkan' },
];

type Session = {
  key: string;
  sessionId: string;
  nim: string;
  startTime: string;
  lastTime: string;
  turns: ChatHistoryEntry[];
  errors: number;
  tokens: number;
};

function groupSessions(entries: ChatHistoryEntry[]): Session[] {
  const map = new Map<string, ChatHistoryEntry[]>();
  for (const entry of entries) {
    const key = entry.sessionId || `__no-session-${entry.id}`;
    const list = map.get(key);
    if (list) list.push(entry);
    else map.set(key, [entry]);
  }

  const sessions: Session[] = [];
  for (const [key, list] of map) {
    const sorted = [...list].sort((a, b) => a.timestamp.localeCompare(b.timestamp));
    sessions.push({
      key,
      sessionId: sorted[0].sessionId,
      nim: sorted[0].nim,
      startTime: sorted[0].timestamp,
      lastTime: sorted[sorted.length - 1].timestamp,
      turns: sorted,
      errors: sorted.filter((t) => t.status === 'error').length,
      tokens: sorted.reduce((a, t) => a + (t.totalTokens ?? 0), 0),
    });
  }
  sessions.sort((a, b) => b.lastTime.localeCompare(a.lastTime));
  return sessions;
}

function TurnCard({ entry, index }: { entry: ChatHistoryEntry; index: number }) {
  const [showSteps, setShowSteps] = useState(false);
  return (
    <article className="rounded-2xl border border-neutral-200/90 bg-white p-4 shadow-[0_1px_2px_rgba(16,24,40,0.04)]">
      <header className="flex flex-wrap items-center gap-x-2 gap-y-1.5 text-[11px] text-neutral-400">
        <span className="flex h-6 w-6 items-center justify-center rounded-full bg-neutral-100 text-[10px] font-bold text-neutral-600">
          {index + 1}
        </span>
        <StatusBadge status={entry.status} />
        <span>{timeOf(entry.timestamp)}</span>
        <span className="text-neutral-300">|</span>
        <span className="inline-flex items-center gap-1">
          <IconClock size={12} /> {formatDuration(entry.durationMs)}
        </span>
        <span className="text-neutral-300">|</span>
        <span className="inline-flex items-center gap-1">
          <IconToken size={12} /> {num(entry.totalTokens ?? 0)}
        </span>
        {entry.model && (
          <>
            <span className="text-neutral-300">|</span>
            <span className="font-mono">{entry.model}</span>
          </>
        )}
        <span className="text-neutral-300">|</span>
        <span>{entry.streamed ? 'Streaming' : 'Non-stream'}</span>
      </header>

      <div className="mt-3">
        <div className="text-[10.5px] font-semibold uppercase tracking-[0.12em] text-neutral-400">
          Permintaan
        </div>
        <p className="mt-1 whitespace-pre-wrap wrap-break-word rounded-xl bg-neutral-50 px-3 py-2.5 text-[13px] leading-relaxed text-neutral-800">
          {entry.request || '-'}
        </p>
      </div>

      <div className="mt-3">
        <div className="text-[10.5px] font-semibold uppercase tracking-[0.12em] text-neutral-400">
          Respons
        </div>
        {entry.response ? (
          <div className="mt-1 rounded-xl border border-neutral-100 px-3 py-2.5 text-[13px]">
            <Markdown content={entry.response} />
          </div>
        ) : (
          <p className="mt-1 rounded-xl bg-neutral-50 px-3 py-2.5 text-[13px] italic text-neutral-400">
            Tidak ada respons (percakapan dibatalkan sebelum selesai).
          </p>
        )}
      </div>

      {entry.steps.length > 0 && (
        <div className="mt-3">
          <button
            type="button"
            onClick={() => setShowSteps((v) => !v)}
            className="text-[11px] font-semibold text-usu-dark transition hover:text-usu"
          >
            {showSteps ? 'Sembunyikan' : 'Lihat'} {entry.steps.length} langkah
          </button>
          {showSteps && (
            <ol className="mt-2 flex flex-wrap gap-1.5">
              {entry.steps.map((step, i) => (
                <li
                  key={`${entry.id}-step-${i}`}
                  className="rounded-full bg-usu-light px-2.5 py-1 text-[11px] font-medium text-usu-dark"
                >
                  {i + 1}. {step}
                </li>
              ))}
            </ol>
          )}
        </div>
      )}

      <footer className="mt-3 flex flex-wrap gap-x-4 gap-y-1 border-t border-neutral-100 pt-2 text-[10.5px] text-neutral-400">
        <span>{num(entry.llmCalls ?? 0)} panggilan LLM</span>
        <span>{num(entry.toolCalls ?? 0)} tool call</span>
        <span>input {num(entry.inputTokens ?? 0)}</span>
        <span>output {num(entry.outputTokens ?? 0)}</span>
      </footer>
    </article>
  );
}

export function ChatHistoryPage() {
  const [limit, setLimit] = useState(100);
  const [status, setStatus] = useState('all');
  const [query, setQuery] = useState('');
  const [selectedKey, setSelectedKey] = useState<string | null>(null);

  const resource = useResource(() => fetchChatHistory(limit), [limit], REFRESH_MS);
  const entries = resource.data?.entries ?? [];

  const sessions = useMemo(() => {
    const filtered = entries.filter((e) => {
      if (status !== 'all' && e.status !== status) return false;
      if (!query.trim()) return true;
      const q = query.trim().toLowerCase();
      return (
        e.nim.toLowerCase().includes(q) ||
        e.request.toLowerCase().includes(q) ||
        e.response.toLowerCase().includes(q)
      );
    });
    return groupSessions(filtered);
  }, [entries, status, query]);

  // Keep the selection valid across refreshes; fall back to the newest session.
  useEffect(() => {
    if (sessions.length === 0) {
      if (selectedKey !== null) setSelectedKey(null);
      return;
    }
    if (!selectedKey || !sessions.some((s) => s.key === selectedKey)) {
      setSelectedKey(sessions[0].key);
    }
  }, [sessions, selectedKey]);

  const selected = sessions.find((s) => s.key === selectedKey) ?? null;

  return (
    <MonitoringLayout>
      <PageHeader
        eyebrow="Monitoring"
        title="Riwayat Percakapan"
        description="Setiap giliran percakapan, dikelompokkan per sesi. Pilih sesi di kiri untuk membaca percakapan lengkap."
        actions={
          <>
            <span className="text-[11px] text-neutral-400">
              {resource.updatedAt
                ? `Diperbarui ${resource.updatedAt.toLocaleTimeString('id-ID', { hour: '2-digit', minute: '2-digit' })}`
                : 'Memuat...'}
            </span>
            <ToolbarButton onClick={resource.reload} title="Muat ulang">
              <IconRefresh size={15} />
              Muat ulang
            </ToolbarButton>
          </>
        }
      />

      {resource.error && (
        <div className="mb-5 rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {resource.error}
        </div>
      )}

      <div className="grid grid-cols-1 gap-5 lg:grid-cols-[340px_minmax(0,1fr)]">
        <div className={`${selected ? 'hidden lg:block' : 'block'}`}>
          <div className="mb-3 flex flex-wrap items-center gap-2">
            <div className="relative min-w-0 flex-1">
              <span className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-neutral-400">
                <IconSearch size={15} />
              </span>
              <input
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                placeholder="Cari NIM atau isi pesan"
                className="h-9 w-full rounded-full border border-neutral-200 bg-white pl-9 pr-3 text-xs text-neutral-700 outline-hidden transition placeholder:text-neutral-400 focus:border-usu"
              />
            </div>
            <Select value={status} onChange={setStatus} options={STATUS_OPTIONS} />
          </div>

          <div className="mb-3 flex items-center justify-between gap-2 px-1">
            <span className="text-[11px] font-medium text-neutral-400">
              {num(sessions.length)} sesi
            </span>
            <Select
              value={limit}
              onChange={(v) => setLimit(Number(v))}
              options={LIMIT_OPTIONS}
            />
          </div>

          <div className="max-h-[calc(100vh-15rem)] space-y-2 overflow-y-auto pr-1">
            {sessions.length === 0 ? (
              <Card>
                <EmptyState
                  title="Tidak ada sesi"
                  description="Coba ubah filter atau mulai percakapan baru dengan agent."
                  icon={<IconChat size={26} />}
                />
              </Card>
            ) : (
              sessions.map((session) => {
                const active = session.key === selectedKey;
                return (
                  <button
                    key={session.key}
                    type="button"
                    onClick={() => setSelectedKey(session.key)}
                    className={`w-full rounded-2xl border px-4 py-3 text-left transition ${
                      active
                        ? 'border-usu/40 bg-usu-light/70 shadow-[0_1px_2px_rgba(16,24,40,0.04)]'
                        : 'border-neutral-200/90 bg-white hover:border-neutral-300'
                    }`}
                  >
                    <div className="flex items-center gap-2.5">
                      <span
                        className={`flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-[11px] font-bold ${
                          active ? 'bg-usu text-white' : 'bg-neutral-100 text-neutral-600'
                        }`}
                      >
                        {session.nim ? session.nim.slice(-2) : '?'}
                      </span>
                      <div className="min-w-0 flex-1">
                        <div className="flex items-center gap-1.5">
                          <span className="truncate text-[13px] font-semibold text-neutral-900">
                            {session.nim || 'Tanpa NIM'}
                          </span>
                          {session.errors > 0 && <Badge tone="danger">{session.errors} error</Badge>}
                        </div>
                        <div className="text-[11px] text-neutral-500">
                          {relativeTime(session.lastTime)} | {num(session.turns.length)} giliran
                        </div>
                      </div>
                    </div>
                    <div className="mt-2 flex items-center gap-3 text-[10.5px] text-neutral-400">
                      <span className="inline-flex items-center gap-1">
                        <IconToken size={11} /> {num(session.tokens)}
                      </span>
                      <span className="truncate font-mono">{session.sessionId.slice(0, 8) || '-'}</span>
                    </div>
                  </button>
                );
              })
            )}
          </div>
        </div>

        <div className={`${selected ? 'block' : 'hidden lg:block'}`}>
          {!selected ? (
            <Card>
              <EmptyState
                title="Pilih satu sesi"
                description="Detail percakapan akan tampil di sini."
                icon={<IconChat size={28} />}
              />
            </Card>
          ) : (
            <>
              <div className="mb-4 rounded-2xl border border-neutral-200/90 bg-white p-4 shadow-[0_1px_2px_rgba(16,24,40,0.04)]">
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div className="flex items-center gap-3">
                    <button
                      type="button"
                      onClick={() => setSelectedKey(null)}
                      aria-label="Kembali ke daftar sesi"
                      className="flex h-9 w-9 items-center justify-center rounded-full border border-neutral-200 text-neutral-500 transition hover:bg-neutral-50 lg:hidden"
                    >
                      <IconArrowLeft size={17} />
                    </button>
                    <div>
                      <div className="text-[15px] font-semibold text-neutral-900">
                        NIM {selected.nim || '-'}
                      </div>
                      <div className="text-[11px] text-neutral-500">
                        Mulai {formatDateTime(selected.startTime)} | {num(selected.turns.length)} giliran
                      </div>
                    </div>
                  </div>
                  <div className="flex flex-wrap items-center gap-2">
                    {selected.errors > 0 && <Badge tone="danger">{selected.errors} error</Badge>}
                    <Badge tone="info">{num(selected.tokens)} token</Badge>
                    <Badge tone="neutral">sesi {selected.sessionId.slice(0, 8) || '-'}</Badge>
                  </div>
                </div>
              </div>

              <div className="space-y-3">
                {selected.turns.map((turn, index) => (
                  <TurnCard key={turn.id} entry={turn} index={index} />
                ))}
              </div>
            </>
          )}
        </div>
      </div>
    </MonitoringLayout>
  );
}
