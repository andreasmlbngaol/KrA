import { useState } from 'react';
import {
  fetchEvaluationCases,
  fetchEvaluationRuns,
  fetchEvaluationSummary,
  fetchSurveyResponses,
  fetchSurveySummary,
} from '../api/monitoring';
import { MonitoringLayout } from '../components/monitoring/MonitoringLayout';
import {
  Badge,
  Card,
  EmptyState,
  PageHeader,
  Select,
  StatTile,
  StatusBadge,
  ToolbarButton,
} from '../components/monitoring/primitives';
import { IconCheck, IconEvaluation, IconRefresh } from '../components/monitoring/icons';
import { useResource } from '../components/monitoring/useResource';
import { formatDateTime } from '../utils/datetime';
import { formatDecimal, formatInt as num, formatPercent as pct } from '../utils/number';

const REFRESH_MS = 60_000;

const LIMIT_OPTIONS = [
  { value: 50, label: '50 data' },
  { value: 100, label: '100 data' },
  { value: 200, label: '200 data' },
  { value: 500, label: '500 data' },
];

type Tab = 'runs' | 'survey';

export function EvaluationPage() {
  const [tab, setTab] = useState<Tab>('runs');
  const [limit, setLimit] = useState(100);
  const [openRun, setOpenRun] = useState<string | null>(null);
  const [openSurvey, setOpenSurvey] = useState<string | null>(null);

  const resource = useResource(
    () =>
      Promise.all([
        fetchEvaluationSummary(),
        fetchEvaluationCases(),
        fetchEvaluationRuns(limit),
        fetchSurveySummary(),
        fetchSurveyResponses(limit),
      ]).then(([summary, cases, runs, surveySummary, responses]) => ({
        summary,
        cases,
        runs,
        surveySummary,
        responses,
      })),
    [limit],
    REFRESH_MS,
  );

  const d = resource.data;
  const summary = d?.summary;
  const runs = d?.runs.runs ?? [];
  const cases = d?.cases.cases ?? [];
  const responses = d?.responses.responses ?? [];

  return (
    <MonitoringLayout>
      <PageHeader
        eyebrow="Monitoring"
        title="Evaluasi Agent"
        description="Akurasi solusi terhadap skenario ground truth, kualitas pemanggilan tool, dan hasil kuesioner TAM/SUS."
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
          label="Akurasi solusi"
          value={pct((summary?.solutionAccuracy ?? 0) * 100)}
          hint={`${num(summary?.correctRuns ?? 0)} dari ${num(summary?.totalRuns ?? 0)} run`}
          icon={<IconCheck size={17} />}
          tone="success"
        />
        <StatTile label="Tool precision" value={pct((summary?.avgPrecision ?? 0) * 100)} />
        <StatTile label="Tool recall" value={pct((summary?.avgRecall ?? 0) * 100)} />
        <StatTile label="Tool F1" value={pct((summary?.avgF1 ?? 0) * 100)} />
      </div>

      <div className="mt-6 inline-flex rounded-full border border-neutral-200 bg-white p-1">
        {(
          [
            { key: 'runs', label: `Evaluasi solusi (${num(runs.length)})` },
            { key: 'survey', label: `Kuesioner (${num(responses.length)})` },
          ] as Array<{ key: Tab; label: string }>
        ).map((item) => (
          <button
            key={item.key}
            type="button"
            onClick={() => setTab(item.key)}
            className={`rounded-full px-4 py-1.5 text-xs font-semibold transition ${
              tab === item.key ? 'bg-usu text-white' : 'text-neutral-500 hover:text-neutral-800'
            }`}
          >
            {item.label}
          </button>
        ))}
      </div>

      {tab === 'runs' ? (
        <>
          <Card
            className="mt-4"
            title="Skenario ground truth"
            description="Case yang dipakai sebagai pembanding saat mengukur jawaban agent."
            bodyClassName="px-0 py-0"
          >
            {cases.length === 0 ? (
              <EmptyState
                title="Belum ada case evaluasi"
                description="Tambahkan lewat POST /api/evaluation/cases sebelum menjalankan evaluasi."
                icon={<IconEvaluation size={26} />}
              />
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full min-w-[900px] text-xs">
                  <thead>
                    <tr className="border-b border-neutral-100 text-left text-[10px] uppercase tracking-[0.08em] text-neutral-400">
                      <th className="px-5 py-2.5 font-semibold">Kode</th>
                      <th className="px-3 py-2.5 font-semibold">NIM</th>
                      <th className="px-3 py-2.5 text-center font-semibold">Sem</th>
                      <th className="px-3 py-2.5 font-semibold">Deskripsi</th>
                      <th className="px-3 py-2.5 text-center font-semibold">Layak</th>
                      <th className="px-3 py-2.5 text-center font-semibold">Rentang SKS</th>
                      <th className="px-3 py-2.5 font-semibold">Tool diharapkan</th>
                      <th className="px-3 py-2.5 font-semibold">Matkul diharapkan</th>
                      <th className="px-5 py-2.5 font-semibold">Dibuat</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-neutral-100">
                    {cases.map((item) => (
                      <tr key={item.code} className="transition hover:bg-neutral-50">
                        <td className="px-5 py-2 font-mono text-[11px] font-semibold text-neutral-700">
                          {item.code}
                        </td>
                        <td className="px-3 py-2 font-mono text-[10.5px] text-neutral-600">{item.nim}</td>
                        <td className="px-3 py-2 text-center text-neutral-500">{item.semester}</td>
                        <td className="max-w-[260px] px-3 py-2 text-neutral-600">{item.description}</td>
                        <td className="px-3 py-2 text-center">
                          {item.expectedFeasible ? (
                            <Badge tone="success">ya</Badge>
                          ) : (
                            <Badge tone="danger">tidak</Badge>
                          )}
                        </td>
                        <td className="px-3 py-2 text-center text-neutral-500">
                          {item.expectedMinSks ?? '-'} - {item.expectedMaxSks ?? '-'}
                        </td>
                        <td className="px-3 py-2">
                          <div className="flex flex-wrap gap-1">
                            {item.expectedTools.length === 0 ? (
                              <span className="text-neutral-300">-</span>
                            ) : (
                              item.expectedTools.map((tool) => (
                                <Badge key={tool} tone="info">
                                  {tool}
                                </Badge>
                              ))
                            )}
                          </div>
                        </td>
                        <td className="px-3 py-2">
                          <div className="flex flex-wrap gap-1">
                            {item.expectedCourseCodes.length === 0 ? (
                              <span className="text-neutral-300">-</span>
                            ) : (
                              item.expectedCourseCodes.map((code) => (
                                <Badge key={code} tone="neutral">
                                  {code}
                                </Badge>
                              ))
                            )}
                          </div>
                        </td>
                        <td className="whitespace-nowrap px-5 py-2 text-[10.5px] text-neutral-400">
                          {item.createdAt ? formatDateTime(item.createdAt) : '-'}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </Card>

          <Card
            className="mt-5"
            title="Riwayat run evaluasi"
            description="Setiap run membandingkan satu jawaban agent dengan skenarionya."
            bodyClassName="px-0 py-0"
          >
            {runs.length === 0 ? (
              <EmptyState
                title="Belum ada run evaluasi"
                description="Jalankan POST /api/evaluation/cases/{code}/run setelah agent menjawab."
                icon={<IconEvaluation size={26} />}
              />
            ) : (
              <ul className="divide-y divide-neutral-100">
                {runs.map((run) => {
                  const id = `${run.caseCode}-${run.timestamp}`;
                  const open = openRun === id;
                  return (
                    <li key={id}>
                      <button
                        type="button"
                        onClick={() => setOpenRun(open ? null : id)}
                        aria-expanded={open}
                        className="flex w-full flex-wrap items-center gap-x-3 gap-y-1.5 px-5 py-3 text-left transition hover:bg-neutral-50"
                      >
                        <span className="w-32 shrink-0 font-mono text-[11px] font-semibold text-neutral-700">
                          {run.caseCode}
                        </span>
                        <StatusBadge status={run.solutionCorrect ? 'OK' : 'ERROR'} />
                        <span className="text-[11px] text-neutral-500">
                          {run.feasible ? 'layak' : 'tidak layak'} | {run.totalSks} SKS
                        </span>
                        <span className="ml-auto flex items-center gap-3 text-[10.5px] text-neutral-500">
                          <span>P {pct(run.precision * 100)}</span>
                          <span>R {pct(run.recall * 100)}</span>
                          <span>F1 {pct(run.f1 * 100)}</span>
                          <span className="hidden w-32 text-right text-neutral-400 sm:inline">
                            {formatDateTime(run.timestamp)}
                          </span>
                        </span>
                      </button>
                      {open && (
                        <div className="space-y-3 border-t border-neutral-100 bg-neutral-50 px-5 py-3 text-[11px]">
                          <div className="grid gap-3 sm:grid-cols-2">
                            <div>
                              <div className="mb-1 font-semibold uppercase tracking-wide text-neutral-400">
                                Tool diharapkan
                              </div>
                              <div className="flex flex-wrap gap-1">
                                {run.expectedTools.length === 0 ? (
                                  <span className="text-neutral-400">-</span>
                                ) : (
                                  run.expectedTools.map((tool) => (
                                    <Badge key={tool} tone="info">
                                      {tool}
                                    </Badge>
                                  ))
                                )}
                              </div>
                            </div>
                            <div>
                              <div className="mb-1 font-semibold uppercase tracking-wide text-neutral-400">
                                Tool yang dipakai
                              </div>
                              <div className="flex flex-wrap gap-1">
                                {run.actualTools.length === 0 ? (
                                  <span className="text-neutral-400">-</span>
                                ) : (
                                  run.actualTools.map((tool) => (
                                    <Badge key={tool} tone="neutral">
                                      {tool}
                                    </Badge>
                                  ))
                                )}
                              </div>
                            </div>
                          </div>
                          <div className="flex flex-wrap gap-x-4 gap-y-1 text-neutral-500">
                            <span>chat {run.chatId?.slice(0, 8) ?? '-'}</span>
                            <span>
                              precision {formatDecimal(run.precision)} | recall{' '}
                              {formatDecimal(run.recall)} | F1 {formatDecimal(run.f1)}
                            </span>
                          </div>
                          {run.notes && (
                            <p className="rounded-xl bg-white p-2.5 text-neutral-600 ring-1 ring-neutral-200">
                              {run.notes}
                            </p>
                          )}
                        </div>
                      )}
                    </li>
                  );
                })}
              </ul>
            )}
          </Card>
        </>
      ) : (
        <>
          <div className="mt-4 grid grid-cols-1 gap-3 sm:grid-cols-2">
            {(d?.surveySummary.instruments ?? []).map((instrument) => (
              <Card
                key={instrument.instrument}
                title={instrument.instrument === 'SUS' ? 'System Usability Scale' : 'Technology Acceptance Model'}
                description={
                  instrument.instrument === 'SUS'
                    ? 'Skor 0 sampai 100, makin tinggi makin baik.'
                    : 'Rata-rata perceived usefulness dan perceived ease of use.'
                }
              >
                <div className="flex items-end justify-between">
                  <div>
                    <div className="text-3xl font-semibold text-neutral-900">
                      {formatDecimal(instrument.avgScore)}
                    </div>
                    <div className="text-[11px] text-neutral-400">
                      {num(instrument.responses)} responden
                    </div>
                  </div>
                  <Badge tone="info">{instrument.instrument}</Badge>
                </div>
              </Card>
            ))}
            {(d?.surveySummary.instruments.length ?? 0) === 0 && (
              <Card className="sm:col-span-2">
                <EmptyState title="Belum ada instrumen" icon={<IconEvaluation size={26} />} />
              </Card>
            )}
          </div>

          <Card
            className="mt-5"
            title="Respons kuesioner"
            description="Setiap jawaban per item, dikelompokkan per responden."
            bodyClassName="px-0 py-0"
          >
            {responses.length === 0 ? (
              <EmptyState
                title="Belum ada respons"
                description="Kirim kuesioner lewat POST /api/evaluation/survey."
                icon={<IconEvaluation size={26} />}
              />
            ) : (
              <ul className="divide-y divide-neutral-100">
                {responses.map((response) => {
                  const id = `${response.nim}-${response.submittedAt}`;
                  const open = openSurvey === id;
                  return (
                    <li key={id}>
                      <button
                        type="button"
                        onClick={() => setOpenSurvey(open ? null : id)}
                        aria-expanded={open}
                        className="flex w-full flex-wrap items-center gap-x-3 gap-y-1.5 px-5 py-3 text-left transition hover:bg-neutral-50"
                      >
                        <span className="w-28 shrink-0 font-mono text-[11px] font-semibold text-neutral-700">
                          {response.nim}
                        </span>
                        <Badge tone={response.instrument === 'SUS' ? 'info' : 'success'}>
                          {response.instrument}
                        </Badge>
                        <span className="text-[11px] text-neutral-500">
                          skor {formatDecimal(response.score)}
                        </span>
                        <span className="ml-auto text-[10.5px] text-neutral-400">
                          {formatDateTime(response.submittedAt)}
                        </span>
                      </button>
                      {open && (
                        <div className="space-y-2 border-t border-neutral-100 bg-neutral-50 px-5 py-3">
                          <div className="flex flex-wrap gap-2 text-[10.5px] text-neutral-500">
                            {response.susScore != null && (
                              <span>Skor SUS {formatDecimal(response.susScore)}</span>
                            )}
                            {response.puScore != null && (
                              <span>PU {formatDecimal(response.puScore)}</span>
                            )}
                            {response.peouScore != null && (
                              <span>PEOU {formatDecimal(response.peouScore)}</span>
                            )}
                          </div>
                          <div className="flex flex-wrap gap-1.5">
                          {response.answers.map((answer) => (
                            <span
                              key={answer.itemCode}
                              className="rounded-full bg-white px-2.5 py-1 text-[10.5px] text-neutral-600 ring-1 ring-neutral-200"
                            >
                              <span className="font-mono font-semibold">{answer.itemCode}</span>
                              <span className="ml-1.5 text-neutral-400">{answer.likert}</span>
                            </span>
                          ))}
                          </div>
                        </div>
                      )}
                    </li>
                  );
                })}
              </ul>
            )}
          </Card>
        </>
      )}

      <p className="mt-5 text-[11px] text-neutral-400">
        Data berasal dari tabel evaluation_case, evaluation_run, survey_response, dan survey_answer.
      </p>
    </MonitoringLayout>
  );
}
