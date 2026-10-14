import { useEffect, useRef, useState } from "react";
import type { FormEvent, KeyboardEvent } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { streamChatMessage } from "../api/chat";
import { ChatMessage } from "../components/ChatMessage";
import type { ChatMessageItem } from "../types/ui";
import { createId } from "../utils/id";

// Pesan default yang dikirim ke backend saat mahasiswa menekan tombol mulai.
// Apa pun rencananya, muara percakapan tetap untuk membantu mengisi KRS, jadi
// tombol ini menghemat langkah mengetik manual di awal.
const STARTER_PROMPT = "Bantu isi KRS semester ini";

const WELCOME: ChatMessageItem = {
  id: "welcome",
  role: "assistant",
  content:
    "Halo! \ud83d\udc4b Aku **KRS Agent** untuk mahasiswa Ilmu Komputer USU.\n\nKlik tombol **“Bantu Isi KRS Semester Ini”** di bawah untuk langsung mulai \u2014 atau ketik sendiri kebutuhanmu kapan saja.",
};

interface ChatPageProps {
  /** True saat animasi tutup sedang berjalan (memicu animasi mengecil). */
  closing: boolean;
  /** Dipanggil saat pengguna menutup chat. */
  onClose: () => void;
}

export function ChatPage({ closing, onClose }: ChatPageProps) {
  const { nim } = useAuth();
  const navigate = useNavigate();
  const [sessionId] = useState(() => createId());
  const [messages, setMessages] = useState<ChatMessageItem[]>([WELCOME]);
  const [input, setInput] = useState("");
  const [sending, setSending] = useState(false);
  const [steps, setSteps] = useState<string[]>([]);
  // True setelah backend mengonfirmasi draft KRS BERHASIL tersimpan (lolos
  // validasi). Memicu tampilnya tombol pintas ke halaman Kelola KRS.
  const [draftSaved, setDraftSaved] = useState(false);
  const listRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLTextAreaElement>(null);
  // True selama pengguna berada dekat dasar daftar. Saat pengguna menggulir ke
  // atas (mis. membaca pesan lama ketika balasan masih loading), nilainya jadi
  // false sehingga auto-scroll TIDAK menariknya kembali ke bawah.
  const stickToBottomRef = useRef(true);

  // Jarak (px) dari dasar yang masih dianggap "menempel di bawah".
  const STICK_THRESHOLD = 120;

  const isNearBottom = () => {
    const el = listRef.current;
    if (!el) return true;
    return el.scrollHeight - el.scrollTop - el.clientHeight <= STICK_THRESHOLD;
  };

  // Perbarui status "menempel di bawah" setiap kali pengguna menggulir manual.
  const handleListScroll = () => {
    stickToBottomRef.current = isNearBottom();
  };

  const scrollToBottom = () => {
    const el = listRef.current;
    el?.scrollTo({ top: el.scrollHeight, behavior: "smooth" });
  };

  // Tampilkan tombol mulai selama mahasiswa belum pernah mengirim pesan.
  const hasUserMessage = messages.some((m) => m.role === "user");
  const showStarter = !hasUserMessage && !sending;

  useEffect(() => {
    // Hanya auto-scroll bila pengguna memang sedang menempel di bawah. Ini
    // membuat daftar tetap bisa digulir ke atas saat balasan sedang loading
    // (sebelumnya update "steps" tiap saat memaksa balik ke bawah).
    if (!stickToBottomRef.current) return;
    scrollToBottom();
  }, [messages, sending, steps, draftSaved]);

  // Shortcut: tekan "/" untuk langsung fokus ke input chat (jika belum sedang mengetik).
  useEffect(() => {
    const handler = (event: globalThis.KeyboardEvent) => {
      if (event.key !== "/") return;
      const el = document.activeElement as HTMLElement | null;
      const tag = el?.tagName;
      const isTyping =
        tag === "INPUT" || tag === "TEXTAREA" || el?.isContentEditable === true;
      if (isTyping) return;
      event.preventDefault();
      inputRef.current?.focus();
    };
    window.addEventListener("keydown", handler);
    return () => window.removeEventListener("keydown", handler);
  }, []);

  // Jaga fokus tetap di input chat setelah mengirim (mis. setelah klik tombol
  // Kirim / tombol mulai, atau setelah balasan selesai). Fokus hanya hilang bila
  // mahasiswa sendiri yang mengklik area lain di layar.
  const refocusInput = () => {
    requestAnimationFrame(() => inputRef.current?.focus());
  };

  // Tutup overlay chat lalu arahkan ke halaman Kelola KRS. Navigasi dulu supaya
  // halaman tujuan sudah ter-render di belakang saat overlay menyusut.
  const goToManageKrs = () => {
    // Selalu sertakan state baru agar React Router membuat history entry baru
    // (location.key berubah) — ini memicu ManageKrsPage memuat ulang datanya
    // bahkan ketika kita SUDAH berada di /kelola-krs. Tanpa ini, navigate ke
    // path yang sama tidak me-refresh data dan info terbaru tidak muncul.
    navigate("/kelola-krs", { state: { refreshAt: Date.now() } });
    onClose();
  };

  const sendMessage = async (rawText: string) => {
    const text = rawText.trim();
    if (!text || sending) return;

    const userMessage: ChatMessageItem = {
      id: createId(),
      role: "user",
      content: text,
    };
    setMessages((prev) => [...prev, userMessage]);
    // Pesan dari pengguna sendiri selalu menarik tampilan ke bawah.
    stickToBottomRef.current = true;
    setInput("");
    setSending(true);
    setSteps([]);
    // Sembunyikan tombol pintas "Kelola KRS" milik turn sebelumnya: tombol baru
    // hanya boleh muncul bila save di turn INI benar-benar berhasil.
    setDraftSaved(false);
    // Kembalikan fokus ke input segera (penting saat dipicu lewat klik tombol).
    refocusInput();

    await streamChatMessage(sessionId, text, {
      onStep: (label) => setSteps((prev) => [...prev, label]),
      onFinal: (content) => {
        setMessages((prev) => [
          ...prev,
          { id: createId(), role: "assistant", content },
        ]);
        setSending(false);
        setSteps([]);
        refocusInput();
      },
      onError: (msg) => {
        setMessages((prev) => [
          ...prev,
          { id: createId(), role: "assistant", content: msg, isError: true },
        ]);
        setSending(false);
        setSteps([]);
        refocusInput();
      },
      // Draft KRS berhasil tersimpan di backend -> tampilkan tombol pintas.
      onDraftSaved: () => setDraftSaved(true),
    });
  };

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    void sendMessage(input);
  };

  const handleKeyDown = (event: KeyboardEvent<HTMLTextAreaElement>) => {
    // Saat IME sedang menyusun teks, jangan perlakukan Enter sebagai kirim.
    if (event.nativeEvent.isComposing) return;
    // Di perangkat sentuh (HP/tablet), Enter = baris baru; kirim cukup lewat
    // tombol "Kirim". Ini mencegah keyboard HP langsung mengirim pesan saat
    // pengguna sebenarnya hanya ingin menambah baris baru.
    const isTouch =
      typeof window !== "undefined" &&
      window.matchMedia?.("(pointer: coarse)").matches === true;
    if (event.key === "Enter" && !event.shiftKey && !isTouch) {
      event.preventDefault();
      void sendMessage(input);
    }
  };

  return (
    <div
      className={`${closing ? "chat-content-out" : "chat-content-in"} flex flex-col bg-white`}
    >
      <header className="sticky top-0 z-20 flex h-16 items-center justify-between gap-3 bg-gradient-to-r from-usu to-usu-dark px-5 text-white shadow">
        <div className="flex items-center gap-3">
          <button
            type="button"
            onClick={onClose}
            aria-label="Tutup"
            title="Tutup"
            className="flex h-8 w-8 items-center justify-center rounded-lg text-white/90 transition hover:bg-white/15"
          >
            <svg
              viewBox="0 0 24 24"
              width="20"
              height="20"
              fill="none"
              stroke="currentColor"
              strokeWidth={2}
              strokeLinecap="round"
              strokeLinejoin="round"
            >
              <path d="M6 6l12 12M18 6 6 18" />
            </svg>
          </button>
          <div className="flex flex-col leading-tight">
            <span className="font-bold">KRS Agent</span>
            {nim && <span className="text-xs text-white/75">NIM {nim}</span>}
          </div>
        </div>
      </header>

      <div
        ref={listRef}
        onScroll={handleListScroll}
        className="flex-1 overflow-y-auto bg-neutral-50 px-4 py-6 sm:px-6"
      >
        <div className="mx-auto flex max-w-5xl flex-col gap-5">
          {messages.map((message) => (
            <ChatMessage key={message.id} message={message} />
          ))}

          {showStarter && (
            <div className="flex flex-col items-start gap-2">
              <button
                type="button"
                onClick={() => void sendMessage(STARTER_PROMPT)}
                className="ai-rainbow-btn inline-flex items-center gap-2 rounded-xl bg-usu px-5 py-3 text-sm font-semibold text-white shadow-sm transition hover:bg-usu-dark"
              >
                <svg
                  viewBox="0 0 24 24"
                  width="18"
                  height="18"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth={2}
                  strokeLinecap="round"
                  strokeLinejoin="round"
                >
                  <path d="M5 12h14M13 6l6 6-6 6" />
                </svg>
                Bantu Isi KRS Semester Ini
              </button>
            </div>
          )}

          {sending && (
            <div className="flex flex-col items-start">
              <div className="mb-1 text-xs font-medium text-neutral-500">
                KRS Agent
              </div>
              <div className="ai-thinking rounded-2xl bg-white px-4 py-3 shadow-sm">
                {steps.length === 0 ? (
                  <span className="ai-thinking-text text-sm font-semibold">
                    KRS Agent sedang berpikir…
                  </span>
                ) : (
                  <ul className="flex flex-col gap-2">
                    {steps.map((step, i) => {
                      const isCurrent = i === steps.length - 1;
                      return (
                        <li
                          key={`${i}-${step}`}
                          className="flex items-center gap-2.5 text-sm"
                        >
                          {isCurrent ? (
                            <span
                              className="ai-step-spinner shrink-0"
                              aria-hidden="true"
                            />
                          ) : (
                            <svg
                              className="shrink-0 text-usu"
                              viewBox="0 0 24 24"
                              width="16"
                              height="16"
                              fill="none"
                              stroke="currentColor"
                              strokeWidth={2.5}
                              strokeLinecap="round"
                              strokeLinejoin="round"
                            >
                              <path d="M20 6 9 17l-5-5" />
                            </svg>
                          )}
                          <span
                            className={
                              isCurrent
                                ? "ai-thinking-text font-semibold"
                                : "text-neutral-500"
                            }
                          >
                            {step}
                          </span>
                        </li>
                      );
                    })}
                  </ul>
                )}
              </div>
            </div>
          )}

          {draftSaved && !sending && (
            <div className="flex flex-col items-start gap-2">
              <button
                type="button"
                onClick={goToManageKrs}
                className="ai-rainbow-btn inline-flex items-center gap-2 rounded-xl bg-usu px-5 py-3 text-sm font-semibold text-white shadow-sm transition hover:bg-usu-dark"
              >
                Buka Halaman Kelola KRS
                <svg
                  viewBox="0 0 24 24"
                  width="18"
                  height="18"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth={2}
                  strokeLinecap="round"
                  strokeLinejoin="round"
                >
                  <path d="M5 12h14M13 6l6 6-6 6" />
                </svg>
              </button>
            </div>
          )}
        </div>
      </div>

      <form
        onSubmit={handleSubmit}
        className="border-t border-neutral-200 bg-white px-4 py-3 sm:px-6"
      >
        <div className="mx-auto flex max-w-5xl items-end gap-2">
          <textarea
            ref={inputRef}
            className="max-h-40 flex-1 resize-none rounded-xl border border-neutral-300 px-3.5 py-2.5 text-sm focus:border-usu focus:outline-none focus:ring-2 focus:ring-usu/30"
            placeholder='Tulis pesan...  (di desktop: "/" untuk fokus, Enter kirim, Shift+Enter baris baru)'
            rows={1}
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={handleKeyDown}
          />
          <button
            type="submit"
            disabled={sending || !input.trim()}
            className={`rounded-xl bg-usu px-5 py-2.5 font-semibold text-white transition disabled:cursor-not-allowed ${
              sending ? "ai-thinking" : "hover:bg-usu-dark disabled:opacity-60"
            }`}
          >
            Kirim
          </button>
        </div>
      </form>
    </div>
  );
}
