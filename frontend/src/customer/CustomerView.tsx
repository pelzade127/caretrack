import { useCallback, useState } from 'react';
import { customerApi } from '../api';
import { useLoader } from '../hooks';
import type { RequestDetail } from '../types';
import { Alert } from '../components/Alert';
import { RequestList } from '../components/RequestList';
import { NewRequestForm } from './NewRequestForm';
import { CustomerRequestDetail } from './CustomerRequestDetail';

export function CustomerView() {
  const loadList = useCallback(() => customerApi.list(), []);
  const { data: requests, error, loading, reload } = useLoader(loadList);
  const [selected, setSelected] = useState<string>();
  const [justCreated, setJustCreated] = useState<string>();

  function handleCreated(created: RequestDetail) {
    setJustCreated(created.referenceNumber);
    setSelected(created.referenceNumber);
    reload();
  }

  function select(reference: string) {
    setJustCreated(undefined);
    setSelected(reference);
  }

  return (
    <div className="layout">
      <aside className="layout__side" aria-labelledby="customer-list-title">
        <div className="side-header">
          <h2 id="customer-list-title" className="panel__title">
            Your requests
          </h2>
          <button
            type="button"
            className="button button--primary button--small"
            onClick={() => {
              setSelected(undefined);
              setJustCreated(undefined);
            }}
            aria-pressed={selected === undefined}
          >
            + New request
          </button>
        </div>

        {loading && !requests && (
          <p className="muted" role="status">
            Loading your requests…
          </p>
        )}
        {error && (
          <Alert tone="error">
            {error}{' '}
            <button type="button" className="link-button" onClick={reload}>
              Retry
            </button>
          </Alert>
        )}
        {requests && requests.length === 0 && (
          <p className="empty">You haven't submitted any requests yet. Use the form to create your first one.</p>
        )}
        {requests && requests.length > 0 && (
          <RequestList requests={requests} selected={selected} onSelect={select} label="Your support requests" />
        )}
      </aside>

      <main className="layout__main" id="main">
        {justCreated && (
          <Alert tone="success" onDismiss={() => setJustCreated(undefined)}>
            Request <strong>{justCreated}</strong> submitted. Keep this reference number to follow up.
          </Alert>
        )}
        {selected ? (
          <CustomerRequestDetail
            key={selected}
            reference={selected}
            onChanged={reload}
            focusOnLoad={justCreated === selected}
          />
        ) : (
          <NewRequestForm onCreated={handleCreated} />
        )}
      </main>
    </div>
  );
}
