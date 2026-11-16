import { useEffect, useState } from 'react';
import { CustomerView } from './customer/CustomerView';
import { AgentView } from './agent/AgentView';

type View = 'customer' | 'agent';

function viewFromHash(): View {
  return window.location.hash === '#agent' ? 'agent' : 'customer';
}

export function App() {
  const [view, setView] = useState<View>(viewFromHash);

  // Keep the chosen view in the URL (#agent) so a reload stays on the same view.
  useEffect(() => {
    const onHashChange = () => setView(viewFromHash());
    window.addEventListener('hashchange', onHashChange);
    return () => window.removeEventListener('hashchange', onHashChange);
  }, []);

  function choose(next: View) {
    window.location.hash = next === 'agent' ? 'agent' : '';
    setView(next);
  }

  return (
    <>
      <a href="#main" className="skip-link">
        Skip to main content
      </a>
      <header className="app-header">
        <div className="app-header__inner">
          <div className="brand">
            <span className="brand__mark" aria-hidden="true">
              ✓
            </span>
            <span>
              <span className="brand__name">CareTrack</span>
              <span className="brand__tag">Support request tracker</span>
            </span>
          </div>

          <div className="view-switch" role="group" aria-label="Choose a view">
            <button
              type="button"
              className="view-switch__option"
              aria-pressed={view === 'customer'}
              onClick={() => choose('customer')}
            >
              Customer view
            </button>
            <button
              type="button"
              className="view-switch__option"
              aria-pressed={view === 'agent'}
              onClick={() => choose('agent')}
            >
              Demo Agent View
            </button>
          </div>
        </div>
      </header>

      <div className="demo-banner" role="note">
        <div className="demo-banner__inner">
          {view === 'customer' ? (
            <>
              <strong>Demo customer view:</strong> no sign-in. Everything here belongs to one fixed demo customer
              account (Jordan Rivera), and anyone using this demo sees the same data.
            </>
          ) : (
            <>
              <strong>Demo Agent View:</strong> acting as “Demo Agent”. This view is as public as the customer view;
              switching views is a demo convenience, not access control.
            </>
          )}
        </div>
      </div>

      <div className="app-body">{view === 'customer' ? <CustomerView /> : <AgentView />}</div>

      <footer className="app-footer">
        CareTrack is a fictional, independent portfolio demo with synthetic data. It is not affiliated with any carrier.
      </footer>
    </>
  );
}
