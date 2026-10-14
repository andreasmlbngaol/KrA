import { http } from './http';
import type { AddKhsRequest, KhsEntryDto, KhsSummaryResponse } from '../types/api';

/** Ringkasan KHS (NIM diambil dari token di server). */
export async function getKhsSummary(): Promise<KhsSummaryResponse> {
  const { data } = await http.get<KhsSummaryResponse>('/khs');
  return data;
}

/** Daftar datar entri KHS untuk halaman Kelola KRS. */
export async function getKhsEntries(): Promise<KhsEntryDto[]> {
  const { data } = await http.get<KhsEntryDto[]>('/khs/entries');
  return data;
}

/** Menambah satu mata kuliah yang sudah diambil. */
export async function addKhsEntry(body: AddKhsRequest): Promise<void> {
  await http.post('/khs/entries', body);
}

/** Menghapus satu entri KHS berdasarkan kode matkul + semester. */
export async function deleteKhsEntry(courseCode: string, semester: number): Promise<void> {
  await http.delete(`/khs/entries/${encodeURIComponent(courseCode)}/${semester}`);
}
