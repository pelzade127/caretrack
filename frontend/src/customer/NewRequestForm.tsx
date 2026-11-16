import { useState, type FormEvent } from 'react';
import { ApiError, customerApi, errorMessage } from '../api';
import { CATEGORIES, CATEGORY_LABELS } from '../labels';
import { LIMITS } from '../limits';
import type { CreateRequestInput, FieldErrors, RequestDetail } from '../types';
import { TextField } from '../components/TextField';
import { Alert } from '../components/Alert';

const DEMO_CUSTOMER_NAME = 'Jordan Rivera';
const FIELD_ORDER = ['customerName', 'category', 'subject', 'description'] as const;

/** Client-side checks that mirror the backend rules, for faster feedback. The backend still decides. */
function validate(input: CreateRequestInput): FieldErrors {
  const errors: FieldErrors = {};
  const name = input.customerName.trim();
  const subject = input.subject.trim();
  const description = input.description.trim();

  if (!name) errors.customerName = 'Customer name is required.';
  else if (name.length > LIMITS.customerNameMax)
    errors.customerName = `Customer name must be at most ${LIMITS.customerNameMax} characters.`;

  if (!input.category) errors.category = 'Choose a category.';

  if (!subject) errors.subject = 'Subject is required.';
  else if (subject.length < LIMITS.subjectMin || subject.length > LIMITS.subjectMax)
    errors.subject = `Subject must be ${LIMITS.subjectMin}–${LIMITS.subjectMax} characters.`;

  if (!description) errors.description = 'Description is required.';
  else if (description.length < LIMITS.descriptionMin || description.length > LIMITS.descriptionMax)
    errors.description = `Description must be ${LIMITS.descriptionMin}–${LIMITS.descriptionMax} characters.`;

  return errors;
}

export function NewRequestForm({ onCreated }: { onCreated: (created: RequestDetail) => void }) {
  const [input, setInput] = useState<CreateRequestInput>({
    customerName: DEMO_CUSTOMER_NAME,
    category: '',
    subject: '',
    description: '',
  });
  const [errors, setErrors] = useState<FieldErrors>({});
  const [formError, setFormError] = useState<string>();
  const [pending, setPending] = useState(false);

  const set = (field: keyof CreateRequestInput) => (value: string) => {
    setInput((prev) => ({ ...prev, [field]: value }));
    if (errors[field]) setErrors((prev) => ({ ...prev, [field]: undefined }));
  };

  function focusFirstError(found: FieldErrors) {
    const first = FIELD_ORDER.find((f) => found[f]);
    if (first) document.getElementById(`new-${first}`)?.focus();
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setFormError(undefined);

    const found = validate(input);
    setErrors(found);
    if (Object.keys(found).length > 0) {
      focusFirstError(found);
      return;
    }

    setPending(true);
    try {
      const created = await customerApi.create(input);
      onCreated(created);
    } catch (error) {
      if (error instanceof ApiError && Object.keys(error.fieldErrors).length > 0) {
        setErrors(error.fieldErrors);
        focusFirstError(error.fieldErrors);
      } else {
        setFormError(errorMessage(error));
      }
    } finally {
      setPending(false);
    }
  }

  return (
    <section className="panel" aria-labelledby="new-request-title">
      <h2 id="new-request-title" className="panel__title">
        Submit a support request
      </h2>
      <p className="muted">Use synthetic details only. This demo stores what you type in a local database.</p>

      <form className="stack" onSubmit={handleSubmit} noValidate aria-busy={pending}>
        <TextField
          id="new-customerName"
          label="Your name"
          hint="Pre-filled for the demo customer. Any made-up name works."
          value={input.customerName}
          onChange={set('customerName')}
          error={errors.customerName}
          maxLength={LIMITS.customerNameMax}
          required
          disabled={pending}
          autoComplete="off"
        />

        <div className={`field${errors.category ? ' field--invalid' : ''}`}>
          <label htmlFor="new-category" className="field__label">
            Category<span className="field__required"> (required)</span>
          </label>
          <select
            id="new-category"
            value={input.category}
            onChange={(e) => set('category')(e.target.value)}
            aria-invalid={errors.category ? true : undefined}
            aria-describedby={errors.category ? 'new-category-error' : undefined}
            required
            disabled={pending}
          >
            <option value="">Choose a category…</option>
            {CATEGORIES.map((c) => (
              <option key={c} value={c}>
                {CATEGORY_LABELS[c]}
              </option>
            ))}
          </select>
          {errors.category && (
            <p id="new-category-error" className="field__error">
              {errors.category}
            </p>
          )}
        </div>

        <TextField
          id="new-subject"
          label="Subject"
          hint="A short summary, e.g. “Calls dropping at home”."
          value={input.subject}
          onChange={set('subject')}
          error={errors.subject}
          maxLength={LIMITS.subjectMax}
          required
          disabled={pending}
          autoComplete="off"
        />

        <TextField
          id="new-description"
          label="Description"
          hint="What happened, and what you've already tried."
          value={input.description}
          onChange={set('description')}
          error={errors.description}
          maxLength={LIMITS.descriptionMax}
          multiline
          rows={5}
          required
          disabled={pending}
        />

        {formError && <Alert tone="error">{formError}</Alert>}

        <div>
          <button type="submit" className="button button--primary" disabled={pending}>
            {pending ? 'Submitting…' : 'Submit request'}
          </button>
        </div>
      </form>
    </section>
  );
}
