import { useMemo, useState } from 'react';
import { fetchCspStats } from '../api/monitoring';
import { MonitoringLayout } from '../components/monitoring/MonitoringLayout';
import {
  Card,
  EmptyState,
  PageHeader,
  ProgressBar,
  Select,
  StatTile,
  StatusBadge,
  ToolbarButton,
} from '../components/monitoring/primitives';
import { IconRefresh, IconSolver } from '../components/monitoring/icons';
import { useResource } from '../components/monitoring/useResource';
import { formatDateTime, formatDuration } from '../utils/datetime';
import { formatDecimal, formatInt as num, formatPercent as pct } from '../utils/number';

const REFRESH_MS = 30_000;

const LIMIT_OPTIONS = [
  { value: 25, label: '25 run' },
  { value: 50, label: '50 run' },
  { value: 100, label: '100 run' },
  { value: 200, label: '200 run' },
];

export function SolverPage() {
  const [limit, setLimit] = useState(50);
  const resource = useResource(() => fetchCspStats(limit), [limit], REFRESH_MS);
  const data = resource.data;
  const summary = data?.summary;
  const recent = data?.recent ?? [];

  const feasibleRate =
    summary && summary.totalSolves > 0 ? (summary.feasibleCount / summary.totalSolves) * 100 : 0;

  const maxDuration = useMemo(
    () => recent.reduce((a, run) => Math.max(a, run.durationMs), 1),
    [recent],
  );

  return (
    <MonitoringLayout>
      <PageHeader
        eyebrow="Monitoring"
        title="Solver CSP"
        description="Biaya pencarian dan hasil penempatan mata kuliah pada setiap proses penyusunan KRS."
        actions={
          <>
            <Select value={limit} onChange={(v) => setLimit(Number(v))} options={LIMIT_OPTIONS} />
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
          label="Total penyelesaian"
          value={num(summary?.totalSolves ?? 0)}
          hint={`${num(summary?.feasibleCount ?? 0)} layak, ${num(summary?.infeasibleCount ?? 0)} tidak`}
          icon={<IconSolver size={17} />}
          tone="info"
        />
        <StatTile
          label="Rasio layak"
          value={pct(feasibleRate)}
          hint="Draft berhasil ditempatkan"
          tone="success"
        />
        <StatTile
          label="Rata-rata durasi"
          value={formatDuration(summary?.avgDurationMs ?? 0)}
          hint="Termasuk preprocessing"
        />
        <StatTile
          label="Rata-rata simpul"
          value={num(summary?.avgNodes ?? 0)}
          hint={`${num(summary?.avgBacktracks ?? 0)} backtrack`}
        />
      </div>

      <div className="mt-5 grid grid-cols-1 gap-5 lg:grid-cols-3">
        <Card
          className="lg:col-span-2"
          title="Biaya pencarian"
          description="Durasi relatif dari proses terakhir yang ditampilkan."
        >
          {recent.length === 0 ? (
            <EmptyState title="Belum ada proses solver" icon={<IconSolver size={26} />} />
          ) : (
            <ul className="space-y-2.5">
              {recent.slice(0, 10).map((run) => (
                <li key={run.id} className="flex items-center gap-3 text-[11px]">
                  <span className="w-16 shrink-0 font-mono text-neutral-500">{run.nim}</span>
                  <StatusBadge status={run.status} />
                  <div className="min-w-0 flex-1">
                    <ProgressBar
                      value={(run.durationMs / maxDuration) * 100}
                      tone={run.status === 'FEASIBLE' ? 'success' : 'danger'}
                    />
                  </div>
                  <span className="w-16 shrink-0 text-right text-neutral-500">
                    {run.durationMs} ms
                  </span>
                  <span className="hidden w-16 shrink-0 text-right text-neutral-400 sm:inline">
                    {num(run.nodes)} simpul
                  </span>
                </li>
              ))}
            </ul>
          )}
        </Card>

        <Card title="Rata-rata biaya" description="Agregat seluruh run tersimpan.">
          <dl className="space-y-3 text-xs">
            {[
              { label: 'Durasi', value: formatDuration(summary?.avgDurationMs ?? 0) },
              { label: 'Simpul dikunjungi', value: num(summary?.avgNodes ?? 0) },
              { label: 'Backtrack', value: num(summary?.avgBacktracks ?? 0) },
              { label: 'Revisi AC-3', value: num(summary?.avgAc3Revisions ?? 0) },
              { label: 'Variabel', value: formatDecimal(summary?.avgVariables ?? 0) },
              { label: 'Nilai domain', value: formatDecimal(summary?.avgDomainValues ?? 0) },
            ].map((row) => (
              <div key={row.label} className="flex items-center justify-between gap-3">
                <dt className="text-neutral-500">{row.label}</dt>
                <dd className="font-semibold text-neutral-800">{row.value}</dd>
              </div>
            ))}
          </dl>
        </Card>
      </div>

      <Card
        className="mt-5"
        title="Riwayat penyelesaian"
        description="Semua kolom yang tersimpan di tabel csp_solve_log."
        bodyClassName="px-0 py-0"
      >
        {recent.length === 0 ? (
          <EmptyState
            title="Belum ada data solver"
            description="Data muncul setelah agent memanggil solveKrs."
            icon={<IconSolver size={26} />}
          />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[1080px] text-xs">
              <thead>
                <tr className="border-b border-neutral-100 text-left text-[10px] uppercase tracking-[0.08em] text-neutral-400">
                  <th className="px-4 py-2.5 font-semibold">Waktu</th>
                  <th className="px-3 py-2.5 font-semibold">NIM</th>
                  <th className="px-3 py-2.5 text-center font-semibold">Sem</th>
                  <th className="px-3 py-2.5 font-semibold">Status</th>
                  <th className="px-3 py-2.5 font-semibold">Tujuan</th>
                  <th className="px-3 py-2.5 text-right font-semibold">SKS</th>
                  <th className="px-3 py-2.5 text-right font-semibold">Var</th>
                  <th className="px-3 py-2.5 text-right font-semibold">Const</th>
                  <th className="px-3 py-2.5 text-right font-semibold">Domain</th>
                  <th className="px-3 py-2.5 text-right font-semibold">Simpul</th>
                  <th className="px-3 py-2.5 text-right font-semibold">Back</th>
                  <th className="px-3 py-2.5 text-right font-semibold">AC-3</th>
                  <th className="px-3 py-2.5 text-right font-semibold">Forward</th>
                  <th className="px-3 py-2.5 text-right font-semibold">Durasi</th>
                  <th className="px-3 py-2.5 text-right font-semibold">Alt</th>
                  <th className="px-3 py-2.5 font-semibold">Chat</th>
                  <th className="px-4 py-2.5 text-center font-semibold">MBKM</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-neutral-100">
                {recent.map((run) => (
                  <tr key={run.id} className="transition hover:bg-neutral-50">
                    <td className="whitespace-nowrap px-4 py-2 text-[10.5px] text-neutral-500">
                      {formatDateTime(run.timestamp)}
                    </td>
                    <td className="px-3 py-2 font-mono text-[10.5px] text-neutral-600">{run.nim}</td>
                    <td className="px-3 py-2 text-center text-neutral-500">{run.semester}</td>
                    <td className="px-3 py-2">
                      <StatusBadge status={run.status} />
                    </td>
                    <td className="px-3 py-2 text-[10.5px] text-neutral-500">
                      {run.objective.replaceAll('_', ' ').toLowerCase()}
                    </td>
                    <td className="px-3 py-2 text-right font-semibold text-neutral-700">
                      {run.resultSks}/{run.targetSks}
                    </td>
                    <td className="px-3 py-2 text-right text-neutral-500">{run.variables}</td>
                    <td className="px-3 py-2 text-right text-neutral-500">{run.constraints}</td>
                    <td className="px-3 py-2 text-right text-neutral-500">{num(run.domainValues)}</td>
                    <td className="px-3 py-2 text-right text-neutral-500">{num(run.nodes)}</td>
                    <td className="px-3 py-2 text-right text-neutral-500">{num(run.backtracks)}</td>
                    <td className="px-3 py-2 text-right text-neutral-500">{num(run.ac3Revisions)}</td>
                    <td className="px-3 py-2 text-right text-neutral-500">{num(run.forwardChecks)}</td>
                    <td className="px-3 py-2 text-right text-neutral-500">{run.durationMs} ms</td>
                    <td className="px-3 py-2 text-right text-neutral-500">{run.alternatives}</td>
                    <td className="px-3 py-2 font-mono text-[10px] text-neutral-400">
                      {run.chatId ? run.chatId.slice(0, 8) : '-'}
                    </td>
                    <td className="px-4 py-2 text-center">
                      {run.mbkmMode ? (
                        <span className="text-[10px] font-semibold text-usu-dark">ya</span>
                      ) : (
                        <span className="text-neutral-300">tidak</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>
    </MonitoringLayout>
  );
}
