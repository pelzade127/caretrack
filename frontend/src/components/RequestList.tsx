import type { RequestSummary } from '../types';
import { CATEGORY_LABELS, formatDateTime } from '../labels';
import { StatusBadge } from './StatusBadge';

interface Props {
  requests: RequestSummary[];
  selected?: string;
  onSelect: (reference: string) => void;
  showCustomer?: boolean;
  label: string;
}

/** A list of request cards. Each card is a real <button>, so it works with Tab/Enter/Space. */
export function RequestList({ requests, selected, onSelect, showCustomer, label }: Props) {
  return (
    <ul className="request-list" aria-label={label}>
      {requests.map((r) => (
        <li key={r.referenceNumber}>
          <button
            type="button"
            className={`request-card${selected === r.referenceNumber ? ' request-card--selected' : ''}`}
            aria-current={selected === r.referenceNumber ? 'true' : undefined}
            onClick={() => onSelect(r.referenceNumber)}
          >
            <span className="request-card__top">
              <span className="request-card__ref">{r.referenceNumber}</span>
              <StatusBadge status={r.status} />
            </span>
            <span className="request-card__subject">{r.subject}</span>
            <span className="request-card__meta">
              {showCustomer && <>{r.customerName} · </>}
              {CATEGORY_LABELS[r.category]} · Updated{' '}
              <time dateTime={r.updatedAt}>{formatDateTime(r.updatedAt)}</time>
            </span>
          </button>
        </li>
      ))}
    </ul>
  );
}
