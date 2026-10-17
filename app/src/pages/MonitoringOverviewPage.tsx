import { Link } from 'react-router-dom';
import {
  fetchChatHistory,
  fetchCspStats,
  fetchEvaluationSummary,
  fetchSurveySummary,
  fetchTokenUsage,
  fetchToolUsage,
} from '../api/monitoring';
import { MonitoringLayout } from '../components/monitoring/MonitoringLayout';
import {
  Badge,
  Card,
  EmptyState,
  PageHeader,
  ProgressBar,
  StatTile,
  StatusBadge,
  ToolbarButton,
} from '../components/monitoring/primitives';
import {
  IconChat,
  IconClock,
  IconEvaluation,
  IconRefresh,
  IconSolver,
  IconToken,
  IconTool,
} from '../components/monitoring/icons';
import { useResource } from '../components/monitoring/useResource';
import { formatDuration, formatMinuteLabel, relativeTime } from '../utils/datetime';
import { formatDecimal, formatInt as num, formatPercent as pct } from '../utils/number';

const REFRESH_MS = 60_000;

export function MonitoringOverviewPage() {
  const resource = useResource(
    () =>
      Promise.all([
        fetchChatHistory(200),
        fetchTokenUsage('6h'),
        fetchCspStats(50),
        fetchToolUsage(),
        fetchEvaluationSummary(),
        fetchSurveySummary(),
      ]).then(([chat, token, csp, tools, evaluation, survey]) => ({
        chat,
        token,
        csp,
        tools,
        evaluation,
        survey,
      })),
    [],
    REFRESH_MS,
  );

  const d = resource.data;
  const entries = d?.chat.entries ?? [];
  const turns = entries.length;
  const finished = entries.filter((e) => e.status === 'final').length;
  const successRate = turns > 0 ? (finished / turns) * 100 : 0;
  const avgDuration = turns > 0 ? entries.reduce((a, e) => a + (e.durationMs ?? 0), 0) / turns : 0;
  const avgTokens = turns > 0 ? entries.reduce((a, e) => a + (e.totalTokens ?? 0), 0) / turns : 0;

  const csp = d?.csp.summary;
  const cspRate = csp && csp.totalSolves > 0 ? (csp.feasibleCount / csp.totalSolves) * 100 : 0;
  const toolErrors = d?.tools.tools.reduce((a, t) => a + t.errors, 0) ?? 0;
  const toolCalls = d?.tools.tools.reduce((a, t) => a + t.calls, 0) ?? 0;
  const toolErrorRate = toolCalls > 0 ? (toolErrors / toolCalls) * 100 : 0;
  const accuracy = (d?.evaluation.solutionAccuracy ?? 0) * 100;

  const topTools = [...(d?.tools.tools ?? [])].slice(0, 6);
  const maxToolCalls = topTools.reduce((a, t) => Math.max(a, t.calls), 1);

  return (
    <MonitoringLayout>
      <PageHeader
        eyebrow="Monitoring"
        title="Ringkasan"
        description="Gambaran singkat percakapan, biaya token, pemakaian tool, kinerja solver, dan hasil evaluasi agent."
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
          label="Percakapan terpantau"
          value={num(turns)}
          hint={`${num(finished)} selesai tanpa error`}
          icon={<IconChat size={17} />}
          tone="info"
        />
        <StatTile
          label="Tingkat sukses"
          value={pct(successRate)}
          hint={`Rata-rata ${formatDuration(avgDuration)} per giliran`}
          icon={<IconClock size={17} />}
          tone="success"
        />
        <StatTile
          label="Token terpakai"
          value={num(d?.token.totalTokens ?? 0)}
          hint={`${num(d?.token.totalLlmRequests ?? 0)} request LLM`}
          icon={<IconToken size={17} />}
          tone="warning"
        />
        <StatTile
          label="Tool call"
          value={num(toolCalls)}
          hint={`${pct(toolErrorRate)} berakhir error`}
          icon={<IconTool size={17} />}
          tone="danger"
        />
      </div>

      <div className="mt-5 grid grid-cols-1 gap-5 xl:grid-cols-3">
        <Card
          className="xl:col-span-2"
          title="Aktivitas terbaru"
          description="Giliran percakapan terakhir dari seluruh mahasiswa."
          action={
            <Link
              to="/monitoring/chat"
              className="text-xs font-semibold text-usu-dark transition hover:text-usu"
            >
              Lihat semua
            </Link>
          }
          bodyClassName="px-0 py-0"
        >
          {entries.length === 0 ? (
            <EmptyState
              title="Belum ada percakapan"
              description="Data akan muncul setelah mahasiswa mulai menggunakan agent."
              icon={<IconChat size={28} />}
            />
          ) : (
            <ul className="divide-y divide-neutral-100">
              {entries.slice(0, 8).map((entry) => (
                <li key={entry.id}>
                  <Link
                    to="/monitoring/chat"
                    className="flex items-start gap-3 px-5 py-3 transition hover:bg-neutral-50"
                  >
                    <span className="mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-usu-light text-[11px] font-bold text-usu-dark">
                      {entry.nim ? entry.nim.slice(-2) : '?'}
                    </span>
                    <div className="min-w-0 flex-1">
                      <div className="flex flex-wrap items-center gap-x-2 gap-y-1">
                        <span className="text-[13px] font-semibold text-neutral-800">{entry.nim}</span>
                        <StatusBadge status={entry.status} />
                        <span className="text-[11px] text-neutral-400">{relativeTime(entry.timestamp)}</span>
                      </div>
                      <p className="mt-0.5 line-clamp-1 text-[12.5px] text-neutral-500">
                        {entry.request || 'Tanpa permintaan'}
                      </p>
                    </div>
                    <div className="shrink-0 text-right text-[11px] text-neutral-400">
                      <div>{formatDuration(entry.durationMs)}</div>
                      <div>{num(entry.totalTokens ?? 0)} token</div>
                    </div>
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </Card>

        <Card title="Kesehatan agent" description="Empat indikator utama kualitas dan biaya.">
          <div className="space-y-4">
            <div>
              <div className="mb-1.5 flex items-center justify-between text-xs">
                <span className="font-medium text-neutral-600">Percakapan sukses</span>
                <span className="font-semibold text-neutral-800">{pct(successRate)}</span>
              </div>
              <ProgressBar value={successRate} tone="success" />
            </div>
            <div>
              <div className="mb-1.5 flex items-center justify-between text-xs">
                <span className="font-medium text-neutral-600">Solver menghasilkan draft</span>
                <span className="font-semibold text-neutral-800">{pct(cspRate)}</span>
              </div>
              <ProgressBar value={cspRate} tone="info" />
            </div>
            <div>
              <div className="mb-1.5 flex items-center justify-between text-xs">
                <span className="font-medium text-neutral-600">Akurasi solusi evaluasi</span>
                <span className="font-semibold text-neutral-800">{pct(accuracy)}</span>
              </div>
              <ProgressBar value={accuracy} tone="success" />
            </div>
            <div>
              <div className="mb-1.5 flex items-center justify-between text-xs">
                <span className="font-medium text-neutral-600">Tool call gagal</span>
                <span className="font-semibold text-neutral-800">{pct(toolErrorRate)}</span>
              </div>
              <ProgressBar value={toolErrorRate} tone="danger" />
            </div>
            <div className="grid grid-cols-2 gap-3 border-t border-neutral-100 pt-4 text-center">
              <div>
                <div className="text-lg font-semibold text-neutral-900">{num(avgTokens)}</div>
                <div className="text-[11px] text-neutral-400">token / giliran</div>
              </div>
              <div>
                <div className="text-lg font-semibold text-neutral-900">
                  {num(d?.token.avgTokensPerRequest ?? 0)}
                </div>
                <div className="text-[11px] text-neutral-400">token / request</div>
              </div>
            </div>
          </div>
        </Card>
      </div>

      <div className="mt-5 grid grid-cols-1 gap-5 xl:grid-cols-3">
        <Card
          title="Tool paling sering dipakai"
          description="Total pemanggilan sejak data monitoring dibersihkan."
          action={
            <Link to="/monitoring/tools" className="text-xs font-semibold text-usu-dark hover:text-usu">
              Detail
            </Link>
          }
        >
          {topTools.length === 0 ? (
            <EmptyState title="Belum ada tool call" icon={<IconTool size={26} />} />
          ) : (
            <ul className="space-y-3">
              {topTools.map((tool) => (
                <li key={tool.toolName}>
                  <div className="mb-1 flex items-center justify-between gap-2 text-xs">
                    <span className="truncate font-mono text-[11.5px] text-neutral-700">{tool.toolName}</span>
                    <span className="shrink-0 text-neutral-500">
                      {num(tool.calls)}x
                      {tool.errors > 0 && <span className="ml-1.5 text-red-600">{num(tool.errors)} error</span>}
                    </span>
                  </div>
                  <ProgressBar value={(tool.calls / maxToolCalls) * 100} tone="info" />
                </li>
              ))}
            </ul>
          )}
        </Card>

        <Card
          title="Solver CSP terakhir"
          description="Biaya pencarian dan hasil penempatan matkul."
          action={
            <Link to="/monitoring/solver" className="text-xs font-semibold text-usu-dark hover:text-usu">
              Detail
            </Link>
          }
          bodyClassName="px-0 py-0"
        >
          {(d?.csp.recent.length ?? 0) === 0 ? (
            <EmptyState title="Belum ada proses solver" icon={<IconSolver size={26} />} />
          ) : (
            <ul className="divide-y divide-neutral-100">
              {d!.csp.recent.slice(0, 5).map((run) => (
                <li key={run.id} className="flex items-center gap-3 px-5 py-2.5 text-xs">
                  <StatusBadge status={run.status} />
                  <span className="font-mono text-[11px] text-neutral-500">{run.nim}</span>
                  <span className="text-neutral-400">sem {run.semester}</span>
                  <span className="ml-auto font-semibold text-neutral-700">{run.resultSks} SKS</span>
                  <span className="w-14 text-right text-neutral-400">{run.durationMs} ms</span>
                </li>
              ))}
            </ul>
          )}
        </Card>

        <Card
          title="Evaluasi & kuesioner"
          description="Akurasi solusi dan penerimaan pengguna."
          action={
            <Link to="/monitoring/evaluation" className="text-xs font-semibold text-usu-dark hover:text-usu">
              Detail
            </Link>
          }
        >
          <div className="space-y-3">
            <div className="flex items-center justify-between rounded-xl bg-neutral-50 px-3 py-2.5">
              <div>
                <div className="text-xs font-semibold text-neutral-700">Solusi benar</div>
                <div className="text-[11px] text-neutral-400">
                  {num(d?.evaluation.correctRuns ?? 0)} dari {num(d?.evaluation.totalRuns ?? 0)} run
                </div>
              </div>
              <span className="text-lg font-semibold text-usu-dark">{pct(accuracy)}</span>
            </div>
            <div className="grid grid-cols-3 gap-2 text-center">
              {[
                { label: 'Precision', value: d?.evaluation.avgPrecision ?? 0 },
                { label: 'Recall', value: d?.evaluation.avgRecall ?? 0 },
                { label: 'F1', value: d?.evaluation.avgF1 ?? 0 },
              ].map((m) => (
                <div key={m.label} className="rounded-xl border border-neutral-100 py-2">
                  <div className="text-sm font-semibold text-neutral-800">{formatDecimal(m.value * 100)}%</div>
                  <div className="text-[10.5px] text-neutral-400">{m.label}</div>
                </div>
              ))}
            </div>
            {(d?.survey.instruments.length ?? 0) > 0 && (
              <div className="flex flex-wrap gap-2 border-t border-neutral-100 pt-3">
                {d!.survey.instruments.map((s) => (
                  <Badge key={s.instrument} tone="info">
                    {s.instrument}: {formatDecimal(s.avgScore)} ({num(s.responses)})
                  </Badge>
                ))}
              </div>
            )}
          </div>
        </Card>
      </div>

      <p className="mt-6 flex items-center gap-1.5 text-[11px] text-neutral-400">
        <IconEvaluation size={13} />
        Semua angka diambil langsung dari tabel monitoring di PostgreSQL, bukan perkiraan.
        {d && d.chat.count > 0 && (
          <span className="ml-1">
            Giliran terakhir: {formatMinuteLabel(entries[0].timestamp)}.
          </span>
        )}
      </p>
    </MonitoringLayout>
  );
}
