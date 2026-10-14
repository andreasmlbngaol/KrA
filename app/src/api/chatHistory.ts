import axios from 'axios';
import { API_BASE_URL } from './http';
import type { ChatHistoryResponse } from '../types/api';

// Dedicated client WITHOUT auth interceptors: the monitoring views are intentionally
// public for now (internal use), matching the token-usage dashboard.
const publicClient = axios.create({ baseURL: API_BASE_URL });

/** Fetches the most recent chat turns across all users (newest first). */
export async function fetchChatHistory(limit: number): Promise<ChatHistoryResponse> {
  const { data } = await publicClient.get<ChatHistoryResponse>('/dashboard/chat-history', {
    params: { limit },
  });
  return data;
}
