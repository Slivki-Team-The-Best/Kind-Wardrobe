import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import * as api from '../api';
import { useAuth } from '../auth/AuthContext';
import { Field, TextInput } from '../components/Fields';
import AuthHeader from '../components/AuthHeader';
import Icon from '../components/Icon';
import { isEmail } from '../utils/format';
import './auth.css';

export default function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [form, setForm] = useState({ email: '', password: '' });
  const [errors, setErrors] = useState({});
  const [message, setMessage] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const update = (field) => (event) => {
    setForm((current) => ({ ...current, [field]: event.target.value }));
    setErrors((current) => ({ ...current, [field]: undefined }));
    setMessage('');
  };

  const validate = () => {
    const next = {};
    if (!form.email.trim()) next.email = 'Введите email';
    else if (!isEmail(form.email)) next.email = 'Некорректный email';
    if (!form.password) next.password = 'Введите пароль';
    setErrors(next);
    return Object.keys(next).length === 0;
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    if (!validate()) return;
    setSubmitting(true);
    try {
      const { accessToken } = await api.login(form.email.trim(), form.password);
      login(accessToken);
      navigate(location.state?.from || '/cabinet', { replace: true });
    } catch (error) {
      setMessage(error.status === 401 ? 'Неверный email или пароль' : error.message);
      setSubmitting(false);
    }
  };

  return (
    <div className="auth-page">
      <AuthHeader />
      <div className="auth-page__body">
        <div className="auth-card">
          <div className="auth-card__icon">
            <Icon name="door" size={20} />
          </div>
          <h1 className="auth-card__title auth-card__title--center">С возвращением!</h1>
          <p className="auth-card__subtitle auth-card__subtitle--center">Ваш следующий добрый обмен уже рядом.</p>
          <form className="auth-form" onSubmit={handleSubmit} noValidate>
            {message && <div className="alert alert--error">{message}</div>}
            <Field label="Email" error={errors.email}>
              {(id) => (
                <TextInput
                  id={id}
                  type="email"
                  autoComplete="email"
                  placeholder="Введите email"
                  value={form.email}
                  onChange={update('email')}
                  invalid={errors.email}
                  autoFocus
                />
              )}
            </Field>
            <Field label="Пароль" error={errors.password}>
              {(id) => (
                <TextInput
                  id={id}
                  type="password"
                  autoComplete="current-password"
                  placeholder="Введите пароль"
                  value={form.password}
                  onChange={update('password')}
                  invalid={errors.password}
                />
              )}
            </Field>
            <div className="auth-form__row-end">
              <Link to="/forgot-password">Забыли пароль?</Link>
            </div>
            <button type="submit" className="btn btn--primary btn--block" disabled={submitting}>
              {submitting ? 'Входим…' : 'Войти'}
            </button>
          </form>
          <p className="auth-card__footer">
            Нет аккаунта? <Link to="/register">Зарегистрироваться</Link>
          </p>
        </div>
        <div className="auth-page__motto">
          <Icon name="leaf" size={14} /> Меньше лишнего. Больше доброго.
        </div>
      </div>
    </div>
  );
}
