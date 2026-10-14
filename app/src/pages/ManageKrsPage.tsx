import { useEffect, useState } from 'react';
import type { ReactNode } from 'react';
import { useLocation } from 'react-router-dom';
import { gooeyToast } from 'goey-toast';
import { useAuth } from '../auth/AuthContext';
import { PortalLayout } from '../components/PortalLayout';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { deleteKrsDraftItem, getKrsDraft } from '../api/krs';
import { getKhsSummary } from '../api/khs';
import type { KhsSummaryResponse, KrsDraftItemDto, KrsDraftResponse } from '../types/api';
import { fmtTime } from '../utils/datetime';
import { titleCase } from '../utils/string';
import { errorMessage } from '../utils/error';

const MAX_SKS = 24;

const DAY_LABEL: Record<string, string> = {
  MONDAY: 'Senin',
  TUESDAY: 'Selasa',
  WEDNESDAY: 'Rabu',
  THURSDAY: 'Kamis',
  FRIDAY: 'Jumat',
  SATURDAY: 'Sabtu',
  SUNDAY: 'Minggu',
};

function jadwalText(item: KrsDraftItemDto): string | null {
  if (!item.day && !item.startTime) return null;
  const day = item.day ? DAY_LABEL[String(item.day)] ?? String(item.day) : '';
  const time = item.startTime && item.endTime ? `${fmtTime(item.startTime)}–${fmtTime(item.endTime)}` : '';
  return [day, time].filter(Boolean).join(', ');
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
    <div className={`rounded-xl border p-4 ${highlight ? 'border-usu bg-usu text-white shadow-sm' : 'border-neutral-200 bg-white'}`}>
      <div className={`text-2xl font-extrabold leading-none ${highlight ? 'text-white' : 'text-neutral-900'}`}>{value}</div>
      <div className={`mt-1 text-[11px] font-medium uppercase tracking-wide ${highlight ? 'text-white/80' : 'text-neutral-400'}`}>{label}</div>
    </div>
  );
}

export function ManageKrsPage() {
  const { nim: authNim } = useAuth();
  const location = useLocation();
  const [data, setData] = useState<KrsDraftResponse | null>(null);
  const [khs, setKhs] = useState<KhsSummaryResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [deletingId, setDeletingId] = useState<string | null>(null);
  const [pendingDelete, setPendingDelete] = useState<KrsDraftItemDto | null>(null);
  const [showSubmitted, setShowSubmitted] = useState(false);

  const load = async () => {
    setLoading(true);
    try {
      const [draft, summary] = await Promise.all([
        getKrsDraft(),
        getKhsSummary().catch(() => null),
      ]);
      setData(draft);
      setKhs(summary);
      setError(null);
    } catch (err) {
      setError(errorMessage(err, 'Gagal memuat draft KRS.'));
    } finally {
      setLoading(false);
    }
  };

  // Muat ulang data tiap kali kita (kembali) bernavigasi ke halaman ini.
  // location.key berubah pada SETIAP navigasi — termasuk saat menekan "Buka
  // Halaman Kelola KRS" dari chat walau sudah berada di halaman ini — sehingga
  // info terbaru ikut ter-update tanpa perlu reload (animasi overlay tetap utuh).
  useEffect(() => {
    void load();
  }, [location.key]);

  const confirmDelete = async () => {
    const item = pendingDelete;
    if (!item) return;
    setPendingDelete(null);
    setDeletingId(item.classOfferingId);
    setError(null);
    const task = deleteKrsDraftItem(item.classOfferingId);
    gooeyToast.promise(task, {
      loading: `Menghapus ${item.courseCode}...`,
      success: `${item.courseCode} dihapus dari KRS.`,
      error: (err) => errorMessage(err, 'Gagal menghapus mata kuliah.'),
    });
    try {
      await task;
      await load();
    } catch {
      // pesan kesalahan sudah ditampilkan lewat toast di atas
    } finally {
      setDeletingId(null);
    }
  };

  const items = data?.items ?? [];
  const hasItems = items.length > 0;
  const latest = khs && khs.semesters.length ? khs.semesters[khs.semesters.length - 1] : null;
  const periodLabel = latest
    ? `Semester ${titleCase(String(latest.semesterType))} T.A. ${latest.academicYear}/${latest.academicYear + 1}`
    : undefined;
  const nim = khs?.nim ?? authNim ?? '-';

  return (
    <PortalLayout periodLabel={periodLabel} studentName={khs?.name}>
      <div className="mb-4 text-sm text-neutral-500">Beranda &#8250; Kelola KRS</div>

      {loading && <p className="text-neutral-500">Memuat draft KRS...</p>}
      {error && (
        <div className="mb-4 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div>
      )}

      {data && !loading && (
        <div className="space-y-6">
          <section className="rounded-2xl border border-neutral-200 bg-white p-5 shadow-sm sm:p-6">
            <h2 className="mb-4 flex items-center gap-2 text-base font-bold text-neutral-900">
              <span className="h-4 w-1 rounded-full bg-usu" />Detail Mahasiswa
            </h2>
            <dl className="grid grid-cols-1 gap-x-8 gap-y-4 sm:grid-cols-2 lg:grid-cols-3">
              <Field label="Nama">{khs?.name ?? 'Mahasiswa'}</Field>
              <Field label="NIM">{nim}</Field>
              <Field label="Semester">{periodLabel ?? `Semester ${data.semester}`}</Field>
              <Field label="Maksimal SKS">{MAX_SKS}</Field>
              <Field label="Status KRS">
                <span className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-semibold ${data.exists ? 'bg-usu-light text-usu-dark' : 'bg-neutral-100 text-neutral-500'}`}>
                  <span className={`h-1.5 w-1.5 rounded-full ${data.exists ? 'bg-usu' : 'bg-neutral-400'}`} />
                  {data.exists ? 'KRS Tersusun' : 'Belum disusun'}
                </span>
              </Field>
            </dl>
          </section>

          <section className="rounded-2xl border border-neutral-200 bg-white p-5 shadow-sm sm:p-6">
            <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
              <h2 className="flex items-center gap-2 text-base font-bold text-neutral-900">
                <span className="h-4 w-1 rounded-full bg-usu" />Mata Kuliah Diambil
              </h2>
              <span className="rounded-full bg-neutral-100 px-3 py-1 text-xs font-medium text-neutral-500">{items.length} mata kuliah</span>
            </div>

            {!hasItems ? (
              <div className="rounded-xl border border-dashed border-neutral-300 bg-neutral-50 px-5 py-10 text-center">
                <p className="text-neutral-600">Belum ada KRS yang tersusun untuk semester ini.</p>
                <p className="mt-1 text-sm text-neutral-400">Gunakan Chat Assistant (tombol di pojok kanan bawah) untuk menyusun KRS-mu dulu.</p>
              </div>
            ) : (
              <>
                <div className="overflow-x-auto rounded-xl border border-neutral-200">
                  <table className="w-full min-w-[920px] text-sm">
                    <thead>
                      <tr className="bg-neutral-50 text-left text-[11px] font-semibold uppercase tracking-wide text-neutral-500">
                        <th className="px-3 py-3 text-center">No</th>
                        <th className="px-3 py-3">Kode</th>
                        <th className="px-3 py-3">Mata Kuliah</th>
                        <th className="px-3 py-3 text-center">SKS</th>
                        <th className="px-3 py-3 text-center">Semester</th>
                        <th className="px-3 py-3 text-center">Kelas</th>
                        <th className="px-3 py-3">Ruangan</th>
                        <th className="px-3 py-3">Jadwal</th>
                        <th className="px-3 py-3">Dosen</th>
                        <th className="px-3 py-3 text-center">Jenis</th>
                        <th className="px-3 py-3 text-center">Aksi</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-neutral-100">
                      {items.map((item, i) => {
                        const jadwal = jadwalText(item);
                        return (
                          <tr key={item.classOfferingId} className="align-top transition hover:bg-usu-light/40">
                            <td className="px-3 py-3 text-center text-neutral-500">{i + 1}</td>
                            <td className="px-3 py-3">
                              <span className="rounded-md bg-neutral-100 px-2 py-0.5 font-mono text-xs font-semibold text-neutral-600">{item.courseCode}</span>
                            </td>
                            <td className="px-3 py-3 font-medium text-neutral-800">{item.courseName}</td>
                            <td className="px-3 py-3 text-center">
                              <span className="inline-flex min-w-[28px] justify-center rounded-full bg-usu-light px-2 py-0.5 text-xs font-bold text-usu-dark">{item.sks}</span>
                            </td>
                            <td className="px-3 py-3 text-center text-neutral-700">{item.semester || '-'}</td>
                            <td className="px-3 py-3 text-center text-neutral-700">{item.classCode}</td>
                            <td className="px-3 py-3 text-neutral-700">{item.room ?? <span className="text-neutral-400">-</span>}</td>
                            <td className="px-3 py-3 text-neutral-700">
                              {jadwal ?? <span className="italic text-neutral-400">Belum diatur</span>}
                            </td>
                            <td className="px-3 py-3 text-neutral-700">
                              {item.lecturers.length ? (
                                <div className="space-y-0.5">
                                  {item.lecturers.map((dosen) => (
                                    <div key={dosen}>{dosen}</div>
                                  ))}
                                </div>
                              ) : (
                                <span className="text-neutral-400">-</span>
                              )}
                            </td>
                            <td className="px-3 py-3 text-center">
                              <span className="inline-block rounded-full bg-gold-light px-2 py-0.5 text-xs font-semibold text-gold-dark">{item.category}</span>
                            </td>
                            <td className="px-3 py-3 text-center">
                              <button
                                type="button"
                                onClick={() => setPendingDelete(item)}
                                disabled={deletingId === item.classOfferingId}
                                title="Hapus dari KRS"
                                className="inline-flex items-center gap-1 rounded-lg border border-red-200 px-2.5 py-1.5 text-xs font-semibold text-red-600 transition hover:bg-red-50 disabled:opacity-50"
                              >
                                <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" strokeWidth={1.8} strokeLinecap="round" strokeLinejoin="round"><path d="M4 7h16M9 7V5h6v2M6 7l1 13h10l1-13M10 11v6M14 11v6" /></svg>
                                {deletingId === item.classOfferingId ? '...' : 'Hapus'}
                              </button>
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>

                <div className="mt-5 grid grid-cols-2 gap-3 sm:grid-cols-3">
                  <StatCard label="Total Mata Kuliah" value={items.length} />
                  <StatCard label="Total SKS Diambil" value={data.totalSks} highlight />
                  <StatCard label="Maksimal SKS" value={MAX_SKS} />
                </div>

                <div className="mt-5 flex flex-wrap justify-end gap-3">
                  <button
                    type="button"
                    onClick={() => window.print()}
                    className="rounded-lg border border-neutral-300 px-5 py-2.5 font-semibold text-neutral-700 transition hover:bg-neutral-50"
                  >
                    Cetak KRS
                  </button>
                  <button
                    type="button"
                    onClick={() => setShowSubmitted(true)}
                    className="rounded-lg bg-usu px-5 py-2.5 font-semibold text-white shadow-sm transition hover:bg-usu-dark"
                  >
                    Ajukan ke Dosen PA
                  </button>
                </div>
              </>
            )}
          </section>
        </div>
      )}

      <ConfirmDialog
        open={!!pendingDelete}
        tone="danger"
        title="Hapus mata kuliah?"
        description={
          pendingDelete
            ? `${pendingDelete.courseCode} - ${pendingDelete.courseName} (${pendingDelete.classCode}) akan dihapus dari KRS.`
            : undefined
        }
        confirmLabel="Hapus"
        cancelLabel="Batal"
        onConfirm={confirmDelete}
        onCancel={() => setPendingDelete(null)}
      />

      {showSubmitted && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-4">
          <div className="w-full max-w-sm rounded-2xl border-t-4 border-gold bg-white p-6 text-center shadow-xl">
            <div className="mx-auto mb-3 flex h-14 w-14 items-center justify-center rounded-full bg-usu-light text-3xl">
              &#9989;
            </div>
            <h3 className="text-lg font-bold text-neutral-900">Berhasil Diajukan</h3>
            <p className="mt-2 text-sm text-neutral-500">
              KRS-mu sudah berhasil diajukan ke Dosen Pembimbing Akademik untuk ditinjau.
            </p>
            <button
              type="button"
              onClick={() => setShowSubmitted(false)}
              className="mt-5 w-full rounded-lg bg-usu px-4 py-2.5 font-semibold text-white transition hover:bg-usu-dark"
            >
              Tutup
            </button>
          </div>
        </div>
      )}
    </PortalLayout>
  );
}
