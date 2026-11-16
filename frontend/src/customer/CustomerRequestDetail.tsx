import { useCallback, useEffect, useRef } from 'react';
import { customerApi } from '../api';
import { useLoader, useScrollIntoViewOnNarrow } from '../hooks';
import { STATUS_HINTS_FOR_CUSTOMER } from '../labels';
import { Alert } from '../components/Alert';
import { MessageForm } from '../components/MessageForm';
import { RequestHeader } from '../components/RequestHeader';
import { Timeline } from '../components/Timeline';

interface Props {
  reference: string;
  onChanged: () => void;
  focusOnLoad: boolean;
}

export function CustomerRequestDetail({ reference, onChanged, focusOnLoad }: Props) {
  const load = useCallback(() => customerApi.get(reference), [reference]);
  const { data: request, setData, error, loading, reload } = useLoader(load);
  const headingRef = useRef<HTMLHeadingElement>(null);
  const articleRef = useRef<HTMLElement>(null);
  useScrollIntoViewOnNarrow(articleRef, Boolean(request));

  useEffect(() => {
    if (request && focusOnLoad) headingRef.current?.focus();
  }, [request?.referenceNumber, focusOnLoad]); // focus once per request, not on every update

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
    <article className="panel detail" aria-labelledby="customer-detail-title" ref={articleRef}>
      <span id="customer-detail-title" className="visually-hidden">
        Request {request.referenceNumber}
      </span>
      <RequestHeader request={request} statusHint={STATUS_HINTS_FOR_CUSTOMER[request.status]} headingRef={headingRef} />

      <section aria-labelledby="customer-history-title">
        <h3 id="customer-history-title" className="section-title">
          History
        </h3>
        <Timeline events={request.history} />
      </section>

      <section aria-labelledby="customer-reply-title" className="detail__action">
        <h3 id="customer-reply-title" className="section-title">
          Add a message
        </h3>
        <MessageForm
          key={request.referenceNumber}
          idPrefix="customer"
          label="Your message"
          hint="Sending a message adds it to the history. It does not change the request's status."
          submitLabel="Send message"
          successMessage="Message added to the history."
          onSubmit={async (message) => {
            const updated = await customerApi.addMessage(request.referenceNumber, message);
            setData(updated);
            onChanged();
          }}
        />
      </section>
    </article>
  );
}
