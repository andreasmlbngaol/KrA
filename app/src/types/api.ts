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
  llmCalls: number;
  toolCalls: number;
  inputTokens: number;
  outputTokens: number;
  totalTokens: number;
  model: string | null;
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

// --- Monitoring: tool calls -------------------------------------------------
export interface ToolCallEntry {
  chatId: string;
  nim: string;
  sessionId: string;
  seq: number;
  toolName: string;
  arguments: string;
  status: string;
  durationMs: number;
  resultSize: number;
  timestamp: string;
}

export interface ToolCallListResponse {
  enabled: boolean;
  count: number;
  entries: ToolCallEntry[];
}

export interface ToolUsageEntry {
  toolName: string;
  calls: number;
  errors: number;
  avgDurationMs: number;
}

export interface ToolUsageResponse {
  enabled: boolean;
  count: number;
  tools: ToolUsageEntry[];
}

// --- Monitoring: CSP solver -------------------------------------------------
export interface CspSummary {
  totalSolves: number;
  feasibleCount: number;
  infeasibleCount: number;
  avgDurationMs: number;
  avgNodes: number;
  avgBacktracks: number;
  avgAc3Revisions: number;
  avgVariables: number;
  avgDomainValues: number;
}

export interface CspSolveEntry {
  id: string;
  timestamp: string;
  nim: string;
  semester: number;
  status: string;
  objective: string;
  variables: number;
  constraints: number;
  domainValues: number;
  targetSks: number;
  resultSks: number;
  nodes: number;
  backtracks: number;
  ac3Revisions: number;
  forwardChecks: number;
  durationMs: number;
  alternatives: number;
  mbkmMode: boolean;
  chatId: string | null;
}

export interface CspStatsResponse {
  enabled: boolean;
  summary: CspSummary;
  recent: CspSolveEntry[];
}

// --- Monitoring: evaluation -------------------------------------------------
export interface EvaluationCase {
  code: string;
  nim: string;
  semester: number;
  description: string;
  expectedFeasible: boolean;
  expectedMinSks: number | null;
  expectedMaxSks: number | null;
  expectedTools: string[];
  expectedCourseCodes: string[];
  active: boolean;
  createdAt: string | null;
}

export interface EvaluationCaseListResponse {
  count: number;
  cases: EvaluationCase[];
}

export interface EvaluationSummary {
  totalRuns: number;
  correctRuns: number;
  solutionAccuracy: number;
  avgPrecision: number;
  avgRecall: number;
  avgF1: number;
}

export interface EvaluationRun {
  caseCode: string;
  chatId: string | null;
  timestamp: string;
  feasible: boolean;
  totalSks: number;
  precision: number;
  recall: number;
  f1: number;
  solutionCorrect: boolean;
  expectedTools: string[];
  actualTools: string[];
  notes: string | null;
}

export interface EvaluationRunListResponse {
  count: number;
  runs: EvaluationRun[];
}

export interface SurveySummary {
  instrument: string;
  responses: number;
  avgScore: number;
}

export interface SurveySummaryResponse {
  instruments: SurveySummary[];
}

export interface SurveyAnswer {
  itemCode: string;
  likert: number;
}

export interface SurveyResponse {
  nim: string;
  instrument: string;
  submittedAt: string;
  score: number;
  susScore: number | null;
  puScore: number | null;
  peouScore: number | null;
  answers: SurveyAnswer[];
}

export interface SurveyResponseListResponse {
  count: number;
  responses: SurveyResponse[];
}

