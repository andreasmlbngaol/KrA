// Helper umum untuk format angka (locale Indonesia / id-ID).

const nf = new Intl.NumberFormat('id-ID');

/** Bulatkan ke bilangan bulat lalu format dengan pemisah ribuan (id-ID). */
export function formatInt(n: number): string {
  return nf.format(Math.round(n));
}

/** Format angka dengan maksimal 1 angka di belakang koma (id-ID). */
export function formatDecimal(n: number): string {
  return n.toLocaleString('id-ID', { maximumFractionDigits: 1 });
}

/** Format persentase dengan maksimal 1 desimal (mis. "12,3%"). */
export function formatPercent(n: number): string {
  return `${formatDecimal(n)}%`;
}

/** Angka ringkas untuk label grafik: 1.2k, 3.4M. */
export function compactNumber(n: number): string {
  if (n >= 1_000_000) return `${(n / 1_000_000).toFixed(1)}M`;
  if (n >= 1_000) return `${(n / 1_000).toFixed(1)}k`;
  return `${n}`;
}
