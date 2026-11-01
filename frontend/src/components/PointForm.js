import { Map, Pencil } from 'lucide-react';
import { useEffect, useId, useState } from 'react';
import { Link } from 'react-router-dom';
import { saveOrganizerPoint } from '../api';
import { ITEM_TYPES } from '../utils/itemTypes';
import './PointForm.css';

const PHONE_PATTERN = /^\+?[0-9][0-9\s().-]{8,19}$/;

function valuesFrom(point) {
  return {
    name: point?.name ?? '',
    address: point?.address ?? '',
    workingHours: point?.workingHours ?? '',
    phone: point?.phone ?? '',
    description: point?.description ?? '',
    acceptedTypes: point?.acceptedTypes ?? [],
  };
}

function validate(values) {
  const errors = {};
  if (!values.name.trim()) errors.name = 'Укажите название пункта';
  if (!values.address.trim()) errors.address = 'Укажите адрес: город, улица, дом';
  if (!values.workingHours.trim()) errors.workingHours = 'Укажите часы работы';
  if (!values.phone.trim()) errors.phone = 'Укажите контактный телефон';
  else if (!PHONE_PATTERN.test(values.phone.trim())) errors.phone = 'Некорректный номер телефона';
  if (values.acceptedTypes.length === 0) errors.acceptedTypes = 'Выберите хотя бы один тип вещей';
  return errors;
}

/**
 * Форма «Основная информация» профиля организатора. Сначала поля только для чтения,
 * «Редактировать» открывает правку. Если пункта ещё нет, форма сразу в режиме правки.
 */
export default function PointForm({ point, onSaved }) {
  const uid = useId();
  const [editing, setEditing] = useState(!point);
  const [values, setValues] = useState(() => valuesFrom(point));
  const [errors, setErrors] = useState({});
  const [formError, setFormError] = useState('');
  const [notice, setNotice] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!editing) setValues(valuesFrom(point));
  }, [point, editing]);

  const setField = (field) => (event) => setValues((current) => ({ ...current, [field]: event.target.value }));

  const toggleType = (type) =>
    setValues((current) => ({
      ...current,
      acceptedTypes: current.acceptedTypes.includes(type)
        ? current.acceptedTypes.filter((t) => t !== type)
        : [...current.acceptedTypes, type],
    }));

  const startEditing = () => {
    setNotice('');
    setEditing(true);
  };

  const cancel = () => {
    setValues(valuesFrom(point));
    setErrors({});
    setFormError('');
    setEditing(false);
  };

  const submit = async (event) => {
    event.preventDefault();
    const found = validate(values);
    setErrors(found);
    setFormError('');
    if (Object.keys(found).length > 0) return;

    setSaving(true);
    try {
      const saved = await saveOrganizerPoint({
        ...values,
        name: values.name.trim(),
        address: values.address.trim(),
        workingHours: values.workingHours.trim(),
        phone: values.phone.trim(),
        description: values.description.trim(),
      });
      onSaved(saved);
      setEditing(false);
      setNotice('Данные пункта сохранены');
    } catch (e) {
      setErrors(e.fieldErrors && typeof e.fieldErrors === 'object' ? e.fieldErrors : {});
      setFormError(e.message);
    } finally {
      setSaving(false);
    }
  };

  const field = (name, label, props = {}) => (
    <div className="form__field">
      <label htmlFor={`${uid}-${name}`} className="form__label">{label}</label>
      <input
        id={`${uid}-${name}`}
        className={`form__input${errors[name] ? ' form__input--error' : ''}`}
        value={values[name]}
        onChange={setField(name)}
        readOnly={!editing}
        aria-invalid={Boolean(errors[name])}
        aria-describedby={errors[name] ? `${uid}-${name}-error` : undefined}
        {...props.input}
      />
      {errors[name] && <p id={`${uid}-${name}-error`} className="form__error">{errors[name]}</p>}
    </div>
  );

  return (
    <form id="point" className="point-form" onSubmit={submit} noValidate>
      <h2 className="point-form__title">Основная информация</h2>

      {field('name', 'Название пункта', { input: { maxLength: 150, autoComplete: 'organization' } })}
      {field('address', 'Адрес', { input: { maxLength: 255, autoComplete: 'street-address' } })}

      <div className="form__row">
        {field('workingHours', 'Часы работы', { input: { maxLength: 255, placeholder: 'Пн–Сб 10:00–20:00' } })}
        {field('phone', 'Контактный телефон', { input: { maxLength: 20, type: 'tel', autoComplete: 'tel' } })}
      </div>

      <div className="form__field">
        <label htmlFor={`${uid}-description`} className="form__label">Описание</label>
        <textarea
          id={`${uid}-description`}
          className="form__input form__textarea"
          rows={3}
          maxLength={2000}
          value={values.description}
          onChange={setField('description')}
          readOnly={!editing}
        />
      </div>

      <fieldset className="form__types" disabled={saving}>
        <legend className="form__label">Типы принимаемых вещей</legend>
        <div className="form__types-grid">
          {ITEM_TYPES.map(({ value, label }) => (
            <label key={value} className={`check${editing ? '' : ' check--readonly'}`}>
              <input
                type="checkbox"
                checked={values.acceptedTypes.includes(value)}
                onChange={() => toggleType(value)}
                tabIndex={editing ? 0 : -1}
                aria-readonly={!editing}
              />
              <span className="check__box" aria-hidden="true" />
              {label}
            </label>
          ))}
        </div>
        {errors.acceptedTypes && <p className="form__error">{errors.acceptedTypes}</p>}
      </fieldset>

      {formError && <p className="form__error form__error--banner" role="alert">{formError}</p>}
      {notice && <p className="form__notice" role="status">{notice}</p>}

      <div className="point-form__actions">
        {editing ? (
          <>
            <button type="submit" className="btn btn--primary" disabled={saving}>
              {saving ? 'Сохраняем…' : 'Сохранить'}
            </button>
            {point && (
              <button type="button" className="btn btn--soft" onClick={cancel} disabled={saving}>
                Отмена
              </button>
            )}
          </>
        ) : (
          <>
            <button type="button" className="btn btn--primary" onClick={startEditing}>
              <Pencil size={16} strokeWidth={1.75} /> Редактировать
            </button>
            <Link to="/map" className="btn btn--soft">
              <Map size={16} strokeWidth={1.75} /> Посмотреть на карте
            </Link>
          </>
        )}
      </div>
    </form>
  );
}
