import type { ReactNode } from 'react';

type Tone = 'error' | 'success' | 'info';

/**
 * role="alert" makes screen readers announce errors immediately;
 * role="status" announces success/info politely.
 */
export function Alert({ tone, children, onDismiss }: { tone: Tone; children: ReactNode; onDismiss?: () => void }) {
  return (
    <div className={`alert alert--${tone}`} role={tone === 'error' ? 'alert' : 'status'}>
      <span className="alert__label">{tone === 'error' ? 'Error:' : tone === 'success' ? 'Done:' : 'Note:'}</span>
      <div className="alert__body">{children}</div>
      {onDismiss && (
        <button type="button" className="alert__dismiss" onClick={onDismiss} aria-label="Dismiss message">
          ×
        </button>
      )}
    </div>
  );
}
