// Helper umum terkait tanggal & waktu. Dipakai lintas halaman (Dashboard, KRS,
// monitoring token, dll). Semua format memakai locale Indonesia (id-ID).

/** Hari dalam seminggu (urutan Senin–Minggu) beserta label singkat Indonesia. */
export const WEEKDAYS = [
  { key: 'MONDAY', short: 'Sen' },
  { key: 'TUESDAY', short: 'Sel' },
  { key: 'WEDNESDAY', short: 'Rab' },
  { key: 'THURSDAY', short: 'Kam' },
  { key: 'FRIDAY', short: 'Jum' },
  { key: 'SATURDAY', short: 'Sab' },
  { key: 'SUNDAY', short: 'Min' },
] as const;

/** Nama bulan singkat (Indonesia). Index 0 = Januari. */
export const MONTHS_ID = ['Jan', 'Feb', 'Mar', 'Apr', 'Mei', 'Jun', 'Jul', 'Agu', 'Sep', 'Okt', 'Nov', 'Des'];

/** Ambil "HH:MM" dari string waktu (mis. "08:00:00"); kosong/null/undefined -> "". */
export function fmtTime(t: string | null | undefined): string {
  if (!t) return '';
  return t.length > 5 ? t.slice(0, 5) : t;
}

/** Jam:menit lokal (id-ID) dari sebuah ISO datetime; kosong -> "-". */
export function timeOf(iso: string): string {
  if (!iso) return '-';
  return new Date(iso).toLocaleTimeString('id-ID', { hour: '2-digit', minute: '2-digit' });
}

/** Label lengkap "dd Mon HH:MM" dari ISO datetime (mis. untuk tooltip grafik). */
export function formatMinuteLabel(iso: string): string {
  const d = new Date(iso);
  return d.toLocaleString('id-ID', {
    day: '2-digit',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
  });
}

/** Jam:menit lokal (id-ID) dari ISO datetime (mis. untuk tick sumbu grafik). */
export function formatHourTick(iso: string): string {
  const d = new Date(iso);
  return d.toLocaleTimeString('id-ID', { hour: '2-digit', minute: '2-digit' });
}

/** Key hari ini dalam format MONDAY..SUNDAY. */
export function todayDayKey(): string {
  const dow = (new Date().getDay() + 6) % 7;
  return WEEKDAYS[dow].key;
}

/** Map key-hari -> "tanggal Bulan" (mis. "16 Jun") untuk minggu berjalan. */
export function weekDates(): Record<string, string> {
  const now = new Date();
  const dow = (now.getDay() + 6) % 7;
  const monday = new Date(now);
  monday.setDate(now.getDate() - dow);
  const out: Record<string, string> = {};
  WEEKDAYS.forEach((d, i) => {
    const dt = new Date(monday);
    dt.setDate(monday.getDate() + i);
    out[d.key] = `${dt.getDate()} ${MONTHS_ID[dt.getMonth()]}`;
  });
  return out;
}

/** Waktu relatif singkat ("baru saja", "12 menit lalu", "3 jam lalu"). */
export function relativeTime(iso: string): string {
  if (!iso) return '-';
  const diffMs = Date.now() - new Date(iso).getTime();
  const seconds = Math.max(0, Math.round(diffMs / 1000));
  if (seconds < 45) return 'baru saja';
  const minutes = Math.round(seconds / 60);
  if (minutes < 60) return `${minutes} menit lalu`;
  const hours = Math.round(minutes / 60);
  if (hours < 24) return `${hours} jam lalu`;
  const days = Math.round(hours / 24);
  if (days < 30) return `${days} hari lalu`;
  return new Date(iso).toLocaleDateString('id-ID', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  });
}

/** Tanggal + jam lengkap ("17 Okt 2026, 22.14"). */
export function formatDateTime(iso: string): string {
  if (!iso) return '-';
  return new Date(iso).toLocaleString('id-ID', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

/** Durasi milidetik ke teks ringkas. */
export function formatDuration(ms: number): string {
  if (!ms || ms <= 0) return '-';
  if (ms < 1000) return `${Math.round(ms)} ms`;
  return `${(ms / 1000).toFixed(1)} dtk`;
}
