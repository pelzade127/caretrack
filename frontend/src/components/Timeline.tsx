import type { HistoryEvent } from '../types';
import { STATUS_LABELS, formatDateTime } from '../labels';

function title(event: HistoryEvent): string {
  switch (event.type) {
    case 'CREATED':
      return 'Request submitted';
    case 'CUSTOMER_MESSAGE':
      return 'Customer message';
    case 'AGENT_UPDATE':
      return 'Update from support';
    case 'STATUS_CHANGE':
      return `Status changed: ${label(event.previousStatus)} → ${label(event.newStatus)}`;
    case 'REOPENED':
      return `Reopened: ${label(event.previousStatus)} → ${label(event.newStatus)}`;
  }
}

function label(status: HistoryEvent['newStatus']): string {
  return status ? STATUS_LABELS[status] : '—';
}

/** Chronological history, oldest first, exactly as the backend returns it. */
export function Timeline({ events }: { events: HistoryEvent[] }) {
  return (
    <ol className="timeline">
      {events.map((event) => (
        <li key={event.id} className={`timeline__item timeline__item--${event.type.toLowerCase()}`}>
          <div className="timeline__marker" aria-hidden="true" />
          <div className="timeline__content">
            <p className="timeline__title">{title(event)}</p>
            <p className="timeline__meta">
              {event.actorName} · {event.actorType === 'AGENT' ? 'Agent' : 'Customer'} ·{' '}
              <time dateTime={event.createdAt}>{formatDateTime(event.createdAt)}</time>
            </p>
            {event.message && (
              <p className="timeline__message">
                {(event.type === 'STATUS_CHANGE' || event.type === 'REOPENED') && (
                  <span className="timeline__reason">Reason: </span>
                )}
                {event.message}
              </p>
            )}
          </div>
        </li>
      ))}
    </ol>
  );
}
