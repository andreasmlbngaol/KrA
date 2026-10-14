import { useEffect, useMemo, useState } from 'react';
import { useAuth } from '../auth/AuthContext';
import { PortalLayout } from '../components/PortalLayout';
import { getKrsDraft } from '../api/krs';
import { getKhsSummary } from '../api/khs';
import type { KhsSummaryResponse, KrsDraftItemDto, KrsDraftResponse } from '../types/api';
import { fmtTime, todayDayKey, weekDates, WEEKDAYS as DAYS } from '../utils/datetime';
import { titleCase } from '../utils/string';

export function DashboardPage() {
  const { nim } = useAuth();
  const [krs, setKrs] = useState<KrsDraftResponse | null>(null);
  const [khs, setKhs] = useState<KhsSummaryResponse | null>(null);
  const [selectedSem, setSelectedSem] = useState<number | null>(null);
  const [activeDay, setActiveDay] = useState<string>(todayDayKey());
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    setLoading(true);
    Promise.all([
      getKrsDraft().catch(() => null),
      getKhsSummary().catch(() => null),
    ])
      .then(([k, h]) => {
        if (!active) return;
        setKrs(k);
        setKhs(h);
        if (h && h.semesters.length) {
          setSelectedSem(h.semesters[h.semesters.length - 1].semester);
        }
        if (!k && !h) setError('Gagal memuat data beranda.');
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, []);

  const dates = useMemo(weekDates, []);

  const scheduleByDay = useMemo(() => {
    const map: Record<string, KrsDraftItemDto[]> = {};
    (krs?.items ?? []).forEach((it) => {
      if (!it.day) return;
      const key = String(it.day).toUpperCase();
      if (!map[key]) map[key] = [];
      map[key].push(it);
    });
    Object.values(map).forEach((list) =>
      list.sort((a, b) => (a.startTime ?? '').localeCompare(b.startTime ?? '')),
    );
    return map;
  }, [krs]);

  const currentSem = khs?.semesters.find((s) => s.semester === selectedSem) ?? null;

  const periodLabel = useMemo(() => {
    if (!khs || !khs.semesters.length) return undefined;
    const s = khs.semesters[khs.semesters.length - 1];
    return `Semester ${titleCase(String(s.semesterType))} T.A. ${s.academicYear}/${s.academicYear + 1}`;
  }, [khs]);

  const daySlots = scheduleByDay[activeDay] ?? [];

  return (
    <PortalLayout periodLabel={periodLabel} studentName={khs?.name}>
      <div className="mb-4 text-sm text-neutral-500">Beranda</div>

      <section className="mb-5 flex items-center gap-4 rounded-2xl border border-neutral-200 bg-white p-4 shadow-sm">
        <div className="flex h-14 w-14 items-center justify-center rounded-xl bg-neutral-200 text-neutral-400">
          <svg viewBox="0 0 24 24" width="30" height="30" fill="none" stroke="currentColor" strokeWidth={1.6}><circle cx="12" cy="8" r="4" /><path d="M4 21c0-4 4-6 8-6s8 2 8 6" /></svg>
        </div>
        <div>
          <h2 className="text-lg font-bold text-neutral-900">{khs?.name ?? 'Mahasiswa'}</h2>
          <div className="mt-1.5 flex flex-wrap gap-2 text-xs">
            <span className="rounded-md border border-neutral-200 bg-neutral-50 px-2.5 py-1 text-neutral-600">NIM: {nim ?? '-'}</span>
            {khs && <span className="rounded-md border border-neutral-200 bg-neutral-50 px-2.5 py-1 text-neutral-600">Semester: {khs.currentSemester}</span>}
            <span className="rounded-md border border-usu/30 bg-usu-light px-2.5 py-1 font-semibold text-usu-dark">KRS</span>
          </div>
        </div>
      </section>

      {loading && <p className="text-neutral-500">Memuat data beranda...</p>}
      {error && !loading && (
        <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div>
      )}

      {!loading && !error && (
        <div className="grid grid-cols-1 gap-5 lg:grid-cols-2">
          <section className="rounded-2xl border border-neutral-200 bg-white p-5 shadow-sm">
            <h3 className="mb-4 text-base font-bold text-neutral-900">Jadwal Kuliah</h3>
            <div className="mb-4 flex flex-wrap gap-2">
              {DAYS.map((d) => {
                const isActive = d.key === activeDay;
                return (
                  <button
                    key={d.key}
                    type="button"
                    onClick={() => setActiveDay(d.key)}
                    className={`min-w-[52px] flex-1 rounded-lg border px-2 py-1.5 text-center transition ${isActive ? 'border-usu bg-usu text-white' : 'border-neutral-200 bg-white hover:border-usu'}`}
                  >
                    <div className="text-[13px] font-semibold">{d.short}</div>
                    <div className={`text-[11px] ${isActive ? 'text-white/85' : 'text-neutral-400'}`}>{dates[d.key]}</div>
                  </button>
                );
              })}
            </div>

            {daySlots.length === 0 ? (
              <div className="py-8 text-center text-sm text-neutral-400">Tidak ada jadwal kuliah</div>
            ) : (
              <ul className="divide-y divide-dashed divide-neutral-200">
                {daySlots.map((s) => (
                  <li key={s.classOfferingId} className="flex gap-3 py-3">
                    <div className="flex items-center gap-1.5 whitespace-nowrap text-[13px] font-semibold text-usu-dark">
                      <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" strokeWidth={2}><circle cx="12" cy="12" r="9" /><path d="M12 7v5l3 2" /></svg>
                      {fmtTime(s.startTime)} - {fmtTime(s.endTime)}
                    </div>
                    <div>
                      <div className="font-semibold text-neutral-800">{s.courseName}</div>
                      <div className="mt-0.5 text-[12.5px] text-neutral-500">
                        {s.courseCode} - {s.classCode}, {s.sks} SKS{s.room ? ` · ${s.room}` : ''}
                      </div>
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </section>

          <section className="rounded-2xl border border-neutral-200 bg-white p-5 shadow-sm">
            <h3 className="mb-4 text-base font-bold text-neutral-900">KHS</h3>
            {!khs || khs.semesters.length === 0 ? (
              <p className="text-sm text-neutral-400">Belum ada data KHS.</p>
            ) : (
              <>
                <div className="mb-3 flex justify-end">
                  <select
                    value={selectedSem ?? ''}
                    onChange={(e) => setSelectedSem(Number(e.target.value))}
                    className="rounded-lg border border-neutral-300 bg-white px-3 py-1.5 text-xs font-medium text-neutral-700 focus:border-usu focus:outline-none"
                  >
                    {khs.semesters.map((s) => (
                      <option key={s.semester} value={s.semester}>
                        Semester {titleCase(String(s.semesterType))} T.A. {s.academicYear}/{s.academicYear + 1}
                      </option>
                    ))}
                  </select>
                </div>
                <table className="w-full border-collapse text-sm">
                  <thead>
                    <tr className="text-left text-[11px] uppercase tracking-wide text-neutral-400">
                      <th className="w-9 border-b border-neutral-200 px-2 py-2 text-center">No</th>
                      <th className="border-b border-neutral-200 px-2 py-2">Mata Kuliah</th>
                      <th className="w-12 border-b border-neutral-200 px-2 py-2 text-center">SKS</th>
                      <th className="w-14 border-b border-neutral-200 px-2 py-2 text-center">Nilai</th>
                    </tr>
                  </thead>
                  <tbody>
                    {currentSem?.courses.map((c, i) => (
                      <tr key={c.courseCode} className="border-b border-neutral-100 last:border-0">
                        <td className="px-2 py-2 text-center text-neutral-500">{i + 1}</td>
                        <td className="px-2 py-2 text-neutral-800">{c.courseName}</td>
                        <td className="px-2 py-2 text-center text-neutral-700">{c.sks}</td>
                        <td className="px-2 py-2 text-center font-semibold text-neutral-700">{c.gradeLabel || '-'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
                <div className="mt-4 space-y-1.5 border-t-2 border-neutral-200 pt-3 text-sm">
                  <div className="flex justify-between"><span className="font-bold">IPK</span><span className="font-bold text-usu-dark">{khs.cumulativeIp.toFixed(2)}</span></div>
                  <div className="flex justify-between"><span className="font-bold">Total SKS Diambil</span><span className="font-bold text-usu-dark">{currentSem?.totalSks ?? '-'}</span></div>
                  <div className="flex justify-between"><span className="font-bold">IP Semester</span><span className="font-bold text-usu-dark">{currentSem ? currentSem.ip.toFixed(2) : '-'}</span></div>
                </div>
              </>
            )}
          </section>
        </div>
      )}
    </PortalLayout>
  );
}
