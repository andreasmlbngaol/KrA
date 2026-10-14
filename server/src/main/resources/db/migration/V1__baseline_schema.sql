-- =====================================================================
-- V1__baseline_schema.sql
-- Baseline schema for KRS academic system.
-- Consolidated from initial pg_dump + design discussion changes:
--   - academic_terms: added is_active (source of truth for "term berjalan").
--   - students: added entry_year, basis for posisi_angkatan calculation
--     (posisi_angkatan is NOT stored — computed at query time from
--     entry_year + academic_terms.year/semester_type).
--   - courses: removed parity_free (redundant vs class_offerings data),
--     removed min_sks (moved to course_constraints — see below).
--   - course_constraints: new table, 1:1 with courses. Holds prodi-internal
--     CSP-relevant fields that are NOT part of the official campus catalog
--     data (so they can't be blindly overwritten by a future catalog re-sync).
--   - chat_history / token_usage_minute: unchanged, now managed by Flyway
--     as well (previously created ad-hoc via Exposed SchemaUtils.create()).
--
-- NOTE: students.semester is kept — it mirrors the official SIA record,
-- used for display/reference/audit only. It is NOT used for CSP eligibility
-- logic anymore; that role is filled by posisi_angkatan (computed at query
-- time from entry_year + academic_terms.year/semester_type).
--
-- NOT included: a "cuti/freeze semester" tracking table was considered
-- during design but has no current concrete consumer (eligibility is
-- prerequisite-based; lock-KOM uses posisi_angkatan) — intentionally
-- omitted from this baseline. Revisit only if a real use case emerges.
-- =====================================================================

-- ---------------------------------------------------------------------
-- academic_terms
-- ---------------------------------------------------------------------
CREATE TABLE academic_terms (
    id              uuid NOT NULL,
    year            integer NOT NULL,
    semester_type   varchar(10) NOT NULL,
    is_active       boolean NOT NULL DEFAULT false,
    CONSTRAINT academic_terms_pkey PRIMARY KEY (id),
    CONSTRAINT academic_terms_year_semester_type_unique UNIQUE (year, semester_type)
);

-- Only one term may be active at a time.
CREATE UNIQUE INDEX uq_academic_terms_single_active
    ON academic_terms (is_active)
    WHERE is_active = true;

-- ---------------------------------------------------------------------
-- students
-- ---------------------------------------------------------------------
CREATE TABLE students (
    nim         varchar(9) NOT NULL,
    name        varchar(100) NOT NULL,
    semester    integer NOT NULL, -- official semester as recorded in SIA; display/audit only, NOT used for CSP eligibility (see posisi_angkatan, computed from entry_year)
    entry_year  integer NOT NULL, -- basis for posisi_angkatan (computed, not stored)
    CONSTRAINT students_pkey PRIMARY KEY (nim),
    CONSTRAINT chk_students_unsigned_byte_semester CHECK ((semester >= 0) AND (semester <= 255))
);

-- ---------------------------------------------------------------------
-- lecturers
-- ---------------------------------------------------------------------
CREATE TABLE lecturers (
    nidn    varchar(10) NOT NULL,
    name    varchar(100) NOT NULL,
    CONSTRAINT lecturers_pkey PRIMARY KEY (nidn)
);

-- ---------------------------------------------------------------------
-- courses
-- ---------------------------------------------------------------------
CREATE TABLE courses (
    code                character varying(20) NOT NULL,
    name                character varying(100) NOT NULL,
    sks                 integer NOT NULL,
    normal_semester     integer NOT NULL,
    type                varchar(20) NOT NULL,
    CONSTRAINT courses_pkey PRIMARY KEY (code),
    CONSTRAINT chk_courses_unsigned_byte_sks CHECK ((sks >= 0) AND (sks <= 255)),
    CONSTRAINT chk_courses_unsigned_byte_normal_semester CHECK ((normal_semester >= 0) AND (normal_semester <= 255))
);

-- ---------------------------------------------------------------------
-- course_constraints
-- Prodi-internal CSP fields, deliberately separated from `courses`
-- (which mirrors official catalog data) so a future catalog re-sync
-- never risks clobbering manually-maintained constraint data.
-- ---------------------------------------------------------------------
CREATE TABLE course_constraints (
    course_code     character varying(20) NOT NULL,
    min_sks         integer NOT NULL DEFAULT 0,
    CONSTRAINT course_constraints_pkey PRIMARY KEY (course_code),
    CONSTRAINT fk_course_constraints_course_code__code
        FOREIGN KEY (course_code) REFERENCES courses(code) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT chk_course_constraints_unsigned_byte_min_sks CHECK ((min_sks >= 0) AND (min_sks <= 255))
);

-- ---------------------------------------------------------------------
-- course_prerequisites
-- ---------------------------------------------------------------------
CREATE TABLE course_prerequisites (
    course_code         character varying(20) NOT NULL,
    prerequisite_code   character varying(20) NOT NULL,
    CONSTRAINT pk_course_prerequisites PRIMARY KEY (course_code, prerequisite_code),
    CONSTRAINT fk_course_prerequisites_course_code__code
        FOREIGN KEY (course_code) REFERENCES courses(code) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_course_prerequisites_prerequisite_code__code
        FOREIGN KEY (prerequisite_code) REFERENCES courses(code) ON UPDATE RESTRICT ON DELETE RESTRICT
);

-- ---------------------------------------------------------------------
-- curriculum_packages
-- ---------------------------------------------------------------------
CREATE TABLE curriculum_packages (
    semester        integer NOT NULL,
    course_code     character varying(20) NOT NULL,
    lock_type       varchar(10) NOT NULL,
    CONSTRAINT pk_curriculum_packages PRIMARY KEY (semester, course_code),
    CONSTRAINT fk_curriculum_packages_course_code__code
        FOREIGN KEY (course_code) REFERENCES courses(code) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT chk_curriculum_packages_unsigned_byte_semester CHECK ((semester >= 0) AND (semester <= 255))
);

-- ---------------------------------------------------------------------
-- class_offerings
-- ---------------------------------------------------------------------
CREATE TABLE class_offerings (
    id              uuid NOT NULL,
    course_code     character varying(20) NOT NULL,
    class_code      character varying(10) NOT NULL,
    term_id         uuid NOT NULL,
    room            character varying(30),
    day             varchar(10),
    start_time      character varying(5),
    end_time        character varying(5),
    quota           integer NOT NULL,
    CONSTRAINT class_offerings_pkey PRIMARY KEY (id),
    CONSTRAINT fk_class_offerings_course_code__code
        FOREIGN KEY (course_code) REFERENCES courses(code) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_class_offerings_term_id__id
        FOREIGN KEY (term_id) REFERENCES academic_terms(id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT chk_class_offerings_unsigned_byte_quota CHECK ((quota >= 0) AND (quota <= 255))
);

-- ---------------------------------------------------------------------
-- offering_lecturers
-- ---------------------------------------------------------------------
CREATE TABLE offering_lecturers (
    offering_id     uuid NOT NULL,
    lecturer_nidn   varchar(10) NOT NULL,
    CONSTRAINT fk_offering_lecturers_offering_id__id
        FOREIGN KEY (offering_id) REFERENCES class_offerings(id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_offering_lecturers_lecturer_nidn__nidn
        FOREIGN KEY (lecturer_nidn) REFERENCES lecturers(nidn) ON UPDATE RESTRICT ON DELETE RESTRICT
);

-- ---------------------------------------------------------------------
-- khs
-- ---------------------------------------------------------------------
CREATE TABLE khs (
     nim             varchar(9) NOT NULL,
     offering_id     uuid NOT NULL,
     grade           varchar(10) NOT NULL,
     CONSTRAINT pk_khs PRIMARY KEY (nim, offering_id),
     CONSTRAINT fk_khs_nim__nim
         FOREIGN KEY (nim) REFERENCES students(nim) ON UPDATE RESTRICT ON DELETE RESTRICT,
     CONSTRAINT fk_khs_offering_id__id
         FOREIGN KEY (offering_id) REFERENCES class_offerings(id) ON UPDATE RESTRICT ON DELETE RESTRICT
);

-- ---------------------------------------------------------------------
-- krs
-- ---------------------------------------------------------------------
CREATE TABLE krs (
    nim                 varchar(9) NOT NULL,
    class_offering_id   uuid NOT NULL,
    semester            integer NOT NULL,
    CONSTRAINT pk_krs PRIMARY KEY (nim, class_offering_id, semester),
    CONSTRAINT fk_krs_nim__nim
        FOREIGN KEY (nim) REFERENCES students(nim) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_krs_class_offering_id__id
        FOREIGN KEY (class_offering_id) REFERENCES class_offerings(id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT chk_krs_unsigned_byte_semester CHECK ((semester >= 0) AND (semester <= 255))
);

-- ---------------------------------------------------------------------
-- token_usage_minute (observability — now Flyway-managed)
-- ---------------------------------------------------------------------
CREATE TABLE token_usage_minute (
    day             varchar(10) NOT NULL,  -- YYYY-MM-DD
    minute          timestamp NOT NULL,
    llm_requests    bigint NOT NULL,
    input_tokens    bigint NOT NULL,
    output_tokens   bigint NOT NULL,
    total_tokens    bigint NOT NULL,
    tool_calls      bigint NOT NULL,
    tool_breakdown  text NOT NULL,
    CONSTRAINT pk_token_usage_minute PRIMARY KEY (day, minute)
);

-- ---------------------------------------------------------------------
-- chat_history (observability — now Flyway-managed)
-- ---------------------------------------------------------------------
CREATE TABLE chat_history (
    day             varchar(10) NOT NULL,  -- YYYY-MM-DD
    id              uuid NOT NULL,
    ts              timestamp NOT NULL,
    nim             varchar(9) NOT NULL,
    session_id      varchar(64) NOT NULL,
    request         text NOT NULL,
    response        text NOT NULL,
    status          varchar(16) NOT NULL,
    streamed        boolean NOT NULL,
    duration_ms     bigint NOT NULL,
    steps           text NOT NULL,
    CONSTRAINT pk_chat_history PRIMARY KEY (day, id)
);
