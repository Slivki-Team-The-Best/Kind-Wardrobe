import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import * as api from '../api';
import { useAuth } from '../auth/AuthContext';
import { Field, PhoneInput, Select, TextInput } from '../components/Fields';
import AuthHeader from '../components/AuthHeader';
import Icon from '../components/Icon';
import { CITIES } from '../constants';
import { isEmail, isPhoneComplete } from '../utils/format';
import './auth.css';

const EMPTY = {
  name: '',
  city: '',
  email: '',
  phone: '',
  password: '',
  passwordConfirm: '',
  pointName: '',
  pointAddress: '',
  workingHours: '',
  pointPhone: '',
  pointDescription: '',
  termsAccepted: false,
};

function validate(role, form) {
  const errors = {};
  const required = (field, message) => {
    if (!String(form[field]).trim()) errors[field] = message;
  };
  if (role === 'USER') {
    required('name', 'Введите имя');
    required('city', 'Выберите город');
  }
  if (!form.email.trim()) errors.email = 'Введите email';
  else if (!isEmail(form.email)) errors.email = 'Некорректный email';
  if (!form.phone) errors.phone = 'Введите телефон';
  else if (!isPhoneComplete(form.phone)) errors.phone = 'Введите телефон полностью';
  if (!form.password) errors.password = 'Введите пароль';
  else if (form.password.length < 8) errors.password = 'Пароль должен быть не короче 8 символов';
  if (!form.passwordConfirm) errors.passwordConfirm = 'Повторите пароль';
  else if (form.password !== form.passwordConfirm) errors.passwordConfirm = 'Пароли не совпадают';
  if (role === 'ORGANIZER') {
    required('pointName', 'Введите название пункта');
    required('pointAddress', 'Введите адрес пункта');
    required('workingHours', 'Укажите часы работы');
    if (!form.pointPhone) errors.pointPhone = 'Укажите контактный телефон';
    else if (!isPhoneComplete(form.pointPhone)) errors.pointPhone = 'Введите телефон полностью';
    required('pointDescription', 'Расскажите о пункте');
  }
  if (!form.termsAccepted) errors.termsAccepted = 'Нужно согласие с условиями использования';
  return errors;
}

export default function RegisterPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [role, setRole] = useState('USER');
  const [form, setForm] = useState(EMPTY);
  const [errors, setErrors] = useState({});
  const [message, setMessage] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const setValue = (field, value) => {
    setForm((current) => ({ ...current, [field]: value }));
    setErrors((current) => ({ ...current, [field]: undefined }));
    setMessage('');
  };
  const update = (field) => (event) => setValue(field, event.target.value);

  const switchRole = (next) => {
    setRole(next);
    setErrors({});
    setMessage('');
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    const found = validate(role, form);
    setErrors(found);
    if (Object.keys(found).length > 0) return;
    setSubmitting(true);
    const payload =
      role === 'USER'
        ? {
            role,
            name: form.name.trim(),
            city: form.city,
            email: form.email.trim(),
            phone: form.phone,
            password: form.password,
            passwordConfirm: form.passwordConfirm,
            termsAccepted: form.termsAccepted,
          }
        : {
            role,
            email: form.email.trim(),
            phone: form.phone,
            password: form.password,
            passwordConfirm: form.passwordConfirm,
            pointName: form.pointName.trim(),
            pointAddress: form.pointAddress.trim(),
            workingHours: form.workingHours.trim(),
            pointPhone: form.pointPhone,
            pointDescription: form.pointDescription.trim(),
            termsAccepted: form.termsAccepted,
          };
    try {
      const { accessToken } = await api.register(payload);
      login(accessToken);
      navigate('/cabinet', { replace: true });
    } catch (error) {
      setErrors(error.fieldErrors || {});
      setMessage(Object.keys(error.fieldErrors || {}).length ? '' : error.message);
      setSubmitting(false);
    }
  };

  const text = (field, label, props = {}) => (
    <Field label={label} required error={errors[field]} className={props.wide ? 'form-grid__wide' : ''}>
      {(id) => (
        <TextInput id={id} value={form[field]} onChange={update(field)} invalid={errors[field]} {...props.input} />
      )}
    </Field>
  );

  const phone = (field, label) => (
    <Field label={label} required error={errors[field]}>
      {(id) => (
        <PhoneInput id={id} value={form[field]} onChange={(value) => setValue(field, value)} invalid={errors[field]} />
      )}
    </Field>
  );

  const credentials = (
    <>
      {text('email', 'Email', { input: { type: 'email', autoComplete: 'email', placeholder: 'anna@example.ru' } })}
      {phone('phone', 'Телефон')}
      {text('password', 'Пароль', {
        input: { type: 'password', autoComplete: 'new-password', placeholder: 'Не менее 8 символов' },
      })}
      {text('passwordConfirm', 'Подтверждение пароля', {
        input: { type: 'password', autoComplete: 'new-password', placeholder: 'Повторите пароль' },
      })}
    </>
  );

  return (
    <div className="auth-page">
      <AuthHeader />
      <div className="auth-page__body">
        <div className="auth-card auth-card--wide">
          <h1 className="auth-card__title">Давайте знакомиться</h1>
          <p className="auth-card__subtitle">Создайте аккаунт и подарите вещам новую жизнь.</p>
          <div className="role-switch" role="group" aria-label="Роль">
            <button
              type="button"
              className="role-switch__option"
              aria-pressed={role === 'USER'}
              onClick={() => switchRole('USER')}
            >
              <Icon name="user" size={16} /> Пользователь
            </button>
            <button
              type="button"
              className="role-switch__option"
              aria-pressed={role === 'ORGANIZER'}
              onClick={() => switchRole('ORGANIZER')}
            >
              <Icon name="store" size={16} /> Организатор
            </button>
          </div>
          <form className="auth-form" onSubmit={handleSubmit} noValidate>
            {message && <div className="alert alert--error">{message}</div>}
            <div className="form-grid">
              {role === 'USER' ? (
                <>
                  {text('name', 'Имя', { input: { autoComplete: 'given-name', placeholder: 'Как к вам обращаться' } })}
                  <Field label="Город" required error={errors.city}>
                    {(id) => (
                      <Select
                        id={id}
                        placeholder="Выберите город"
                        options={CITIES}
                        value={form.city}
                        onChange={update('city')}
                        invalid={errors.city}
                      />
                    )}
                  </Field>
                  {credentials}
                </>
              ) : (
                <>
                  {credentials}
                  {text('pointName', 'Название пункта', { input: { placeholder: 'Например, Тёплый круг' } })}
                  {text('pointAddress', 'Адрес пункта', { input: { placeholder: 'Город, улица, дом' } })}
                  {text('workingHours', 'Часы работы', { input: { placeholder: 'Пн–Сб, 10:00–20:00' } })}
                  {phone('pointPhone', 'Контактный телефон')}
                  {text('pointDescription', 'Описание', {
                    wide: true,
                    input: { placeholder: 'Расскажите о вашем пункте обмена' },
                  })}
                </>
              )}
            </div>
            <Field error={errors.termsAccepted}>
              <label className="checkbox">
                <input
                  type="checkbox"
                  checked={form.termsAccepted}
                  onChange={(event) => setValue('termsAccepted', event.target.checked)}
                />
                Согласен с условиями использования
              </label>
            </Field>
            <button type="submit" className="btn btn--primary btn--block" disabled={submitting}>
              {submitting ? 'Создаём аккаунт…' : 'Зарегистрироваться'}
            </button>
          </form>
          <p className="auth-card__footer">
            Уже есть аккаунт? <Link to="/login">Войти</Link>
          </p>
        </div>
      </div>
    </div>
  );
}
