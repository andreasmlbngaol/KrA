-- =====================================================================
-- V2__csp_solver_and_evaluation_observability.sql
--
-- Dua kelompok perubahan:
--   1. Kuota kelas: kolom `enrolled` agar constraint kapasitas di CSP
--      memakai sisa kursi (quota - enrolled), bukan hanya kapasitas total.
--   2. Observability evaluasi: semua data yang dibutuhkan untuk mengukur
--      metrik pada proposal —
--        - solution accuracy      -> evaluation_case / evaluation_run
--        - tool invocation accuracy -> agent_tool_call
--        - efisiensi token        -> kolom metrik pada chat_history
--        - kinerja solver CSP     -> csp_solve_log
--        - penerimaan pengguna    -> survey_response / survey_answer
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. Kuota kelas
-- ---------------------------------------------------------------------
ALTER TABLE class_offerings
    ADD COLUMN enrolled integer NOT NULL DEFAULT 0;

-- Backfill dari KRS yang sudah tercatat (jumlah mahasiswa per kelas),
-- di-clamp ke kapasitas agar CHECK di bawah tidak pernah gagal.
UPDATE class_offerings co
SET enrolled = LEAST(COALESCE(sub.cnt, 0), co.quota)
FROM (
    SELECT class_offering_id, COUNT(*) AS cnt
    FROM krs
    GROUP BY class_offering_id
) sub
WHERE sub.class_offering_id = co.id;

-- Sisa kursi tidak boleh negatif dan tidak boleh melebihi kapasitas.
ALTER TABLE class_offerings
    ADD CONSTRAINT chk_class_offerings_enrolled_range
        CHECK (enrolled >= 0 AND enrolled <= quota);

-- Mempercepat pencarian kelas yang masih punya kursi pada term aktif.
CREATE INDEX idx_class_offerings_term_course
    ON class_offerings (term_id, course_code);

-- ---------------------------------------------------------------------
-- 2a. Metrik per-turn pada chat_history
--     Satu baris chat_history = satu giliran percakapan. Kolom baru
--     memungkinkan perhitungan efisiensi token & jumlah tool call
--     per tugas (bukan hanya agregat per menit).
-- ---------------------------------------------------------------------
ALTER TABLE chat_history
    ADD COLUMN llm_calls     integer NOT NULL DEFAULT 0,
    ADD COLUMN tool_calls    integer NOT NULL DEFAULT 0,
    ADD COLUMN input_tokens  bigint  NOT NULL DEFAULT 0,
    ADD COLUMN output_tokens bigint  NOT NULL DEFAULT 0,
    ADD COLUMN total_tokens  bigint  NOT NULL DEFAULT 0,
    ADD COLUMN model         varchar(64);

-- ---------------------------------------------------------------------
-- 2b. agent_tool_call
--     Setiap pemanggilan tool oleh LLM: nama, argumen, status, durasi.
--     Dasar perhitungan precision/recall/F1 pemanggilan tool.
-- ---------------------------------------------------------------------
CREATE TABLE agent_tool_call (
    id              uuid NOT NULL,
    day             varchar(10) NOT NULL,   -- YYYY-MM-DD (UTC), selaras chat_history
    chat_id         uuid NOT NULL,
    ts              timestamp NOT NULL,
    nim             varchar(9) NOT NULL,
    session_id      varchar(64) NOT NULL,
    seq             integer NOT NULL,       -- urutan pemanggilan dalam satu turn
    tool_name       varchar(64) NOT NULL,
    arguments       text NOT NULL,
    status          varchar(16) NOT NULL,   -- OK / ERROR
    duration_ms     bigint NOT NULL,
    result_size     integer NOT NULL,
    CONSTRAINT pk_agent_tool_call PRIMARY KEY (id),
    CONSTRAINT fk_agent_tool_call_nim__nim
        FOREIGN KEY (nim) REFERENCES students(nim) ON UPDATE RESTRICT ON DELETE CASCADE
);

CREATE INDEX idx_agent_tool_call_chat ON agent_tool_call (day, chat_id);
CREATE INDEX idx_agent_tool_call_nim_ts ON agent_tool_call (nim, ts);

-- ---------------------------------------------------------------------
-- 2c. csp_solve_log
--     Satu baris per pemanggilan solver. Menyimpan ukuran masalah dan
--     statistik pencarian sebagai bukti empiris efisiensi algoritma
--     (AC-3 / MAC / backtracking), bukan sekadar klaim.
-- ---------------------------------------------------------------------
CREATE TABLE csp_solve_log (
    id                  uuid NOT NULL,
    ts                  timestamp NOT NULL,
    nim                 varchar(9) NOT NULL,
    semester            integer NOT NULL,
    status              varchar(16) NOT NULL,   -- FEASIBLE / INFEASIBLE
    objective           varchar(24) NOT NULL,   -- MAXIMIZE_SKS / MEET_TARGET / FEASIBILITY_ONLY
    variables           integer NOT NULL,       -- jumlah variabel (mata kuliah)
    constraints         integer NOT NULL,       -- jumlah constraint aktif
    domain_values       integer NOT NULL,       -- total nilai domain awal
    target_sks          integer NOT NULL,
    result_sks          integer NOT NULL,
    nodes               integer NOT NULL,       -- simpul yang dikunjungi
    backtracks          integer NOT NULL,
    ac3_revisions       integer NOT NULL,       -- jumlah revise() pada AC-3
    forward_checks      integer NOT NULL,
    duration_ms         bigint  NOT NULL,
    alternatives        integer NOT NULL,
    mbkm_mode           boolean NOT NULL,
    chat_id             uuid,
    CONSTRAINT pk_csp_solve_log PRIMARY KEY (id),
    CONSTRAINT fk_csp_solve_log_nim__nim
        FOREIGN KEY (nim) REFERENCES students(nim) ON UPDATE RESTRICT ON DELETE CASCADE
);

CREATE INDEX idx_csp_solve_log_ts ON csp_solve_log (ts);
CREATE INDEX idx_csp_solve_log_nim ON csp_solve_log (nim, ts);

-- ---------------------------------------------------------------------
-- 2d. evaluation_case / evaluation_run
--     Ground truth untuk uji akurasi. `evaluation_case` diisi manual
--     (skenario uji), `evaluation_run` dicatat saat skenario dijalankan.
-- ---------------------------------------------------------------------
CREATE TABLE evaluation_case (
    id                      uuid NOT NULL,
    code                    varchar(32) NOT NULL,
    nim                     varchar(9) NOT NULL,
    semester                integer NOT NULL,
    description             text NOT NULL,
    expected_feasible       boolean NOT NULL,
    expected_min_sks        integer,
    expected_max_sks        integer,
    expected_tools          text NOT NULL DEFAULT '[]',  -- JSON array nama tool
    expected_course_codes   text NOT NULL DEFAULT '[]',  -- JSON array kode matkul
    active                  boolean NOT NULL DEFAULT true,
    created_at              timestamp NOT NULL,
    CONSTRAINT pk_evaluation_case PRIMARY KEY (id),
    CONSTRAINT uq_evaluation_case_code UNIQUE (code),
    CONSTRAINT fk_evaluation_case_nim__nim
        FOREIGN KEY (nim) REFERENCES students(nim) ON UPDATE RESTRICT ON DELETE CASCADE
);

CREATE TABLE evaluation_run (
    id                  uuid NOT NULL,
    case_id             uuid NOT NULL,
    chat_id             uuid,
    ts                  timestamp NOT NULL,
    feasible            boolean NOT NULL,
    total_sks           integer NOT NULL,
    tool_precision      numeric(5,4),
    tool_recall         numeric(5,4),
    tool_f1             numeric(5,4),
    solution_correct    boolean NOT NULL,
    notes               text,
    CONSTRAINT pk_evaluation_run PRIMARY KEY (id),
    CONSTRAINT fk_evaluation_run_case_id__id
        FOREIGN KEY (case_id) REFERENCES evaluation_case(id) ON UPDATE RESTRICT ON DELETE CASCADE
);

CREATE INDEX idx_evaluation_run_case ON evaluation_run (case_id, ts);

-- ---------------------------------------------------------------------
-- 2e. survey_response / survey_answer
--     Kuesioner TAM (perceived usefulness, perceived ease of use) dan
--     SUS (10 item Likert). Satu response = satu responden.
-- ---------------------------------------------------------------------
CREATE TABLE survey_response (
    id              uuid NOT NULL,
    nim             varchar(9) NOT NULL,
    instrument      varchar(8) NOT NULL,   -- TAM / SUS
    submitted_at    timestamp NOT NULL,
    sus_score       numeric(5,2),          -- 0..100, hanya untuk SUS
    pu_score        numeric(5,2),          -- rata-rata PU, hanya untuk TAM
    peou_score      numeric(5,2),          -- rata-rata PEOU, hanya untuk TAM
    CONSTRAINT pk_survey_response PRIMARY KEY (id),
    CONSTRAINT chk_survey_response_instrument CHECK (instrument IN ('TAM', 'SUS')),
    CONSTRAINT fk_survey_response_nim__nim
        FOREIGN KEY (nim) REFERENCES students(nim) ON UPDATE RESTRICT ON DELETE CASCADE
);

CREATE INDEX idx_survey_response_instrument ON survey_response (instrument, submitted_at);

CREATE TABLE survey_answer (
    id              uuid NOT NULL,
    response_id     uuid NOT NULL,
    item_code       varchar(16) NOT NULL,  -- mis. PU1, PEOU1, SUS01
    likert          smallint NOT NULL,     -- 1..5
    CONSTRAINT pk_survey_answer PRIMARY KEY (id),
    CONSTRAINT uq_survey_answer_item UNIQUE (response_id, item_code),
    CONSTRAINT chk_survey_answer_likert CHECK (likert BETWEEN 1 AND 5),
    CONSTRAINT fk_survey_answer_response_id__id
        FOREIGN KEY (response_id) REFERENCES survey_response(id) ON UPDATE RESTRICT ON DELETE CASCADE
);
