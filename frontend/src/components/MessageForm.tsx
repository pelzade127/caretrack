import { useState, type FormEvent } from 'react';
import { TextField } from './TextField';
import { Alert } from './Alert';
import { ApiError, errorMessage } from '../api';
import { LIMITS } from '../limits';

interface Props {
  idPrefix: string;
  label: string;
  hint: string;
  submitLabel: string;
  successMessage: string;
  onSubmit: (message: string) => Promise<void>;
}

/** Used for both the customer's message and the agent's customer-visible update. */
export function MessageForm({ idPrefix, label, hint, submitLabel, successMessage, onSubmit }: Props) {
  const [message, setMessage] = useState('');
  const [fieldError, setFieldError] = useState<string>();
  const [formError, setFormError] = useState<string>();
  const [success, setSuccess] = useState(false);
  const [pending, setPending] = useState(false);
  const fieldId = `${idPrefix}-message`;

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setSuccess(false);
    setFormError(undefined);

    const trimmed = message.trim();
    const problem = !trimmed
      ? 'Message is required.'
      : trimmed.length > LIMITS.messageMax
        ? `Message must be at most ${LIMITS.messageMax} characters.`
        : undefined;
    setFieldError(problem);
    if (problem) {
      document.getElementById(fieldId)?.focus();
      return;
    }

    setPending(true);
    try {
      await onSubmit(trimmed);
      setMessage('');
      setSuccess(true);
    } catch (error) {
      if (error instanceof ApiError && error.fieldErrors.message) {
        setFieldError(error.fieldErrors.message);
      } else {
        setFormError(errorMessage(error));
      }
    } finally {
      setPending(false);
    }
  }

  return (
    <form className="stack" onSubmit={handleSubmit} noValidate aria-busy={pending}>
      <TextField
        id={fieldId}
        label={label}
        hint={hint}
        value={message}
        onChange={(v) => {
          setMessage(v);
          setSuccess(false);
        }}
        error={fieldError}
        maxLength={LIMITS.messageMax}
        multiline
        rows={3}
        required
        disabled={pending}
      />
      {formError && <Alert tone="error">{formError}</Alert>}
      {success && <Alert tone="success">{successMessage}</Alert>}
      <div>
        <button type="submit" className="button button--primary" disabled={pending}>
          {pending ? 'Sending…' : submitLabel}
        </button>
      </div>
    </form>
  );
}
