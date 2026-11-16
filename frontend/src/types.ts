// Mirrors the backend DTOs in com.caretrack.dto. Keep the two in sync.

export type RequestStatus = 'OPEN' | 'IN_PROGRESS' | 'WAITING_ON_CUSTOMER' | 'RESOLVED';
export type RequestCategory = 'BILLING' | 'ACCOUNT_ACCESS' | 'SERVICE_ISSUE' | 'ORDER_SUPPORT';
export type EventType = 'CREATED' | 'CUSTOMER_MESSAGE' | 'AGENT_UPDATE' | 'STATUS_CHANGE' | 'REOPENED';
export type ActorType = 'CUSTOMER' | 'AGENT';

export interface RequestSummary {
  referenceNumber: string;
  customerName: string;
  category: RequestCategory;
  subject: string;
  status: RequestStatus;
  createdAt: string;
  updatedAt: string;
}

export interface HistoryEvent {
  id: number;
  type: EventType;
  actorType: ActorType;
  actorName: string;
  message: string | null;
  previousStatus: RequestStatus | null;
  newStatus: RequestStatus | null;
  createdAt: string;
}

export interface RequestDetail extends RequestSummary {
  description: string;
  allowedNextStatuses: RequestStatus[];
  history: HistoryEvent[];
}

export interface CreateRequestInput {
  customerName: string;
  category: RequestCategory | '';
  subject: string;
  description: string;
}

export interface FieldErrorItem {
  field: string;
  message: string;
}

export interface ApiErrorBody {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  fieldErrors: FieldErrorItem[];
}

export type FieldErrors = Partial<Record<string, string>>;
