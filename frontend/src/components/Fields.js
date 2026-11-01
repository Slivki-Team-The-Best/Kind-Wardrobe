import { useId } from 'react';
import { formatPhone, phoneDigits } from '../utils/format';
import './ui.css';

export function PageLoader() {
  return (
    <div className="page-loader">
      <div className="spinner" role="status" aria-label="Загрузка" />
    </div>
  );
}

export function Field({ label, required, note, error, hint, hintRight, className = '', children }) {
  const id = useId();
  const control = typeof children === 'function' ? children(id) : children;
  return (
    <div className={`field ${className}`}>
      {label && (
        <label className="field__label" htmlFor={id}>
          {label}
          {required && ' *'}
          {note && <span className="field__label-note"> · {note}</span>}
        </label>
      )}
      {control}
      {error ? (
        <span className="field__error">{error}</span>
      ) : (
        hint && <span className={`field__hint ${hintRight ? 'field__hint--right' : ''}`}>{hint}</span>
      )}
    </div>
  );
}

export function TextInput({ id, invalid, className = '', ...props }) {
  return <input id={id} className={`input ${invalid ? 'input--invalid' : ''} ${className}`} {...props} />;
}

export function TextArea({ id, invalid, ...props }) {
  return <textarea id={id} className={`textarea ${invalid ? 'textarea--invalid' : ''}`} {...props} />;
}

export function Select({ id, invalid, placeholder, options, value, ...props }) {
  return (
    <select id={id} className={`select ${invalid ? 'select--invalid' : ''}`} value={value ?? ''} required {...props}>
      <option value="" disabled>
        {placeholder}
      </option>
      {options.map((option) => {
        const item = typeof option === 'string' ? { value: option, label: option } : option;
        return (
          <option key={item.value} value={item.value}>
            {item.label}
          </option>
        );
      })}
    </select>
  );
}

export function PhoneInput({ value, onChange, ...props }) {
  const handleChange = (event) => {
    const next = event.target.value;
    let digits = phoneDigits(next);
    if (next.length < value.length && digits === phoneDigits(value)) {
      digits = digits.slice(0, -1);
    }
    onChange(digits.length <= 1 ? '' : formatPhone(digits));
  };
  return <TextInput type="tel" inputMode="tel" placeholder="+7 (___) ___-__-__" value={value} onChange={handleChange} {...props} />;
}

export function StatusBadge({ tone, children }) {
  return <span className={`badge badge--${tone}`}>{children}</span>;
}
