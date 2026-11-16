import { useState, type FormEvent } from 'react';
import { ApiError, errorMessage } from '../api';
import { LIMITS } from '../limits';
import { STATUS_LABELS } from '../labels';
import type { RequestStatus } from '../types';
import { TextField } from '../components/TextField';
import { Alert } from '../components/Alert';

interface Props {
  currentStatus: RequestStatus;
  /** Comes from the backend response, so the UI never keeps its own copy of the rules. */
  allowedNextStatuses: RequestStatus[];
  onSubmit: (newStatus: RequestStatus, explanation: string) => Promise<void>;
}

function optionLabel(current: RequestStatus, next: RequestStatus) {
  return current === 'RESOLVED' && next === 'IN_PROGRESS'
    ? `Reopen → ${STATUS_LABELS[next]}`
    : STATUS_LABELS[next];
}

export function StatusChangeForm({ currentStatus, allowedNextStatuses, onSubmit }: Props) {
  const [newStatus, setNewStatus] = useState<RequestStatus | ''>('');
  const [explanation, setExplanation] = useState('');
  const [errors, setErrors] = useState<{ newStatus?: string; explanation?: string }>({});
  const [formError, setFormError] = useState<string>();
  const [success, setSuccess] = useState<string>();
  const [pending, setPending] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setSuccess(undefined);
    setFormError(undefined);

    const trimmed = explanation.trim();
    const found: typeof errors = {};
    if (!newStatus) found.newStatus = 'Choose the new status.';
    if (!trimmed) found.explanation = 'An explanation is required for every status change.';
    else if (trimmed.length < LIMITS.explanationMin || trimmed.length > LIMITS.explanationMax)
      found.explanation = `Explanation must be ${LIMITS.explanationMin}–${LIMITS.explanationMax} characters.`;
    setErrors(found);
    if (found.newStatus) return document.getElementById('status-new')?.focus();
    if (found.explanation) return document.getElementById('status-explanation')?.focus();
    if (!newStatus) return;

    setPending(true);
    try {
      await onSubmit(newStatus, trimmed);
      setSuccess(`Status changed to ${STATUS_LABELS[newStatus]}.`);
      setNewStatus('');
      setExplanation('');
    } catch (error) {
      if (error instanceof ApiError && Object.keys(error.fieldErrors).length > 0) {
        setErrors(error.fieldErrors);
      } else {
        // e.g. 409 Conflict when the rules reject the transition
        setFormError(errorMessage(error));
      }
    } finally {
      setPending(false);
    }
  }

  return (
    <form className="stack" onSubmit={handleSubmit} noValidate aria-busy={pending}>
      <div className={`field${errors.newStatus ? ' field--invalid' : ''}`}>
        <label htmlFor="status-new" className="field__label">
          New status<span className="field__required"> (required)</span>
        </label>
        <p id="status-new-hint" className="field__hint">
          Currently {STATUS_LABELS[currentStatus]}. Only allowed next statuses are listed; the server enforces the
          same rules.
        </p>
        <select
          id="status-new"
          value={newStatus}
          onChange={(e) => {
            setNewStatus(e.target.value as RequestStatus | '');
            setSuccess(undefined);
          }}
          aria-invalid={errors.newStatus ? true : undefined}
          aria-describedby={`status-new-hint${errors.newStatus ? ' status-new-error' : ''}`}
          disabled={pending}
          required
        >
          <option value="">Choose…</option>
          {allowedNextStatuses.map((s) => (
            <option key={s} value={s}>
              {optionLabel(currentStatus, s)}
            </option>
          ))}
        </select>
        {errors.newStatus && (
          <p id="status-new-error" className="field__error">
            {errors.newStatus}
          </p>
        )}
      </div>

      <TextField
        id="status-explanation"
        label="Explanation"
        hint="Recorded in the history and visible to the customer."
        value={explanation}
        onChange={(v) => {
          setExplanation(v);
          setSuccess(undefined);
        }}
        error={errors.explanation}
        maxLength={LIMITS.explanationMax}
        multiline
        rows={2}
        required
        disabled={pending}
      />

      {formError && <Alert tone="error">{formError}</Alert>}
      {success && <Alert tone="success">{success}</Alert>}

      <div>
        <button type="submit" className="button button--primary" disabled={pending}>
          {pending ? 'Saving…' : 'Change status'}
        </button>
      </div>
    </form>
  );
}
