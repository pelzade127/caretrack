import { useCallback, useState } from 'react';
import { agentApi } from '../api';
import { useLoader } from '../hooks';
import { CATEGORIES, CATEGORY_LABELS, STATUSES, STATUS_LABELS } from '../labels';
import type { RequestCategory, RequestStatus } from '../types';
import { Alert } from '../components/Alert';
import { RequestList } from '../components/RequestList';
import { AgentRequestDetail } from './AgentRequestDetail';

export function AgentView() {
  const [status, setStatus] = useState<RequestStatus | ''>('');
  const [category, setCategory] = useState<RequestCategory | ''>('');
  const [selected, setSelected] = useState<string>();

  const loadList = useCallback(() => agentApi.list({ status, category }), [status, category]);
  const { data: requests, error, loading, reload } = useLoader(loadList);
  const filtered = status !== '' || category !== '';

  return (
    <div className="layout">
      <aside className="layout__side" aria-labelledby="agent-list-title">
        <h2 id="agent-list-title" className="panel__title">
          All demo requests
        </h2>

        <div className="filters" role="group" aria-label="Filter requests">
          <div className="field">
            <label htmlFor="filter-status" className="field__label">
              Status
            </label>
            <select id="filter-status" value={status} onChange={(e) => setStatus(e.target.value as RequestStatus | '')}>
              <option value="">All statuses</option>
              {STATUSES.map((s) => (
                <option key={s} value={s}>
                  {STATUS_LABELS[s]}
                </option>
              ))}
            </select>
          </div>
          <div className="field">
            <label htmlFor="filter-category" className="field__label">
              Category
            </label>
            <select
              id="filter-category"
              value={category}
              onChange={(e) => setCategory(e.target.value as RequestCategory | '')}
            >
              <option value="">All categories</option>
              {CATEGORIES.map((c) => (
                <option key={c} value={c}>
                  {CATEGORY_LABELS[c]}
                </option>
              ))}
            </select>
          </div>
        </div>

        <p className="muted result-count" role="status" aria-live="polite">
          {loading ? 'Loading…' : requests ? `${requests.length} request${requests.length === 1 ? '' : 's'}` : ''}
        </p>

        {error && (
          <Alert tone="error">
            {error}{' '}
            <button type="button" className="link-button" onClick={reload}>
              Retry
            </button>
          </Alert>
        )}
        {requests && requests.length === 0 && (
          <p className="empty">
            {filtered ? 'No requests match these filters.' : 'There are no requests yet.'}
            {filtered && (
              <>
                {' '}
                <button
                  type="button"
                  className="link-button"
                  onClick={() => {
                    setStatus('');
                    setCategory('');
                  }}
                >
                  Clear filters
                </button>
              </>
            )}
          </p>
        )}
        {requests && requests.length > 0 && (
          <RequestList
            requests={requests}
            selected={selected}
            onSelect={setSelected}
            showCustomer
            label="Demo requests"
          />
        )}
      </aside>

      <main className="layout__main" id="main">
        {selected ? (
          <AgentRequestDetail key={selected} reference={selected} onChanged={reload} />
        ) : (
          <div className="panel placeholder">
            <h2 className="panel__title">Select a request</h2>
            <p className="muted">
              Choose a request from the list to see its history, post a customer-visible update, or change its status.
            </p>
          </div>
        )}
      </main>
    </div>
  );
}
