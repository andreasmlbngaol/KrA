import axios from 'axios';
import { API_BASE_URL } from './http';
import type { TokenUsageReport } from '../types/api';
// Dedicated client WITHOUT auth interceptors: the monitoring dashboard is
// intentionally public for now (internal use), so it must work without a login.
const publicClient = axios.create({ baseURL: API_BASE_URL });

export async function fetchTokenUsage(range: string): Promise<TokenUsageReport> {
  const { data } = await publicClient.get<TokenUsageReport>('/dashboard/token-usage', {
    params: { range },
  });
  return data;
}
