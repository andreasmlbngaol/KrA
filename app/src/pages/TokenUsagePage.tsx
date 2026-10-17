import { useMemo, useState } from 'react';
import type { CSSProperties } from 'react';
import { fetchTokenUsage } from '../api/monitoring';
import { MonitoringLayout } from '../components/monitoring/MonitoringLayout';
import {
  Card,
  EmptyState,
  PageHeader,
  ProgressBar,
  Select,
  StatTile,
  ToolbarButton,
} from '../components/monitoring/primitives';
import { IconRefresh, IconToken, IconTool } from '../components/monitoring/icons';
import { useResource } from '../components/monitoring/useResource';
import { TokenUsageChart, type MetricKey } from '../components/TokenUsageChart';
import { formatMinuteLabel, timeOf } from '../utils/datetime';
import { formatDecimal, formatInt as num, formatPercent as pct } from '../utils/number';

const RANGE_OPTIONS = [
  { value: '5m', label: '5 menit' },
  { value: '15m', label: '15 menit' },
  { value: '30m', label: '30 menit' },
  { value: '1h', label: '1 jam' },
  { value: '6h', label: '6 jam' },
];

const C = {
  total: '#13a05b',
  input: '#2563eb',
  output: '#9333ea',
  request: '#f5b800',
  tool: '#dc2626',
};

const dot = (color: string): CSSProperties => ({ backgroundColor: color });

const TOKEN_METRICS: Array<{ key: MetricKey; label: string; color: string }> = [
  { key: 'totalTokens', label: 'Total token', color: C.total },
  { key: 'inputTokens', label: 'Token input', color: C.input },
  { key: 'outputTokens', label: 'Token output', color: C.output },
];

const ACTIVITY_METRICS: Array<{ key: MetricKey; label: string; color: string }> = [
  { key: 'llmRequests', label: 'Request LLM', color: C.request },
  { key: 'toolCalls', label: 'Tool call', color: C.tool },
];

const REFRESH_MS = 60_000;

export function TokenUsagePage() {
  const [range, setRange] = useState('15m');
  const resource = useResource(() => fetchTokenUsage(range), [range], REFRESH_MS);
  const report = resource.data;

  const derived = useMemo(() => {
    if (!report) return null;
    const total = report.totalTokens;
    const inputPct = total > 0 ? (report.totalInputTokens / total) * 100 : 0;
    const outputPct = total > 0 ? (report.totalOutputTokens / total) * 100 : 0;
    const activeMinutes = report.series.filter(
      (p) => p.totalTokens > 0 || p.llmRequests > 0 || p.toolCalls > 0,
    ).length;
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
      activeMinutes,
      totalMinutes: report.series.length,
      avgPerActiveMin: activeMinutes > 0 ? total / activeMinutes : 0,
      toolsPerReq:
        report.totalLlmRequests > 0 ? report.totalToolCalls / report.totalLlmRequests : 0,
      peakValue,
      peakMinute,
      distinctTools: report.toolBreakdown.length,
    };
  }, [report]);

  const series = report?.series ?? [];

  return (
    <MonitoringLayout>
      <PageHeader
        eyebrow="Monitoring"
        title="Pemakaian Token"
        description="Metrik per menit dari pemanggilan model bahasa, disimpan persisten di PostgreSQL."
        actions={
          <>
            <Select value={range} onChange={setRange} options={RANGE_OPTIONS} label="Rentang" />
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

      {report && !report.enabled && (
        <div className="mb-5 rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">
          Penyimpanan metrik nonaktif, jadi data masih kosong. Periksa konfigurasi
          <code className="mx-1 rounded-sm bg-white px-1">enabled</code> lalu restart server.
        </div>
      )}

      <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
        <StatTile
          label="Total token"
          value={num(report?.totalTokens ?? 0)}
          hint={derived ? `${num(derived.avgPerActiveMin)} token / menit aktif` : undefined}
          icon={<IconToken size={17} />}
          tone="success"
        />
        <StatTile
          label="Token input"
          value={num(report?.totalInputTokens ?? 0)}
          hint={derived ? `${pct(derived.inputPct)} dari total` : undefined}
        />
        <StatTile
          label="Token output"
          value={num(report?.totalOutputTokens ?? 0)}
          hint={derived ? `${pct(derived.outputPct)} dari total` : undefined}
        />
        <StatTile
          label="Request LLM"
          value={num(report?.totalLlmRequests ?? 0)}
          hint={derived ? `${formatDecimal(derived.toolsPerReq)} tool / request` : undefined}
          icon={<IconTool size={17} />}
          tone="warning"
        />
      </div>

      <div className="mt-5 grid grid-cols-1 gap-5 lg:grid-cols-3">
        <Card
          className="lg:col-span-2"
          title="Distribusi input dan output"
          description={report ? `Jendela ${timeOf(report.fromMinute)} sampai ${timeOf(report.toMinute)}` : undefined}
        >
          {derived && report && report.totalTokens > 0 ? (
            <>
              <div className="mb-2 flex items-center justify-between text-xs">
                <span className="font-medium text-neutral-600">Proporsi token</span>
                <span className="text-neutral-500">
                  {pct(derived.inputPct)} input | {pct(derived.outputPct)} output
                </span>
              </div>
              <div className="flex h-3 w-full overflow-hidden rounded-full bg-neutral-100">
                <div style={{ width: `${derived.inputPct}%`, backgroundColor: C.input }} />
                <div style={{ width: `${derived.outputPct}%`, backgroundColor: C.output }} />
              </div>
              <div className="mt-3 flex flex-wrap gap-4 text-[11px] text-neutral-500">
                <span className="flex items-center gap-1.5">
                  <span className="inline-block h-2.5 w-2.5 rounded-full" style={dot(C.input)} />
                  Input {num(report.totalInputTokens)}
                </span>
                <span className="flex items-center gap-1.5">
                  <span className="inline-block h-2.5 w-2.5 rounded-full" style={dot(C.output)} />
                  Output {num(report.totalOutputTokens)}
                </span>
              </div>
            </>
          ) : (
            <EmptyState
              title="Belum ada token tercatat"
              description="Data muncul setelah agent memanggil model pada rentang ini."
              icon={<IconToken size={26} />}
            />
          )}
        </Card>

        <Card title="Statistik jendela" description="Angka turunan dari seri per menit.">
          <dl className="space-y-3 text-xs">
            {[
              {
                label: 'Menit aktif',
                value: derived ? `${num(derived.activeMinutes)} / ${num(derived.totalMinutes)}` : '-',
              },
              {
                label: 'Rata-rata token / request',
                value: num(report?.avgTokensPerRequest ?? 0),
              },
              {
                label: 'Puncak token / menit',
                value: derived ? num(derived.peakValue) : '-',
                hint: derived?.peakMinute ? `pukul ${timeOf(derived.peakMinute)}` : undefined,
              },
              { label: 'Total tool call', value: num(report?.totalToolCalls ?? 0) },
              { label: 'Jenis tool dipakai', value: num(derived?.distinctTools ?? 0) },
            ].map((row) => (
              <div key={row.label} className="flex items-start justify-between gap-3">
                <dt className="text-neutral-500">{row.label}</dt>
                <dd className="text-right">
                  <span className="font-semibold text-neutral-800">{row.value}</span>
                  {row.hint && <span className="ml-1 text-[10.5px] text-neutral-400">{row.hint}</span>}
                </dd>
              </div>
            ))}
          </dl>
        </Card>
      </div>

      <Card
        className="mt-5"
        title="Grafik per menit"
        description="Arahkan kursor ke grafik untuk melihat nilai tiap menit."
      >
        <div className="grid grid-cols-1 gap-5 xl:grid-cols-2">
          {[...TOKEN_METRICS, ...ACTIVITY_METRICS].map((metric) => (
            <div key={metric.key} className="rounded-xl border border-neutral-100 p-4">
              <div className="mb-2 flex items-center gap-2">
                <span className="inline-block h-2.5 w-2.5 rounded-full" style={dot(metric.color)} />
                <h3 className="text-xs font-semibold text-neutral-700">{metric.label}</h3>
              </div>
              <TokenUsageChart points={series} metric={metric.key} color={metric.color} heightClass="h-56" />
            </div>
          ))}
        </div>
      </Card>

      <Card
        className="mt-5"
        title="Rincian per tool"
        description="Jumlah pemanggilan tool pada jendela waktu ini."
        bodyClassName="px-0 py-0"
      >
        {report && report.toolBreakdown.length > 0 ? (
          <ul className="divide-y divide-neutral-100">
            {report.toolBreakdown.map((tool) => {
              const share =
                report.totalToolCalls > 0 ? (tool.count / report.totalToolCalls) * 100 : 0;
              return (
                <li key={tool.name} className="flex items-center gap-3 px-5 py-2.5">
                  <span className="w-52 shrink-0 truncate font-mono text-[11.5px] text-neutral-700">
                    {tool.name}
                  </span>
                  <div className="min-w-0 flex-1">
                    <ProgressBar value={share} tone="info" />
                  </div>
                  <span className="w-20 shrink-0 text-right text-xs text-neutral-600">
                    <span className="font-semibold">{num(tool.count)}x</span>
                    <span className="ml-1.5 text-[10.5px] text-neutral-400">{pct(share)}</span>
                  </span>
                </li>
              );
            })}
          </ul>
        ) : (
          <EmptyState title="Belum ada tool call pada rentang ini" icon={<IconTool size={26} />} />
        )}
      </Card>

      {report && (
        <p className="mt-5 text-[11px] text-neutral-400">
          Jendela {formatMinuteLabel(report.fromMinute)} sampai {formatMinuteLabel(report.toMinute)} |{' '}
          {derived ? formatDecimal(derived.avgPerActiveMin) : '0'} token per menit aktif.
        </p>
      )}
    </MonitoringLayout>
  );
}
