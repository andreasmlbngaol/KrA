// Helper umum untuk manipulasi string.

/** Kapitalkan huruf pertama, sisanya huruf kecil (mis. "REGULER" -> "Reguler"). */
export function titleCase(s: string): string {
  return s ? s.charAt(0).toUpperCase() + s.slice(1).toLowerCase() : s;
}

/** Potong string bila melebihi panjang maksimum dan tambahkan elipsis. */
export function truncate(value: string, max = 20): string {
  return value.length > max ? value.slice(0, max) + '...' : value;
}
