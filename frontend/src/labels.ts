import type { RequestCategory, RequestStatus } from './types';

export const CATEGORY_LABELS: Record<RequestCategory, string> = {
  BILLING: 'Billing',
  ACCOUNT_ACCESS: 'Account access',
  SERVICE_ISSUE: 'Service issue',
  ORDER_SUPPORT: 'Order support',
};

export const CATEGORIES = Object.keys(CATEGORY_LABELS) as RequestCategory[];

export const STATUS_LABELS: Record<RequestStatus, string> = {
  OPEN: 'Open',
  IN_PROGRESS: 'In progress',
  WAITING_ON_CUSTOMER: 'Waiting on customer',
  RESOLVED: 'Resolved',
};

export const STATUSES = Object.keys(STATUS_LABELS) as RequestStatus[];

/** Shown to the customer under the status badge. */
export const STATUS_HINTS_FOR_CUSTOMER: Record<RequestStatus, string> = {
  OPEN: 'We have your request and will pick it up soon.',
  IN_PROGRESS: 'An agent is working on this.',
  WAITING_ON_CUSTOMER: 'We need a reply from you. Add a message below.',
  RESOLVED: 'This request is resolved. Add a message if something is still wrong.',
};

const dateTime = new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' });

export function formatDateTime(iso: string): string {
  return dateTime.format(new Date(iso));
}
