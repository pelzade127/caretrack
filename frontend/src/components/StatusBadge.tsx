import type { RequestStatus } from '../types';
import { STATUS_LABELS } from '../labels';

// Each status has a distinct shape AND a text label, so it reads correctly
// without color (color-blind users, grayscale printing, screen readers).
// The icon is decorative (aria-hidden); the text label carries the meaning.
function StatusIcon({ status }: { status: RequestStatus }) {
  const common = { width: 12, height: 12, viewBox: '0 0 12 12', 'aria-hidden': true, focusable: false } as const;
  switch (status) {
    case 'OPEN':
      return (
        <svg {...common}>
          <circle cx="6" cy="6" r="4.5" fill="none" stroke="currentColor" strokeWidth="1.6" />
        </svg>
      );
    case 'IN_PROGRESS':
      return (
        <svg {...common}>
          <circle cx="6" cy="6" r="4.5" fill="none" stroke="currentColor" strokeWidth="1.6" />
          <path d="M6 1.5 A4.5 4.5 0 0 1 6 10.5 Z" fill="currentColor" />
        </svg>
      );
    case 'WAITING_ON_CUSTOMER':
      return (
        <svg {...common}>
          <rect x="2.5" y="2" width="2.4" height="8" rx="0.6" fill="currentColor" />
          <rect x="7.1" y="2" width="2.4" height="8" rx="0.6" fill="currentColor" />
        </svg>
      );
    case 'RESOLVED':
      return (
        <svg {...common}>
          <path d="M2 6.3 L4.8 9 L10 3.2" fill="none" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
      );
  }
}

export function StatusBadge({ status }: { status: RequestStatus }) {
  return (
    <span className={`badge badge--${status.toLowerCase()}`}>
      <StatusIcon status={status} />
      {STATUS_LABELS[status]}
    </span>
  );
}
