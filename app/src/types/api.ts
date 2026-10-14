/**
 * DTOs mirroring the Spring server's wire contract (see server .../presentation/dto).
 * In the old project these came from the compiled Kotlin/JS `krsShared` package; here
 * they are plain TypeScript types so the FE has no Kotlin/JS dependency.
 */

export type Grade = 'A' | 'B_PLUS' | 'B' | 'C_PLUS' | 'C' | 'D' | 'E' | 'T';
export type SemesterType = 'GANJIL' | 'GENAP';
export type Day = 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY';

// --- Auth ---
export interface LoginRequest {
  nim: string;
  password: string;
}

export interface TokenResponse {
  accessToken: string;
  tokenType?: string;
}

// --- Agent / chat ---
export interface ChatRequest {
  sessionId: string;
  message: string;
}

/** One NDJSON line from /agent/chat/stream. */
export interface ChatStreamEvent {
  type: 'step' | 'draftSaved' | 'final' | 'error' | 'ping';
  label?: string;
  content?: string;
}

// --- Dashboard (observability) ---
export interface ChatHistoryEntry {
  id: string;
  timestamp: string;
  nim: string;
  sessionId: string;
  request: string;
  response: string;
  status: string;
  streamed: boolean;
  durationMs: number;
  steps: string[];
}

export interface ChatHistoryResponse {
  enabled: boolean;
  count: number;
  entries: ChatHistoryEntry[];
}

export interface TokenUsagePoint {
  minute: string;
  llmRequests: number;
  inputTokens: number;
  outputTokens: number;
  totalTokens: number;
  toolCalls: number;
  avgTokensPerRequest: number;
}

export interface TokenUsageToolCount {
  name: string;
  count: number;
}

export interface TokenUsageReport {
  rangeMinutes: number;
  enabled: boolean;
  fromMinute: string;
  toMinute: string;
  totalLlmRequests: number;
  totalInputTokens: number;
  totalOutputTokens: number;
  totalTokens: number;
  totalToolCalls: number;
  avgTokensPerRequest: number;
  toolBreakdown: TokenUsageToolCount[];
  series: TokenUsagePoint[];
}

// --- Academic: KHS ---
export interface KhsCourseDto {
  courseCode: string;
  courseName: string;
  classCode: string;
  sks: number;
  grade: Grade;
  gradeLabel: string;
  gradePoint: number;
  classType: string;
  category: string;
}

export interface KhsSemesterDto {
  semester: number;
  academicYear: number;
  semesterType: SemesterType;
  totalSks: number;
  ip: number;
  courses: KhsCourseDto[];
}

export interface KhsSummaryResponse {
  nim: string;
  name: string;
  currentSemester: number;
  totalPassedSks: number;
  cumulativeIp: number;
  semesters: KhsSemesterDto[];
}

export interface KhsEntryDto {
  courseCode: string;
  courseName: string;
  classCode: string;
  sks: number;
  grade: Grade;
  gradeLabel: string;
  semesterTaken: number;
  academicYear: number;
  semesterType: SemesterType;
  classType: string;
  category: string;
}

export interface AddKhsRequest {
  courseCode: string;
  classCode: string;
  grade: Grade;
  semesterTaken: number;
  academicYear: number;
  semesterType: SemesterType;
}

// --- Academic: KRS draft ---
export interface KrsDraftItemDto {
  classOfferingId: string;
  courseCode: string;
  courseName: string;
  classCode: string;
  room?: string | null;
  day?: Day | null;
  startTime?: string | null;
  endTime?: string | null;
  sks: number;
  semester: number;
  classType: string;
  category: string;
  lecturers: string[];
}

export interface KrsDraftResponse {
  semester: number;
  exists: boolean;
  totalSks: number;
  items: KrsDraftItemDto[];
}
