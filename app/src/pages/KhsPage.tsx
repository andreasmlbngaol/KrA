import { useEffect, useState } from 'react';
import type { ReactNode } from 'react';
import axios from 'axios';
import { PortalLayout } from '../components/PortalLayout';
import { getKhsSummary } from '../api/khs';
import type { KhsSummaryResponse } from '../types/api';
import { titleCase } from '../utils/string';

function gradeBadge(grade: string): string {
  if (grade === 'A' || grade === 'B_PLUS') return 'bg-usu-light text-usu-dark';
  if (grade === 'B' || grade === 'C_PLUS') return 'bg-gold-light text-gold-dark';
  return 'bg-red-100 text-red-700';
}

function Field({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div>
      <dt className="text-[11px] font-semibold uppercase tracking-wide text-neutral-400">{label}</dt>
      <dd className="mt-0.5 text-sm font-medium text-neutral-800">{children}</dd>
    </div>
  );
}

function StatCard({ label, value, highlight }: { label: string; value: ReactNode; highlight?: boolean }) {
  return (
    <div className={`rounded-xl border p-4 ${highlight ? 'border-usu bg-usu text-white shadow-xs' : 'border-neutral-200 bg-white'}`}>
      <div className={`text-2xl font-extrabold leading-none ${highlight ? 'text-white' : 'text-neutral-900'}`}>{value}</div>
      <div className={`mt-1 text-[11px] font-medium uppercase tracking-wide ${highlight ? 'text-white/80' : 'text-neutral-400'}`}>{label}</div>
    </div>
  );
}

export function KhsPage() {
  const [data, setData] = useState<KhsSummaryResponse | null>(null);
  const [selected, setSelected] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let activeFlag = true;
    setLoading(true);
    getKhsSummary()
      .then((res) => {
        if (!activeFlag) return;
        setData(res);
        setSelected(res.semesters.length ? res.semesters[res.semesters.length - 1].semester : null);
        setError(null);
      })
      .catch((err) => {
        if (!activeFlag) return;
        let msg = 'Gagal memuat data KHS.';
        if (axios.isAxiosError(err) && typeof err.response?.data === 'string' && err.response.data) {
          msg = err.response.data;
        }
        setError(msg);
      })
      .finally(() => {
        if (activeFlag) setLoading(false);
      });
    return () => {
      activeFlag = false;
    };
  }, []);

  const current = data?.semesters.find((s) => s.semester === selected) ?? null;
  const latest = data && data.semesters.length ? data.semesters[data.semesters.length - 1] : null;
  const periodLabel = latest
    ? `Semester ${titleCase(String(latest.semesterType))} T.A. ${latest.academicYear}/${latest.academicYear + 1}`
    : undefined;

  return (
    <PortalLayout periodLabel={periodLabel} studentName={data?.name}>
      <div className="mb-4 text-sm text-neutral-500">Beranda &#8250; Informasi KHS</div>

      {loading && <p className="text-neutral-500">Memuat KHS...</p>}
      {error && (
        <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div>
      )}

      {data && !loading && !error && (
        <div className="space-y-6">
          <section className="rounded-2xl border border-neutral-200 bg-white p-5 shadow-xs sm:p-6">
            <div className="flex flex-wrap items-start justify-between gap-5">
              <div className="min-w-0 flex-1">
                <h2 className="mb-4 flex items-center gap-2 text-base font-bold text-neutral-900">
                  <span className="h-4 w-1 rounded-full bg-usu" />Detail Mahasiswa
                </h2>
                <dl className="grid grid-cols-1 gap-x-8 gap-y-4 sm:grid-cols-2">
                  <Field label="Nama">{data.name}</Field>
                  <Field label="NIM">{data.nim}</Field>
                  <Field label="Semester Berjalan">Semester {data.currentSemester}</Field>
                </dl>
              </div>
              <div className="grid w-full grid-cols-2 gap-3 sm:w-auto">
                <StatCard label="IPK" value={data.cumulativeIp.toFixed(2)} highlight />
                <StatCard label="SKS Lulus" value={data.totalPassedSks} />
              </div>
            </div>
          </section>

          {data.semesters.length === 0 ? (
            <section className="rounded-2xl border border-dashed border-neutral-300 bg-neutral-50 px-5 py-10 text-center">
              <p className="text-neutral-600">Belum ada riwayat KHS yang tercatat.</p>
            </section>
          ) : (
            <section className="rounded-2xl border border-neutral-200 bg-white p-5 shadow-xs sm:p-6">
              <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
                <h2 className="flex items-center gap-2 text-base font-bold text-neutral-900">
                  <span className="h-4 w-1 rounded-full bg-usu" />Kartu Hasil Studi
                </h2>
                <select
                  value={selected ?? ''}
                  onChange={(e) => setSelected(Number(e.target.value))}
                  className="rounded-lg border border-neutral-300 bg-white px-3 py-2 text-sm font-medium text-neutral-800 shadow-xs focus:border-usu focus:outline-hidden focus:ring-2 focus:ring-usu/30"
                >
                  {data.semesters.map((s) => (
                    <option key={s.semester} value={s.semester}>
                      Semester {s.semester} &mdash; TA {s.academicYear}/{s.academicYear + 1} ({titleCase(String(s.semesterType))})
                    </option>
                  ))}
                </select>
              </div>

              {current && (
                <>
                  <div className="overflow-x-auto rounded-xl border border-neutral-200">
                    <table className="w-full min-w-[680px] text-sm">
                      <thead>
                        <tr className="bg-neutral-50 text-left text-[11px] font-semibold uppercase tracking-wide text-neutral-500">
                          <th className="px-4 py-3 text-center">No</th>
                          <th className="px-4 py-3">Kode</th>
                          <th className="px-4 py-3">Mata Kuliah</th>
                          <th className="px-4 py-3 text-center">Kelas</th>
                          <th className="px-4 py-3">Tipe</th>
                          <th className="px-4 py-3 text-center">Jenis</th>
                          <th className="px-4 py-3 text-center">SKS</th>
                          <th className="px-4 py-3 text-center">Nilai</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-neutral-100">
                        {current.courses.map((c, i) => (
                          <tr key={c.courseCode} className="transition hover:bg-usu-light/40">
                            <td className="px-4 py-3 text-center text-neutral-500">{i + 1}</td>
                            <td className="px-4 py-3">
                              <span className="rounded-md bg-neutral-100 px-2 py-0.5 font-mono text-xs font-semibold text-neutral-600">{c.courseCode}</span>
                            </td>
                            <td className="px-4 py-3 font-medium text-neutral-800">{c.courseName}</td>
                            <td className="px-4 py-3 text-center text-neutral-700">{c.classCode}</td>
                            <td className="px-4 py-3 text-neutral-700">{c.classType}</td>
                            <td className="px-4 py-3 text-center">
                              <span className="inline-block rounded-full bg-gold-light px-2 py-0.5 text-xs font-semibold text-gold-dark">{c.category}</span>
                            </td>
                            <td className="px-4 py-3 text-center">
                              <span className="inline-flex min-w-[28px] justify-center rounded-full bg-usu-light px-2 py-0.5 text-xs font-bold text-usu-dark">{c.sks}</span>
                            </td>
                            <td className="px-4 py-3 text-center">
                              <span className={`inline-block min-w-[34px] rounded-full px-2 py-0.5 text-[13px] font-bold ${gradeBadge(String(c.grade))}`}>
                                {c.gradeLabel}
                              </span>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>

                  <div className="mt-5 grid grid-cols-1 gap-3 sm:grid-cols-3">
                    <StatCard label="Total Mata Kuliah" value={current.courses.length} />
                    <StatCard label="Total SKS Diambil" value={current.totalSks} />
                    <StatCard label="IP Semester" value={current.ip.toFixed(2)} highlight />
                  </div>
                </>
              )}
            </section>
          )}
        </div>
      )}
    </PortalLayout>
  );
}
