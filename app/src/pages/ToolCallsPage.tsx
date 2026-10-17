import { useMemo, useState } from 'react';
import { fetchToolCalls, fetchToolUsage } from '../api/monitoring';
import { MonitoringLayout } from '../components/monitoring/MonitoringLayout';
import {
  Badge,
  Card,
  EmptyState,
  PageHeader,
  ProgressBar,
  Select,
  StatTile,
  StatusBadge,
  ToolbarButton,
} from '../components/monitoring/primitives';
import { IconAlert, IconRefresh, IconTool } from '../components/monitoring/icons';
import { useResource } from '../components/monitoring/useResource';
import { formatDateTime, formatDuration } from '../utils/datetime';
import { formatDecimal, formatInt as num, formatPercent as pct } from '../utils/number';

const REFRESH_MS = 30_000;

const LIMIT_OPTIONS = [
  { value: 50, label: '50 panggilan' },
  { value: 100, label: '100 panggilan' },
  { value: 200, label: '200 panggilan' },
  { value: 500, label: '500 panggilan' },
];

export function ToolCallsPage() {
  const [limit, setLimit] = useState(100);
  const [toolFilter, setToolFilter] = useState('all');
  const [statusFilter, setStatusFilter] = useState('all');
  const [openId, setOpenId] = useState<string | null>(null);

  const resource = useResource(
    () => Promise.all([fetchToolUsage(), fetchToolCalls(limit)]).then(([usage, calls]) => ({ usage, calls })),
    [limit],
    REFRESH_MS,
  );

  const usage = resource.data?.usage.tools ?? [];
  const calls = resource.data?.calls.entries ?? [];

  const totals = useMemo(() => {
    const allCalls = usage.reduce((a, t) => a + t.calls, 0);
    const errors = usage.reduce((a, t) => a + t.errors, 0);
    const weighted = usage.reduce((a, t) => a + t.avgDurationMs * t.calls, 0);
    return {
      calls: allCalls,
      errors,
      errorRate: allCalls > 0 ? (errors / allCalls) * 100 : 0,
      avgDuration: allCalls > 0 ? weighted / allCalls : 0,
      distinct: usage.length,
    };
  }, [usage]);

  const filtered = useMemo(
    () =>
      calls.filter((call) => {
        if (toolFilter !== 'all' && call.toolName !== toolFilter) return false;
        if (statusFilter !== 'all' && call.status !== statusFilter) return false;
        return true;
      }),
    [calls, toolFilter, statusFilter],
  );

  const toolOptions = [
    { value: 'all', label: 'Semua tool' },
    ...usage.map((t) => ({ value: t.toolName, label: t.toolName })),
  ];

  return (
    <MonitoringLayout>
      <PageHeader
        eyebrow="Monitoring"
        title="Pemanggilan Tool"
        description="Setiap tool yang dipanggil agent, lengkap dengan argumen, status, dan durasinya."
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

      <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
        <StatTile
          label="Total panggilan"
          value={num(totals.calls)}
          hint={`${num(totals.distinct)} jenis tool`}
          icon={<IconTool size={17} />}
          tone="info"
        />
        <StatTile
          label="Rata-rata durasi"
          value={formatDuration(totals.avgDuration)}
          hint="Berbobot jumlah panggilan"
        />
        <StatTile
          label="Panggilan gagal"
          value={num(totals.errors)}
          hint={pct(totals.errorRate)}
          icon={<IconAlert size={17} />}
          tone="danger"
        />
        <StatTile
          label="Ditampilkan"
          value={num(filtered.length)}
          hint={`dari ${num(calls.length)} panggilan terbaru`}
        />
      </div>

      <Card
        className="mt-5"
        title="Ringkasan per tool"
        description="Agregat seluruh data yang tersimpan."
        bodyClassName="px-0 py-0"
      >
        {usage.length === 0 ? (
          <EmptyState
            title="Belum ada tool call"
            description="Data akan terisi saat agent mulai memanggil tool."
            icon={<IconTool size={26} />}
          />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[560px] text-sm">
              <thead>
                <tr className="border-b border-neutral-100 text-left text-[10.5px] uppercase tracking-[0.1em] text-neutral-400">
                  <th className="px-5 py-2.5 font-semibold">Tool</th>
                  <th className="px-3 py-2.5 text-right font-semibold">Panggilan</th>
                  <th className="px-3 py-2.5 text-right font-semibold">Error</th>
                  <th className="px-3 py-2.5 text-right font-semibold">Rata-rata</th>
                  <th className="px-5 py-2.5 font-semibold">Porsi</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-neutral-100">
                {usage.map((tool) => {
                  const share = totals.calls > 0 ? (tool.calls / totals.calls) * 100 : 0;
                  return (
                    <tr key={tool.toolName} className="transition hover:bg-neutral-50">
                      <td className="px-5 py-2.5 font-mono text-[11.5px] text-neutral-700">
                        {tool.toolName}
                      </td>
                      <td className="px-3 py-2.5 text-right font-semibold text-neutral-800">
                        {num(tool.calls)}
                      </td>
                      <td className="px-3 py-2.5 text-right">
                        {tool.errors > 0 ? (
                          <span className="font-semibold text-red-600">{num(tool.errors)}</span>
                        ) : (
                          <span className="text-neutral-300">0</span>
                        )}
                      </td>
                      <td className="px-3 py-2.5 text-right text-neutral-500">
                        {formatDuration(tool.avgDurationMs)}
                      </td>
                      <td className="px-5 py-2.5">
                        <div className="flex items-center gap-2">
                          <ProgressBar value={share} tone="info" className="max-w-[160px]" />
                          <span className="w-10 text-right text-[10.5px] text-neutral-400">
                            {pct(share)}
                          </span>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      <Card
        className="mt-5"
        title="Log panggilan terbaru"
        description="Klik satu baris untuk melihat argumen yang dikirim agent."
        action={
          <div className="flex flex-wrap items-center gap-2">
            <Select value={toolFilter} onChange={setToolFilter} options={toolOptions} />
            <Select
              value={statusFilter}
              onChange={setStatusFilter}
              options={[
                { value: 'all', label: 'Semua status' },
                { value: 'OK', label: 'OK' },
                { value: 'ERROR', label: 'Error' },
              ]}
            />
            <Select value={limit} onChange={(v) => setLimit(Number(v))} options={LIMIT_OPTIONS} />
          </div>
        }
        bodyClassName="px-0 py-0"
      >
        {filtered.length === 0 ? (
          <EmptyState title="Tidak ada panggilan yang cocok" icon={<IconTool size={26} />} />
        ) : (
          <ul className="divide-y divide-neutral-100">
            {filtered.map((call) => {
              const id = `${call.chatId}-${call.seq}-${call.timestamp}`;
              const open = openId === id;
              return (
                <li key={id}>
                  <button
                    type="button"
                    onClick={() => setOpenId(open ? null : id)}
                    aria-expanded={open}
                    className="flex w-full items-center gap-3 px-5 py-2.5 text-left transition hover:bg-neutral-50"
                  >
                    <span className="w-8 shrink-0 text-[10.5px] font-semibold text-neutral-400">
                      #{call.seq}
                    </span>
                    <span className="w-48 shrink-0 truncate font-mono text-[11.5px] text-neutral-700">
                      {call.toolName}
                    </span>
                    <StatusBadge status={call.status} />
                    <span className="hidden text-[11px] text-neutral-400 sm:inline">{call.nim}</span>
                    <span className="ml-auto shrink-0 text-[11px] text-neutral-500">
                      {formatDuration(call.durationMs)}
                    </span>
                    <span className="hidden w-20 shrink-0 text-right text-[10.5px] text-neutral-400 md:inline">
                      {num(call.resultSize)} B
                    </span>
                    <span className="hidden w-32 shrink-0 text-right text-[10.5px] text-neutral-400 lg:inline">
                      {formatDateTime(call.timestamp)}
                    </span>
                  </button>
                  {open && (
                    <div className="border-t border-neutral-100 bg-neutral-50 px-5 py-3">
                      <div className="mb-1 flex flex-wrap items-center gap-2 text-[10.5px] text-neutral-400">
                        <Badge tone="neutral">chat {call.chatId.slice(0, 8)}</Badge>
                        <Badge tone="neutral">sesi {call.sessionId.slice(0, 8)}</Badge>
                        <span>hasil {num(call.resultSize)} byte</span>
                      </div>
                      <pre className="max-h-64 overflow-auto whitespace-pre-wrap break-words rounded-xl bg-white p-3 text-[11.5px] leading-relaxed text-neutral-700 ring-1 ring-neutral-200">
                        {call.arguments || '{}'}
                      </pre>
                    </div>
                  )}
                </li>
              );
            })}
          </ul>
        )}
      </Card>

      <p className="mt-5 text-[11px] text-neutral-400">
        Total {formatDecimal(totals.calls)} panggilan tersimpan. Rata-rata {formatDuration(totals.avgDuration)} per panggilan.
      </p>
    </MonitoringLayout>
  );
}
