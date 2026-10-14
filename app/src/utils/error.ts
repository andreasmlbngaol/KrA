// Helper umum untuk menangani error.
import axios from 'axios';

/**
 * Ambil pesan error yang ramah ditampilkan. Bila error berasal dari axios dan
 * body response berupa string (konvensi backend kita), pakai itu; jika tidak,
 * pakai pesan fallback.
 */
export function errorMessage(err: unknown, fallback: string): string {
  if (axios.isAxiosError(err) && typeof err.response?.data === 'string' && err.response.data) {
    return err.response.data;
  }
  return fallback;
}
