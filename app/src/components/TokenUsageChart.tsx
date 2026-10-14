import { useMemo, useRef, useState } from 'react';
import type { TokenUsagePoint } from '../types/api';
import { formatMinuteLabel, formatHourTick } from '../utils/datetime';
import { compactNumber } from '../utils/number';

export type MetricKey =
  | 'totalTokens'
  | 'inputTokens'
  | 'outputTokens'
  | 'llmRequests'
  | 'toolCalls';

interface Props {
  points: TokenUsagePoint[];
  metric: MetricKey;
  /** Stroke/fill color (hex). */
  color?: string;
  /** Tailwind height class for the chart area (default "h-72"). */
  heightClass?: string;
}

// Fixed internal coordinate system; SVG scales responsively to its container.
const VBW = 1000;
const VBH = 320;
const PAD = { top: 16, right: 16, bottom: 28, left: 56 };
const PLOT_W = VBW - PAD.left - PAD.right;
const PLOT_H = VBH - PAD.top - PAD.bottom;

export function TokenUsageChart({ points, metric, color = '#487629', heightClass = 'h-72' }: Props) {
  const svgRef = useRef<SVGSVGElement | null>(null);
  const [hoverIdx, setHoverIdx] = useState<number | null>(null);

  const n = points.length;

  const { maxVal, xFor, yFor, areaPath, linePath } = useMemo(() => {
    const values = points.map((p) => p[metric] as number);
    const max = Math.max(1, ...values);
    const x = (i: number) =>
      PAD.left + (n <= 1 ? PLOT_W / 2 : (i / (n - 1)) * PLOT_W);
    const y = (v: number) => PAD.top + PLOT_H - (v / max) * PLOT_H;

    let line = '';
    let area = '';
    points.forEach((p, i) => {
      const px = x(i);
      const py = y(p[metric] as number);
      line += `${i === 0 ? 'M' : 'L'}${px.toFixed(1)},${py.toFixed(1)} `;
    });
    if (n > 0) {
      area =
        `M${x(0).toFixed(1)},${(PAD.top + PLOT_H).toFixed(1)} ` +
        points
          .map((p, i) => `L${x(i).toFixed(1)},${y(p[metric] as number).toFixed(1)}`)
          .join(' ') +
        ` L${x(n - 1).toFixed(1)},${(PAD.top + PLOT_H).toFixed(1)} Z`;
    }
    return { maxVal: max, xFor: x, yFor: y, areaPath: area, linePath: line };
  }, [points, metric, n]);

  function handleMove(e: React.MouseEvent<SVGSVGElement>) {
    const svg = svgRef.current;
    if (!svg || n === 0) return;
    const rect = svg.getBoundingClientRect();
    const relX = ((e.clientX - rect.left) / rect.width) * VBW;
    const ratio = (relX - PAD.left) / PLOT_W;
    const idx = Math.round(ratio * (n - 1));
    setHoverIdx(Math.max(0, Math.min(n - 1, idx)));
  }

  const yTicks = useMemo(() => {
    const count = 4;
    return Array.from({ length: count + 1 }, (_, i) => {
      const v = (maxVal / count) * i;
      return { v, y: yFor(v) };
    });
  }, [maxVal, yFor]);

  // ~6 evenly spaced x labels.
  const xTicks = useMemo(() => {
    if (n === 0) return [] as Array<{ i: number; x: number; label: string }>;
    const desired = Math.min(6, n);
    const step = Math.max(1, Math.floor((n - 1) / (desired - 1 || 1)));
    const ticks: Array<{ i: number; x: number; label: string }> = [];
    for (let i = 0; i < n; i += step) {
      ticks.push({ i, x: xFor(i), label: formatHourTick(points[i].minute) });
    }
    return ticks;
  }, [points, n, xFor]);

  const hovered = hoverIdx != null ? points[hoverIdx] : null;
  const hoverX = hoverIdx != null ? xFor(hoverIdx) : 0;
  // Position the tooltip on the left when hovering the right half (avoids clipping).
  const tooltipLeftPct = hoverIdx != null ? (hoverX / VBW) * 100 : 0;
  const tooltipOnRight = tooltipLeftPct > 60;

  if (n === 0) {
    return (
      <div className={`flex ${heightClass} items-center justify-center rounded-xl border border-dashed border-neutral-200 text-sm text-neutral-400`}>
        Belum ada data pada rentang ini.
      </div>
    );
  }

  return (
    <div className="relative w-full">
      <svg
        ref={svgRef}
        viewBox={`0 0 ${VBW} ${VBH}`}
        preserveAspectRatio="none"
        className={`${heightClass} w-full`}
        onMouseMove={handleMove}
        onMouseLeave={() => setHoverIdx(null)}
      >
        {/* horizontal grid + y labels */}
        {yTicks.map((t, i) => (
          <g key={i}>
            <line
              x1={PAD.left}
              x2={VBW - PAD.right}
              y1={t.y}
              y2={t.y}
              stroke="#e5e5e5"
              strokeWidth={1}
            />
            <text x={PAD.left - 8} y={t.y + 4} textAnchor="end" fontSize={13} fill="#9ca3af">
              {compactNumber(Math.round(t.v))}
            </text>
          </g>
        ))}

        {/* area + line */}
        <path d={areaPath} fill={color} fillOpacity={0.12} stroke="none" />
        <path d={linePath} fill="none" stroke={color} strokeWidth={2} vectorEffect="non-scaling-stroke" />

        {/* x ticks */}
        {xTicks.map((t) => (
          <text
            key={t.i}
            x={t.x}
            y={VBH - 8}
            textAnchor="middle"
            fontSize={13}
            fill="#9ca3af"
          >
            {t.label}
          </text>
        ))}

        {/* hover guides */}
        {hoverIdx != null && hovered && (
          <g>
            <line
              x1={hoverX}
              x2={hoverX}
              y1={PAD.top}
              y2={PAD.top + PLOT_H}
              stroke={color}
              strokeOpacity={0.5}
              strokeWidth={1}
              vectorEffect="non-scaling-stroke"
            />
            <circle
              cx={hoverX}
              cy={yFor(hovered[metric] as number)}
              r={4}
              fill={color}
              stroke="#fff"
              strokeWidth={2}
            />
          </g>
        )}
      </svg>

      {/* tooltip */}
      {hoverIdx != null && hovered && (
        <div
          className="pointer-events-none absolute top-2 z-10 w-52 rounded-lg border border-neutral-200 bg-white p-3 text-xs shadow-lg"
          style={
            tooltipOnRight
              ? { right: `${100 - tooltipLeftPct}%`, marginRight: 8 }
              : { left: `${tooltipLeftPct}%`, marginLeft: 8 }
          }
        >
          <div className="mb-1.5 font-semibold text-neutral-700">
            {formatMinuteLabel(hovered.minute)}
          </div>
          <dl className="space-y-1">
            <Row label="Total token" value={hovered.totalTokens.toLocaleString('id-ID')} />
            <Row label="Input" value={hovered.inputTokens.toLocaleString('id-ID')} />
            <Row label="Output" value={hovered.outputTokens.toLocaleString('id-ID')} />
            <Row label="LLM requests" value={`${hovered.llmRequests}`} />
            <Row label="Token/request" value={hovered.avgTokensPerRequest.toLocaleString('id-ID')} />
            <Row label="Tool calls" value={`${hovered.toolCalls}`} />
          </dl>
        </div>
      )}
    </div>
  );
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center justify-between gap-3">
      <dt className="text-neutral-500">{label}</dt>
      <dd className="font-medium text-neutral-800">{value}</dd>
    </div>
  );
}
