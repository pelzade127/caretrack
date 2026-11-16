import type { Ref } from 'react';
import type { RequestDetail } from '../types';
import { CATEGORY_LABELS, formatDateTime } from '../labels';
import { StatusBadge } from './StatusBadge';

/** Subject, reference, status and key facts shared by the customer and agent detail views. */
interface Props {
  request: RequestDetail;
  statusHint?: string;
  /** Focus target after navigation, so keyboard and screen-reader users land on the new content. */
  headingRef?: Ref<HTMLHeadingElement>;
}

export function RequestHeader({ request, statusHint, headingRef }: Props) {
    return (
      <header className="detail__header">
        <p className="detail__reference">
          Reference <strong>{request.referenceNumber}</strong>
        </p>
        <h2 ref={headingRef} tabIndex={-1} className="detail__title">
          {request.subject}
        </h2>
        <div className="detail__status">
          <StatusBadge status={request.status} />
          {statusHint && <span className="detail__hint">{statusHint}</span>}
        </div>
        <dl className="facts">
          <div>
            <dt>Category</dt>
            <dd>{CATEGORY_LABELS[request.category]}</dd>
          </div>
          <div>
            <dt>Customer</dt>
            <dd>{request.customerName}</dd>
          </div>
          <div>
            <dt>Submitted</dt>
            <dd>
              <time dateTime={request.createdAt}>{formatDateTime(request.createdAt)}</time>
            </dd>
          </div>
          <div>
            <dt>Last updated</dt>
            <dd>
              <time dateTime={request.updatedAt}>{formatDateTime(request.updatedAt)}</time>
            </dd>
          </div>
        </dl>
        <h3 className="section-title">Description</h3>
        <p className="detail__description">{request.description}</p>
      </header>
    );
}
