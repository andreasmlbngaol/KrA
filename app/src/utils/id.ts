// Helper umum untuk pembuatan id.

/** Buat UUID acak (mis. untuk key elemen / pesan chat sementara). */
export function createId(): string {
  return crypto.randomUUID();
}
