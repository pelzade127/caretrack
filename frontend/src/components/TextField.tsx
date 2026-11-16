import type { ChangeEvent } from 'react';

interface Props {
  id: string;
  label: string;
  value: string;
  onChange: (value: string) => void;
  error?: string;
  hint?: string;
  maxLength?: number;
  multiline?: boolean;
  rows?: number;
  required?: boolean;
  disabled?: boolean;
  autoComplete?: string;
}

/**
 * Labelled input or textarea with an error message and character counter, wired up
 * with aria-invalid / aria-describedby so assistive tech reads the error with the field.
 */
export function TextField({
  id,
  label,
  value,
  onChange,
  error,
  hint,
  maxLength,
  multiline,
  rows = 4,
  required,
  disabled,
  autoComplete,
}: Props) {
  const hintId = hint ? `${id}-hint` : undefined;
  const errorId = error ? `${id}-error` : undefined;
  const countId = maxLength ? `${id}-count` : undefined;
  const describedBy = [hintId, errorId, countId].filter(Boolean).join(' ') || undefined;

  const common = {
    id,
    name: id,
    value,
    required,
    disabled,
    // Allow typing past the limit so the counter and message explain the problem
    // rather than input silently stopping.
    'aria-invalid': error ? true : undefined,
    'aria-describedby': describedBy,
    onChange: (e: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => onChange(e.target.value),
  };

  const over = maxLength !== undefined && value.length > maxLength;

  return (
    <div className={`field${error ? ' field--invalid' : ''}`}>
      <label htmlFor={id} className="field__label">
        {label}
        {required && <span className="field__required"> (required)</span>}
      </label>
      {hint && (
        <p id={hintId} className="field__hint">
          {hint}
        </p>
      )}
      {multiline ? <textarea {...common} rows={rows} /> : <input {...common} type="text" autoComplete={autoComplete} />}
      <div className="field__footer">
        {error ? (
          <p id={errorId} className="field__error">
            {error}
          </p>
        ) : (
          <span />
        )}
        {maxLength !== undefined && (
          <span id={countId} className={`field__count${over ? ' field__count--over' : ''}`}>
            {value.length}/{maxLength}
          </span>
        )}
      </div>
    </div>
  );
}
