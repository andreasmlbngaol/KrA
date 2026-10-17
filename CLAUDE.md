# CLAUDE.md — KrA (KRS Assistant)

Panduan konteks & kondisi proyek untuk Claude Code. Baca ini dulu sebelum menyentuh kode.

---

## 0. Status Proyek — BACA DULU

> ⚠️ **Ini VERSI AWAL dan MASIH JAUH DARI SELESAI.**

Yang **sudah** ada:
- Server Spring Boot fungsional: auth JWT, data akademik (KHS/KRS/kurikulum/kelas), agent AI (Koog + OpenRouter), observability token.
- Web app React fungsional: login, dashboard, KHS, kelola KRS, chat agent (streaming), monitoring.
- Docker (prod + dev), seeder otomatis, Flyway migration, DB ter-expose untuk inspector.

Yang **BELUM** ada — dan ini inti tesis:
- **❌ Integrasi CSP (Constraint Satisfaction Problem).** Ini tujuan utama proyek, tapi **belum diimplementasikan sama sekali**. Saat ini pemilihan matkul/draft KRS sepenuhnya dilakukan oleh **LLM lewat tool-calling** (agent memanggil tool validasi + menyusun draft sendiri berdasarkan prompt). **Tidak ada solver CSP, tidak ada backtracking, tidak ada forward-checking, tidak ada variabel/domain/constraint eksplisit** yang dijalankan sebagai algoritma.
- Validasi yang ada (`KrsValidationUseCase`) bersifat **imperatif/checklist** (cek bentrok jadwal, prasyarat, kuota, duplikat, elektif) — **bukan** CSP solver. Ia memverifikasi draft yang sudah disusun, bukan **mencari** solusi.
- **Belum ada** endpoint yang menerima daftar matkul yang diinginkan lalu mengembalikan **kombinasi kelas/KOM optimal** hasil pencarian CSP.
- Password masih plaintext `password == nim` (TODO di `AuthController`) — belum ada kolom `passwordHash`.
- Belum ada test selain smoke test `ServerApplicationTests`.
- Belum ada integrasi SIA/SATU MAHASISWA (data dari seeder).
- `chat_history` in-memory Koog `InMemoryChatHistoryProvider` → **tidak persist** lintas restart (histori sesi agent); yang persist hanya observability `chat_history` table (untuk dashboard), bukan memori percakapan.

**Arah ke depan (rencana CSP):** inti proyek adalah memodelkan penyusunan KRS sebagai CSP — variabel = matkul yang perlu diambil, domain = kelas/KOM yang tersedia, constraint = prasyarat, kuota, bentrok jadwal, batas SKS/IP, paritas ganjil-genap, KOM tetap, praktikum-butuh-teori, min SKS capstone, MKWU reserved, MBKM. LLM berperan sebagai **antarmuka & reasoning**, sedangkan **pencarian solusi yang benar** diserahkan ke CSP solver deterministik (bukan ditebak LLM). Belum ada satu baris pun dari ini.

---

## 1. Apa Itu KrA

**KrA (KRS Assistant)** — asisten akademik LLM untuk mahasiswa **Prodi Ilmu Komputer USU** (Universitas Sumatera Utara). Membantu mahasiswa **menyusun draft KRS** (Kartu Rencana Studi) yang nanti diajukan manual ke Dosen Pembimbing Akademik.

- **Tujuan akademik:** skripsi S1. Pendekatan yang diklaim = **CSP (Constraint Satisfaction Problem)** untuk validasi/penyusunan KRS.
- **Bahasa antarmuka & prompt:** Indonesia.
- **Pengguna:** mahasiswa (login pakai NIM).

### Proyek lama vs baru
- **Lama** (`/home/sana/Code/kmp/KRS`): Ktor + Kotlin Multiplatform (modul `:core`, `:app:sharedLogic`, `:server`, webApp Kotlin/JS `krsShared`). **Ini sumber kebenaran** untuk perilaku, route, prompt, dan UI — migrasi harus menyesuaikan, bukan mendesain ulang.
- **Baru** (`/home/sana/Code/project/KrA`): Spring Boot monorepo (`server` + `app`), standalone Gradle, tanpa Kotlin/JS. **Ini proyek aktif.**

---

## 2. Layout Repo

```
KrA/
├── CLAUDE.md                 # dokumen ini
├── docker-compose.yml        # prod stack (project name: kra-prod)
├── docker-compose.dev.yml    # dev stack  (project name: kra-dev)
├── .env                      # PROD secrets (gitignored)
├── .env.example              # template prod
├── .env.dev.example          # template dev
├── server/                   # Spring Boot (Gradle standalone, rootProject "kra")
│   ├── Dockerfile            # prod (multi-stage, eclipse-temurin:25-jdk-noble → 25-jre-noble)
│   ├── Dockerfile.dev        # dev (bind-mount + ./gradlew bootRun)
│   ├── gradle/libs.versions.toml
│   ├── settings.gradle.kts   # rootProject.name = "kra"
│   └── src/main/
│       ├── kotlin/id/andreasmlbngaol/kra/…
│       └── resources/
│           ├── application.yaml
│           ├── db/migration/V1__baseline_schema.sql
│           └── prompt/*.md    # 9 modul system prompt
└── app/                      # React 19 + Vite 8 (bun, bukan node)
    ├── Dockerfile            # prod (oven/bun:1-alpine build → nginx:1.27-alpine)
    ├── Dockerfile.dev        # dev (bun + Vite HMR)
    ├── nginx.conf            # SPA + reverse-proxy /api/ (streaming-safe)
    ├── .env / .env.example   # VITE_API_BASE_URL=/api
    └── src/{api,auth,components,pages,types,utils,styles}
```

---

## 3. Arsitektur

**Feature-based + clean architecture, 4 layer per fitur:** `domain` / `data` / `presentation` / `di`.
Dependency mengarah ke dalam: `presentation → domain ← data`; `di` merangkai.
Wiring **Spring auto-wiring** (constructor injection) — **bukan** manual DI seperti proyek lama.

Package root: `id.andreasmlbngaol.kra`.

Fitur:
- `academic` — domain akademik (courses, KHS, KRS, kurikulum, mahasiswa, seeder).
- `agent` — AI agent (Koog), tool sets, observability.
- `auth` — JWT, security.
- `core` — enums, error handling, web config (CORS, exception handler, root controller).

Setiap fitur:
```
<feature>/
├── domain/{model,repository,usecase}   # model murni + interface repo + @Service use case
├── data/{entity,repository(+jpa),seed,observability,tools}
├── presentation/{Controller.kt,dto/}
└── di/                                  # @Configuration (bean)
```

Konvensi:
- **Use case = `@Service`** (thin orchestration, tanpa state).
- **Repository interface di `domain`, implementasi di `data`** (`*RepositoryImpl`), JPA di `data/repository/jpa`.
- **Entity JPA terpisah dari model domain** (mapping manual di `*RepositoryImpl`).
- DTO di `presentation/dto`, dipetakan dari model domain (kadang use case mengembalikan DTO langsung — mis. `KhsUseCase.getKhsSummary`).
- Enum domain di `core.enums`, di-serialisasi `kotlinx.serialization` (`@SerialName` = wire value persis).

---

## 4. Tech Stack & Versi

**Server** (`server/gradle/libs.versions.toml`):
| Komponen | Versi |
|---|---|
| Kotlin | 2.4.21 |
| Spring Boot | 4.1.1 (Jakarta, Spring Framework 7) |
| Java toolchain | 25 |
| Gradle wrapper | 9.8.1 |
| Koog (AI) | 1.3.0 (additions 1.3.0-beta) |
| Ktor client | 3.6.0 (dipaksa; koog pin 3.3.3 punya bug Content-Length) |
| kotlinx-coroutines | 1.11.0 (dipaksa untuk kompat ktor 3.6.0) |
| kotlinx-serialization | 1.11.0 |
| PostgreSQL driver | 42.7.14 |
| Flyway | 13.10.0 |
| java-jwt (auth0) | 4.6.2 |
| springdoc-openapi | 3.1.1 |
| Hibernate | 7.4.5 (via SB4) |

- Persistensi: **Spring Data JPA + Hibernate**, `ddl-auto: validate` (schema dari Flyway, **bukan** auto-generate).
- DB: **PostgreSQL 16**. Test: **H2** (in-memory).
- Model LLM: **`anthropic/claude-haiku-5.5`** via `OpenRouterLLMClient` (butuh `OPENROUTER_API_KEY`; fail-fast kalau kosong). Endpoint OpenRouter OpenAI-compatible (`https://openrouter.ai/api/v1/chat/completions`).

**Frontend** (`app/package.json`):
| Komponen | Versi |
|---|---|
| React / React DOM | 19.2.8 |
| react-router-dom | 7.5.0 |
| Vite | 8.3.4 |
| TypeScript | ~6.0.2 |
| Tailwind CSS | 3.4 |
| axios | ^1.7.7 |
| goey-toast | ^0.5.0 |
| react-markdown / remark-gfm | ^9.0.1 / ^4.0.0 |
| oxlint | ^1.81.0 |
| **Package manager** | **bun** (1.4.x) — **JANGAN pakai node/npm** |

> ⚠️ **Frontend WAJIB pakai `bun`.** Install `bun install`, build `bun run build`, dev `bun run dev`.

---

## 5. Server — Route & Endpoint

Base: `/api`. Semua **kecuali** `auth` & `dashboard` butuh `Authorization: Bearer <accessToken>`.

| Method | Path | Controller | Auth |
|---|---|---|---|
| GET | `/` | `RootController` | publik → 301 redirect ke `api/` |
| GET | `/api`, `/api/` | `RootController` | publik → `"KrA API is running"` |
| POST | `/api/auth/login` | `AuthController` | publik |
| POST | `/api/auth/refresh` | `AuthController` | publik (butuh cookie refresh) |
| POST | `/api/auth/logout` | `AuthController` | publik |
| GET | `/api/dashboard/chat-history` | `DashboardController` | **publik** (internal monitoring) |
| GET | `/api/dashboard/token-usage` | `DashboardController` | **publik** |
| POST | `/api/agent/chat` | `AgentController` | JWT |
| POST | `/api/agent/chat/stream` | `AgentController` | JWT — NDJSON |
| GET | `/api/khs` | `KhsController` | JWT |
| GET/POST | `/api/khs/entries` | `KhsController` | JWT |
| DELETE | `/api/khs/entries/{courseCode}/{semester}` | `KhsController` | JWT |
| GET | `/api/krs/draft` | `KrsController` | JWT |
| DELETE | `/api/krs/draft/{classOfferingId}` | `KrsController` | JWT |

Route ini **sudah disamakan persis** dengan proyek Ktor lama (catatan: `khs` dulu sempat `/api/academic/khs`, sudah dikembalikan ke `/api/khs`).

**Auth detail:**
- Login: NIM + password → `TokenResponse{accessToken, tokenType}` di body + **refresh cookie HttpOnly** (`refreshToken`, path `/api/auth`).
- **TODO di kode:** verifikasi password masih `request.password != request.nim` (plaintext = NIM). Harus diganti verifikasi hash dari kolom `Student.passwordHash`.
- Access token: HMAC256, klaim `nim` + `type=access`, expiry 15 menit. Refresh: 30 hari.
- Security stateless, CSRF off, CORS on (`core/web/CorsConfig.kt`).

**Streaming chat** (`/api/agent/chat/stream`):
- `StreamingResponseBody` + `runBlocking` + `Channel.UNLIMITED`.
- Tulis 1 byte awal **segera** agar proxy melihat aktivitas.
- Event NDJSON (`ChatStreamEvent{type,label,content}`), `type` ∈ `step` / `draftSaved` / `final` / `error` / `ping`.
- **Heartbeat `ping` tiap 5 detik** (menutup jeda sebelum token pertama).
- `error` memakai pesan ramah dari `core/error/ErrorHandling.kt` (tidak pernah bocorkan detail provider).

---

## 6. Server — Aturan Domain (WAJIB dipahami, ini inti logika)

Logika ini ada di `KhsUseCase`, `KrsValidationUseCase`, `CourseUseCase`. **Semua angka SKS/IP harus dari sini, jangan dihitung LLM.**

**Grade** (`core.enums.Grade`): `A, B_PLUS, B, C_PLUS, C, D, E, T`. Point: A=4.0, B+=3.5, B=3.0, C+=2.5, C=2.0, D=1.0, E=0.0, T=0.0. `T` (Tunda) **diabaikan** di perhitungan IP/IPK.

**KHS:**
- IP semester = Σ(SKS × point) / ΣSKS, pakai **nilai terbaik** per matkul, abaikan `T`.
- IPK = kumulatif pakai nilai terbaik per matkul (abaikan `T`).
- Lulus = grade ≠ E dan ≠ T. Prasyarat dianggap lulus bila grade **minimal D**.

**Kuota SKS berdasarkan IP semester sebelumnya** (`getMaxSksByIp`):
`IP ≥ 3.00 → 24`, `≥ 2.50 → 22`, `≥ 2.00 → 20`, `≥ 1.50 → 18`, `< 1.50 → 16`.
Semester 1–2 (atau KHS sebelumnya kosong) → **24**.

**MKWU reserved SKS** (`getMkwuReservedSks`): sem 3 = 4, sem 4 = 2, sem 5 = 2, lainnya 0. Semester 6–8 **tidak ada MKWU**.

**Validasi draft** (`KrsValidationUseCase.validateDraft`) — cek: total vs max SKS (setelah reserved MKWU), **bentrok jadwal** (hari sama + jam overlap), **pelanggaran prasyarat**, **matkul duplikat** (kode sama >1×), **kewajiban elektif** (pilih 1 dari N REGULAR_ELECTIVE normalSemester == semester). `remainingSks = maxSks − mkwuReserved − totalSks`. Draft disimpan **hanya** bila tak ada isu pemblokir (`KrsUseCase.saveDraftKrs` → `SaveDraftKrsResult.Success/Rejected`).

**Paritas:** matkul ganjil hanya di semester ganjil, genap hanya genap. **Pengecualian:** KP/Magang `ILK4202` & Tugas Akhir `ILK4299` (parity-free).

**Tipe matkul** (`CourseType`): `MKWU`, `MKWK`, `REGULAR_MANDATORY`, `REGULAR_ELECTIVE`. Label FE via `jenisLabel()`.

**Repeatable** (`getRepeatableCourses`): matkul dengan grade terbaik ≤ C+ (ordinal ≥ C_PLUS) dan ditawarkan semester ini. `mustRepeat` bila grade E/T.

**Capstone:** `ILK4202` (KP/Magang) & `ILK4299` (TA) — syarat `minSks` di `course_constraints`. MBKM (sem 6 & 7) → batas SKS `min(20, batas IP)`.

---

## 7. Agent (Koog)

**`KrsAgent`** (`agent/data/KrsAgent.kt`):
- `@Service`; **instance agent dibangun per-request** (agar callback `onToolStep`/`onDraftSaved` terisolasi), tapi **chat memory dibagi** (`InMemoryChatHistoryProvider`, windowSize 50).
- System prompt = modul universal + 1 modul alur semester. Modul di `resources/prompt/`:
  - Universal: `00-identitas-dan-alur-wajib.md`, `04-aturan-umum.md`, `05-aturan-kritis.md`, `06-aturan-draft-krs.md`, `07-format-tampilan-rekomendasi.md`, `08-gaya-jawaban.md`.
  - Alur: sem 1–2 → `01-…`, sem 3–5 → `02-…`, sem 6–8 → `03-…`.
- NIM **disuntikkan** sebagai konteks sistem (`buildContextualMessage`) — agent **tidak boleh** tanya NIM.
- Observability lewat `handleEvents`: `onLLMCallCompleted` → `TokenUsageTracker.recordLlmCall`; `onToolCallStarting` → `recordToolCall` + emit step.

**Tool sets** (semua `@Component` kecuali `KrsToolSet` yang per-request):
- `StudentInfoToolSet`: `getStudentInfo`.
- `KhsToolSet`: `getKhs`, `getIpSemester`, `getTotalPassedSks`, `getRepeatableCourses`.
- `CourseToolSet`: `getAvailableCourses`, `getCurriculumPackage`, `getCourseClasses`, `calculateTotalSks`, `getRemainingMandatoryCourses`, `getRequiredElectives`, `getEligibleUpperCourses`, `getCapstoneCourses`.
- `KrsToolSet`: `getKrsDraft`, `saveDraftKrs`, `getSksBudget`, `getMbkmSksBudget`, `validateKrsDraft`.

Semua tool mengembalikan **JSON string** (dibaca LLM per field). Label ramah step ada di `agent/data/ToolStepLabels.kt`.

**TokenUsageTracker** (`agent/data/observability`): counter Atomic per-menit; scheduler **tiap 60 detik** log + persist 1 baris `token_usage_minute` ke PostgreSQL, lalu reset window. Dashboard membaca ini (`/api/dashboard/token-usage`, range `15m`/`6h`/menit).

---

## 8. Data Model (Flyway V1, PostgreSQL)

13 tabel: `academic_terms`, `students`, `lecturers`, `courses`, `course_constraints`, `course_prerequisites`, `curriculum_packages`, `class_offerings`, `offering_lecturers`, `khs`, `krs`, `token_usage_minute`, `chat_history`.

Catatan desain penting:
- `academic_terms.is_active` = sumber kebenaran "term berjalan" (hanya 1 aktif).
- `students.entry_year` → `posisi_angkatan` **dihitung saat query** (bukan disimpan); `students.semester` hanya display/audit, **bukan** untuk logika CSP.
- `course_constraints.min_sks` (1:1 dengan `courses`) memisahkan constraint internal prodi dari katalog resmi, agar re-sync katalog tidak menimpa.
- `ClassOffering` = kelas **per-term** (KOM A/B/C identitas tetap, tapi dosen/jadwal/kuota bisa beda tiap tahun).
- `khs`/`krs` pakai composite key.

**Seeder** (`AcademicSeeder`, `ApplicationRunner`): **idempotent** — skip bila `students` sudah ada isi. Mengisi dosen, matkul, constraint, prasyarat, term 2022–2026 (2026 GANJIL aktif), 748 class offerings, 40 mahasiswa, 51 paket kurikulum, 1127 KHS. **Test student: NIM `230001`, password = NIM (`230001`)**. Data seed ada di `AcademicSeedData.kt` + `KhsSeedScenarios.kt`.

---

## 9. Frontend

- Entry: `src/main.tsx` → `App.tsx` (`AuthProvider > BrowserRouter > Routes`).
- Route: `/login`, `/dashboard`, `/khs`, `/kelola-krs`, `/monitoring`, `/monitoring/chat`. `ProtectedRoute` untuk yang perlu auth. Fallback `*` → `/dashboard`.
- Komponen global: `ChatLauncher` (FAB chat) + `GooeyToaster` (bottom-left).
- API client: `src/api/http.ts` — axios, `baseURL = VITE_API_BASE_URL ?? '/api'`, access token di header `Authorization`, **auto-refresh on 401** (single-flight via `refreshPromise`; refresh client `withCredentials` untuk kirim cookie HttpOnly).
- Tipe: `src/types/api.ts` = mirror plain-TS dari DTO server (menggantikan `krsShared` Kotlin/JS proyek lama). `src/types/ui.ts` = tipe UI.
- Halaman: `LoginPage`, `DashboardPage`, `KhsPage`, `ManageKrsPage`, `ChatPage`, `TokenUsagePage`, `ChatHistoryPage`.
- `ChatPage` streaming: pakai `fetch` mentah (bukan axios) untuk NDJSON dari `/agent/chat/stream`; menangani `step`/`draftSaved`/`final`/`error`/`ping`.
- Styling: Tailwind 3, warna `usu` (hijau #13a05b) & `gold` (#f5b800). `styles/global.css` punya animasi chat & markdown.
- **Streaming via nginx wajib** `proxy_buffering off` + timeout panjang (sudah ada di `app/nginx.conf`).

---

## 10. Docker & Environment

**Compose:**
- `docker-compose.yml` → project **`kra-prod`**: postgres (127.0.0.1:5433, volume `kra_pgdata`) + server (internal :8083, healthcheck) + app (`${WEB_PORT:-80}:80`). Server internal-only (diakses nginx).
- `docker-compose.dev.yml` → project **`kra-dev`**: port 5434 / 8081 / 5173, bind-mount source, volume `kra_pgdata_dev` + `kra_gradle_cache_dev`, Vite HMR.

**Env:**
- **Root `.env`** (prod) & **`.env.dev`** (dev, via `--env-file`). Server **tidak** memuat file `.env` sendiri — variabel disuntik lewat Compose. Untuk run lokal langsung, set env di shell/IDE.
- `application.yaml` membaca: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `PORT` (default 8080), `OPENROUTER_API_KEY`, `JWT_*`.
- `app/.env` → `VITE_API_BASE_URL=/api`.

**⚠️ Base image Docker DIPIN ke varian `-noble`** (`eclipse-temurin:25-jdk-noble` / `25-jre-noble`). Tag polos `eclipse-temurin:25-jre` pernah berganti varian Alpine↔Ubuntu dan memecahkan healthcheck (Alpine tanpa `bash`, Ubuntu tanpa `wget`). Healthcheck server pakai `bash -c '</dev/tcp/127.0.0.1/8083'`.

**Postgres untuk IntelliJ DB Inspector:** `jdbc:postgresql://127.0.0.1:5433/kra_db`, user `postgres`, pass `postgres`.

---

## 11. Perintah Umum

```bash
# --- Server ---
cd server
./gradlew bootRun                 # run lokal (butuh env DB_URL/OPENROUTER_API_KEY)
./gradlew bootJar                 # build jar
./gradlew compileKotlin           # cek kompilasi cepat
./gradlew test                    # H2

# --- Frontend (BUN, bukan node) ---
cd app
bun install
bun run dev                       # Vite HMR di :5173 (proxy /api → :8083)
bun run build                     # tsc -b && vite build
bun run lint                      # oxlint

# --- Docker ---
docker compose up -d --build                 # prod
docker compose down                          # stop prod
docker compose down -v                       # stop + hapus DB (seed ulang saat up)
docker compose --env-file .env.dev -f docker-compose.dev.yml up   # dev
```

---

## 12. Konvensi & Batasan (JANGAN dilanggar)

1. **Frontend pakai `bun`** — jangan node/npm/yarn.
2. **Git commit: TANPA co-author selain `andreasmlbngaol`.** Jangan tambahkan `Co-Authored-By: Claude` di proyek ini (instruksi eksplisit pemilik proyek).
3. **Jangan hitung SKS/IP/konflik manual** di logika agent — selalu lewat tool/use case.
4. **Route harus mengikuti proyek lama (Ktor)** — sumber kebenaran. Kalau menambah/mengubah endpoint, samakan dengan yang lama.
5. **Angka SKS yang ditampilkan ke mahasiswa** harus persis dari hasil tool (`calculateTotalSks`, `getSksBudget`, `validateKrsDraft`), bukan penjumlahan LLM.
6. **Entity JPA terpisah dari model domain**; jangan expose entity ke presentation.
7. **Enum wire value via `@SerialName`** — kontrak JSON ke FE tidak boleh berubah tanpa alasan.
8. **Jangan bocorkan error provider mentah** ke user (pakai `ErrorHandling.kt`).
9. Istilah teknis sistem (`Hard Lock`, `Soft Lock`, `REGULAR_MANDATORY`, `MKWU`, `MKWK`, UUID) **tidak boleh** muncul di jawaban ke mahasiswa — hanya label ramah.

---

## 13. Roadmap / Yang Belum Selesai

**Prioritas inti (inti skripsi — CSP):**
1. **Rancang & implementasikan CSP solver** untuk penyusunan KRS: definisikan variabel (matkul), domain (kelas/KOM), constraint (prasyarat, kuota, jadwal, SKS, paritas, KOM, praktikum-teori, min SKS, MKWU, MBKM), dan algoritma pencarian (backtracking + forward checking / AC-3).
2. **Endpoint baru** yang mengembalikan solusi KRS optimal dari daftar keinginan mahasiswa (bukan sekadar validasi draft yang sudah jadi).
3. **Integrasikan solver dengan agent** — LLM menyusun preferensi & menjelaskan hasil, solver yang mencari solusi.

**Lain-lain:**
4. Password hashing (`Student.passwordHash`) — hapus TODO plaintext di `AuthController`.
5. Persist chat memory agent (sekarang in-memory, hilang saat restart).
6. Test: unit test use case + test solver CSP (sekarang cuma smoke test).
7. Auth untuk `/api/dashboard/**` (sekarang publik "untuk internal").
8. Integrasi data nyata dari SIA/SATU MAHASISWA (sekarang seeder).
9. Rate limiting & kuota token per mahasiswa.

> **Ingat:** versi sekarang adalah **kerangka fungsional + agent LLM**, belum sistem CSP. Klaim tesis (CSP) belum terwujud di kode. Setiap perubahan ke arah CSP harus mempertahankan kontrak route & DTO yang sudah disamakan dengan proyek lama.
