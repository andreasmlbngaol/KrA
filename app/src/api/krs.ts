import { http } from './http';
import type { KrsDraftResponse } from '../types/api';

/** Ambil draft KRS semester berjalan (NIM & semester ditentukan server dari token). */
export async function getKrsDraft(): Promise<KrsDraftResponse> {
  const { data } = await http.get<KrsDraftResponse>('/krs/draft');
  return data;
}

/** Hapus satu matkul dari draft KRS semester berjalan. */
export async function deleteKrsDraftItem(classOfferingId: string): Promise<void> {
  await http.delete(`/krs/draft/${encodeURIComponent(classOfferingId)}`);
}
