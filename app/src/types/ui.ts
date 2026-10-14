/**
 * Tipe khusus front-end (UI) yang TIDAK ada di kontrak API.
 * Semua DTO backend ada di `types/api.ts`.
 */

/** Peran pengirim pesan pada UI chat. */
export type ChatRole = 'user' | 'assistant';

/** Satu gelembung pesan di layar chat. */
export interface ChatMessageItem {
  id: string;
  role: ChatRole;
  content: string;
  isError?: boolean;
}
