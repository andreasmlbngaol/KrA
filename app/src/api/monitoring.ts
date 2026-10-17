import axios from 'axios';
import { API_BASE_URL } from './http';
import type {
  ChatHistoryResponse,
  CspStatsResponse,
  EvaluationCaseListResponse,
  EvaluationRunListResponse,
  EvaluationSummary,
  SurveyResponseListResponse,
  SurveySummaryResponse,
  TokenUsageReport,
  ToolCallListResponse,
  ToolUsageResponse,
} from '../types/api';

// The monitoring views are internal and intentionally public, so they use a client
// without the auth interceptors.
const publicClient = axios.create({ baseURL: API_BASE_URL });

/** Most recent chat turns across all users (newest first). */
export async function fetchChatHistory(limit: number): Promise<ChatHistoryResponse> {
  const { data } = await publicClient.get<ChatHistoryResponse>('/dashboard/chat-history', {
    params: { limit },
  });
  return data;
}

/** Per-minute token usage series plus aggregates for one range. */
export async function fetchTokenUsage(range: string): Promise<TokenUsageReport> {
  const { data } = await publicClient.get<TokenUsageReport>('/dashboard/token-usage', {
    params: { range },
  });
  return data;
}

/** Newest individual tool invocations. */
export async function fetchToolCalls(limit: number): Promise<ToolCallListResponse> {
  const { data } = await publicClient.get<ToolCallListResponse>('/dashboard/tool-calls', {
    params: { limit },
  });
  return data;
}

/** Per-tool call counts, errors, and average duration. */
export async function fetchToolUsage(): Promise<ToolUsageResponse> {
  const { data } = await publicClient.get<ToolUsageResponse>('/dashboard/tool-usage');
  return data;
}

/** CSP solver aggregates plus the most recent solves. */
export async function fetchCspStats(limit = 50): Promise<CspStatsResponse> {
  const { data } = await publicClient.get<CspStatsResponse>('/dashboard/csp-stats', {
    params: { limit },
  });
  return data;
}

export async function fetchEvaluationSummary(): Promise<EvaluationSummary> {
  const { data } = await publicClient.get<EvaluationSummary>('/evaluation/summary');
  return data;
}

export async function fetchEvaluationCases(): Promise<EvaluationCaseListResponse> {
  const { data } = await publicClient.get<EvaluationCaseListResponse>('/evaluation/cases');
  return data;
}

export async function fetchEvaluationRuns(limit = 100): Promise<EvaluationRunListResponse> {
  const { data } = await publicClient.get<EvaluationRunListResponse>('/evaluation/runs', {
    params: { limit },
  });
  return data;
}

export async function fetchSurveySummary(): Promise<SurveySummaryResponse> {
  const { data } = await publicClient.get<SurveySummaryResponse>('/evaluation/survey/summary');
  return data;
}

export async function fetchSurveyResponses(limit = 100): Promise<SurveyResponseListResponse> {
  const { data } = await publicClient.get<SurveyResponseListResponse>(
    '/evaluation/survey/responses',
    { params: { limit } },
  );
  return data;
}
