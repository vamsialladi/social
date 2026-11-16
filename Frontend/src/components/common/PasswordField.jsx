import { useState } from 'react';
import { CheckCircle2, XCircle, Eye, EyeOff } from 'lucide-react';
import { getPasswordValidationMessage } from './passwordValidation';
import './PasswordField.css';

export default function PasswordField({
  id,
  name,
  label,
  headerAction = null,
  value = '',
  onChange,
  placeholder = '',
  autoComplete = 'current-password',
  required = true,
  disabled = false,
  inputClassName = 'form-control',
  showValidation = false,
  isConfirm = false,
  compareValue = '',
  error = '',
  maxLength = 25,
}) {
  const [visible, setVisible] = useState(false);
  const [touched, setTouched] = useState(false);

  const validationMessage = showValidation && !isConfirm && (touched || value.length > 0)
    ? getPasswordValidationMessage(value)
    : '';
  const passwordsMatch = isConfirm ? value.length > 0 && value === compareValue : true;
  const confirmMessage = isConfirm && (touched || value.length > 0)
    ? (!value
      ? 'Confirm password is required'
      : (passwordsMatch ? 'Passwords match' : 'Password and confirm password do not match'))
    : '';

  const effectiveError = error || validationMessage;

  return (
    <div className="pw-field-group">
      <div className="pw-field-header">
        <label className="pw-field-label" htmlFor={id}>
          {label}
        </label>
        {headerAction}
      </div>

      <div className="pw-input-wrap">
        <input
          id={id}
          name={name}
          type={visible ? 'text' : 'password'}
          className={`${inputClassName} pw-input`.trim()}
          value={value}
          onChange={(event) => {
            if (!touched) {
              setTouched(true);
            }
            onChange(event);
          }}
          onBlur={() => setTouched(true)}
          placeholder={placeholder}
          autoComplete={autoComplete}
          required={required}
          disabled={disabled}
          maxLength={maxLength}
          aria-invalid={Boolean(effectiveError || (isConfirm && confirmMessage && !passwordsMatch))}
        />
        <button
          type="button"
          className="pw-toggle-btn"
          onClick={() => setVisible((prev) => !prev)}
          aria-label={visible ? `Hide ${label || 'password'}` : `Show ${label || 'password'}`}
          title={visible ? 'Hide password' : 'Show password'}
        >
          {visible ? <EyeOff size={18} /> : <Eye size={18} />}
        </button>
      </div>

      {effectiveError && (
        <div className="pw-inline-feedback is-invalid" role="alert">
          <XCircle size={15} />
          <span>{effectiveError}</span>
        </div>
      )}

      {isConfirm && confirmMessage && (
        <div
          className={`pw-inline-feedback ${passwordsMatch ? 'is-valid' : 'is-invalid'}`}
          role="status"
          aria-live="polite"
        >
          {passwordsMatch ? <CheckCircle2 size={15} /> : <XCircle size={15} />}
          <span>{confirmMessage}</span>
        </div>
      )}
    </div>
  );
}
