import { useCallback, useRef } from 'react';
import { agentApi } from '../api';
import { useLoader, useScrollIntoViewOnNarrow } from '../hooks';
import { Alert } from '../components/Alert';
import { MessageForm } from '../components/MessageForm';
import { RequestHeader } from '../components/RequestHeader';
import { Timeline } from '../components/Timeline';
import { StatusChangeForm } from './StatusChangeForm';

export function AgentRequestDetail({ reference, onChanged }: { reference: string; onChanged: () => void }) {
  const load = useCallback(() => agentApi.get(reference), [reference]);
  const { data: request, setData, error, loading, reload } = useLoader(load);
  const articleRef = useRef<HTMLElement>(null);
  useScrollIntoViewOnNarrow(articleRef, Boolean(request));

  if (loading && !request) return <p className="panel muted" role="status">Loading request {reference}…</p>;
  if (error && !request) {
    return (
      <div className="panel stack">
        <Alert tone="error">{error}</Alert>
        <div>
          <button type="button" className="button" onClick={reload}>
            Try again
          </button>
        </div>
      </div>
    );
  }
  if (!request) return null;

  return (
    <article className="panel detail" ref={articleRef}>
      <RequestHeader request={request} />

      <div className="agent-actions">
        <section aria-labelledby="agent-status-title" className="detail__action">
          <h3 id="agent-status-title" className="section-title">
            Change status
          </h3>
          <StatusChangeForm
            key={request.referenceNumber}
            currentStatus={request.status}
            allowedNextStatuses={request.allowedNextStatuses}
            onSubmit={async (newStatus, explanation) => {
              setData(await agentApi.changeStatus(request.referenceNumber, newStatus, explanation));
              onChanged();
            }}
          />
        </section>

        <section aria-labelledby="agent-update-title" className="detail__action">
          <h3 id="agent-update-title" className="section-title">
            Customer-visible update
          </h3>
          <MessageForm
            key={request.referenceNumber}
            idPrefix="agent"
            label="Update for the customer"
            hint="Shown in the customer's history. Does not change the status."
            submitLabel="Post update"
            successMessage="Update posted to the history."
            onSubmit={async (message) => {
              setData(await agentApi.addUpdate(request.referenceNumber, message));
              onChanged();
            }}
          />
        </section>
      </div>

      <section aria-labelledby="agent-history-title">
        <h3 id="agent-history-title" className="section-title">
          History
        </h3>
        <Timeline events={request.history} />
      </section>
    </article>
  );
}
