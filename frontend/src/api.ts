import type {
  ApiErrorBody,
  CreateRequestInput,
  FieldErrors,
  RequestCategory,
  RequestDetail,
  RequestStatus,
  RequestSummary,
} from './types';

/**
 * Empty by default: requests go to /api on the same origin and the Vite dev
 * server proxies them to Spring Boot. Set VITE_API_BASE_URL to call the
 * backend directly (the backend's dev CORS allows http://localhost:5173).
 */
const BASE_URL: string = import.meta.env.VITE_API_BASE_URL ?? '';

/** An error the UI can show directly. */
export class ApiError extends Error {
  readonly status: number;
  readonly fieldErrors: FieldErrors;

  constructor(status: number, message: string, fieldErrors: FieldErrors = {}) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.fieldErrors = fieldErrors;
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let response: Response;
  try {
    response = await fetch(BASE_URL + path, {
      ...init,
      headers: { Accept: 'application/json', ...(init?.body ? { 'Content-Type': 'application/json' } : {}) },
    });
  } catch {
    throw new ApiError(0, "Can't reach the CareTrack API. Is the backend running on port 8080?");
  }

  if (response.ok) {
    return (await response.json()) as T;
  }

  // Every backend error uses the ApiErrorResponse shape; fall back gracefully if not.
  let body: Partial<ApiErrorBody> = {};
  try {
    body = (await response.json()) as ApiErrorBody;
  } catch {
    // Non-JSON error (e.g. proxy error page) — ignore the body.
  }
  const fieldErrors: FieldErrors = {};
  for (const fe of body.fieldErrors ?? []) {
    fieldErrors[fe.field] ??= fe.message; // first message per field
  }
  const message =
    body.message ?? (response.status >= 500 ? 'The server had a problem. Please try again.' : 'Request failed.');
  throw new ApiError(response.status, message, fieldErrors);
}

const post = <T>(path: string, body: unknown) => request<T>(path, { method: 'POST', body: JSON.stringify(body) });
const ref = (reference: string) => encodeURIComponent(reference);

export const customerApi = {
  list: () => request<RequestSummary[]>('/api/customer/requests'),
  get: (reference: string) => request<RequestDetail>(`/api/customer/requests/${ref(reference)}`),
  create: (input: CreateRequestInput) => post<RequestDetail>('/api/customer/requests', input),
  addMessage: (reference: string, message: string) =>
    post<RequestDetail>(`/api/customer/requests/${ref(reference)}/messages`, { message }),
};

export const agentApi = {
  list: (filters: { status?: RequestStatus | ''; category?: RequestCategory | '' }) => {
    const params = new URLSearchParams();
    if (filters.status) params.set('status', filters.status);
    if (filters.category) params.set('category', filters.category);
    const query = params.toString();
    return request<RequestSummary[]>(`/api/agent/requests${query ? `?${query}` : ''}`);
  },
  get: (reference: string) => request<RequestDetail>(`/api/agent/requests/${ref(reference)}`),
  addUpdate: (reference: string, message: string) =>
    post<RequestDetail>(`/api/agent/requests/${ref(reference)}/updates`, { message }),
  changeStatus: (reference: string, newStatus: RequestStatus, explanation: string) =>
    post<RequestDetail>(`/api/agent/requests/${ref(reference)}/status-changes`, { newStatus, explanation }),
};

export function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : 'Something went wrong.';
}
