import { http, API_BASE_URL, refreshAccessToken } from "./http";
import { getAccessToken } from "../auth/tokenStorage";
import type { ChatRequest } from "../types/api";

/**
 * Wire shape of one NDJSON line from /agent/chat/stream. The web app parses the stream
 * by hand (not via the Kotlin serializer), so the discriminator arrives as a plain string.
 */
type ChatStreamWire = {
  type: "step" | "draftSaved" | "final" | "error" | "ping";
  label?: string;
  content?: string;
};

/**
 * Sends a chat message. The backend returns the assistant reply as
 * plain-text Markdown (text/plain), so we keep the raw string untouched.
 */
export async function sendChatMessage(
  sessionId: string,
  message: string,
): Promise<string> {
  const body: ChatRequest = { sessionId, message };
  const { data } = await http.post<string>("/agent/chat", body, {
    responseType: "text",
    transformResponse: [(d) => d as string],
  });
  return data;
}

/** Handler peristiwa selama streaming balasan chat. */
export interface ChatStreamHandlers {
  /** Dipanggil tiap agent mulai menjalankan satu langkah/tool (label sudah ramah). */
  onStep: (label: string) => void;
  /** Dipanggil sekali dengan balasan akhir (Markdown). */
  onFinal: (content: string) => void;
  /** Dipanggil sekali bila terjadi kesalahan; pesan sudah ramah untuk ditampilkan. */
  onError: (message: string) => void;
  /** Dipanggil bila backend mengonfirmasi draft KRS berhasil tersimpan (lolos validasi). */
  onDraftSaved?: () => void;
}

/**
 * Mengirim pesan chat lewat endpoint streaming (NDJSON). Setiap baris adalah satu
 * event JSON: "step" (langkah/tool dimulai), "final" (balasan akhir), atau "error".
 *
 * Memakai fetch + ReadableStream (bukan EventSource) agar bisa menyertakan header
 * Authorization Bearer.
 */
export async function streamChatMessage(
  sessionId: string,
  message: string,
  handlers: ChatStreamHandlers,
): Promise<void> {
  const body: ChatRequest = { sessionId, message };

  // Satu kali percobaan fetch dengan access token terkini.
  const attempt = (accessToken: string | undefined): Promise<Response> =>
    fetch(`${API_BASE_URL}/agent/chat/stream`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
      },
      body: JSON.stringify(body),
    });

  let res: Response;
  try {
    res = await attempt(getAccessToken() ?? undefined);

    // Endpoint streaming memakai fetch mentah (perlu agar header Authorization
    // bisa disertakan pada ReadableStream), jadi ia TIDAK lewat interceptor axios.
    // Maka tangani 401 di sini: segarkan access token lalu coba SEKALI lagi —
    // menyamai perilaku auto-refresh milik http (axios).
    if (res.status === 401) {
      try {
        const newToken = await refreshAccessToken();
        res = await attempt(newToken);
      } catch {
        handlers.onError("Sesimu sudah berakhir. Silakan masuk kembali.");
        return;
      }
    }
  } catch {
    handlers.onError(
      "Tidak dapat terhubung ke server. Periksa koneksimu lalu coba lagi.",
    );
    return;
  }

  if (!res.ok || !res.body) {
    let msg = "Maaf, terjadi kesalahan saat menghubungi server.";
    try {
      const text = await res.text();
      if (text) msg = text;
    } catch {
      /* abaikan: pakai pesan default */
    }
    handlers.onError(msg);
    return;
  }

  let finalEmitted = false;
  const handleLine = (line: string) => {
    const trimmed = line.trim();
    if (!trimmed) return;
    let evt: ChatStreamWire;
    try {
      evt = JSON.parse(trimmed) as ChatStreamWire;
    } catch {
      return;
    }
    if (evt.type === "step" && evt.label) {
      handlers.onStep(evt.label);
    } else if (evt.type === "draftSaved") {
      handlers.onDraftSaved?.();
    } else if (evt.type === "final") {
      finalEmitted = true;
      handlers.onFinal(evt.content ?? "");
    } else if (evt.type === "error") {
      finalEmitted = true;
      handlers.onError(evt.content ?? "Maaf, terjadi kesalahan.");
    }
  };

  const reader = res.body.getReader();
  const decoder = new TextDecoder();
  let buffer = "";

  try {
    for (;;) {
      const { done, value } = await reader.read();
      if (done) break;
      buffer += decoder.decode(value, { stream: true });
      let idx = buffer.indexOf("\n");
      while (idx >= 0) {
        handleLine(buffer.slice(0, idx));
        buffer = buffer.slice(idx + 1);
        idx = buffer.indexOf("\n");
      }
    }
    buffer += decoder.decode();
    if (buffer.trim()) handleLine(buffer);
  } catch {
    if (!finalEmitted) {
      handlers.onError(
        "Koneksi terputus saat memproses jawaban. Coba lagi ya.",
      );
    }
    return;
  }

  if (!finalEmitted) {
    handlers.onError("Maaf, jawaban tidak lengkap diterima. Coba lagi ya.");
  }
}
