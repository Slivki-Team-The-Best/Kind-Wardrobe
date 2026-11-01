import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import * as api from '../api';
import { useAuth } from '../auth/AuthContext';
import Avatar from '../components/Avatar';
import { Field, PhoneInput, Select, TextInput } from '../components/Fields';
import Icon from '../components/Icon';
import { CITIES, EXCHANGE_FORMATS, IMAGE_TYPES, LISTING_STATUSES, MAX_AVATAR_BYTES, itemTypeOf, labelOf } from '../constants';
import { formatDate, formatKg, formatPhone, isPhoneComplete } from '../utils/format';
import './cabinet.css';

function formFrom(user) {
  return { name: user.name || '', city: user.city || '', phone: formatPhone(user.phone || '') };
}

function MyListingsPreview() {
  const [listings, setListings] = useState(null);

  useEffect(() => {
    let cancelled = false;
    api
      .getMyListings()
      .then((items) => {
        if (!cancelled) setListings(items);
      })
      .catch(() => {
        if (!cancelled) setListings([]);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <section className="section">
      <div className="section__head">
        <h2 className="section-title">Мои объявления</h2>
        {listings && listings.length > 0 && (
          <Link to="/my-listings" className="link-arrow">
            Все · {listings.length} →
          </Link>
        )}
      </div>
      {listings && listings.length === 0 && (
        <div className="empty">
          Объявлений пока нет
          <Link to="/listings/new" className="btn btn--soft">
            <Icon name="plus" size={16} /> Создать объявление
          </Link>
        </div>
      )}
      {listings && listings.length > 0 && (
        <div className="mini-list">
          {listings.slice(0, 3).map((listing) => {
            const status = LISTING_STATUSES[listing.status];
            return (
              <Link key={listing.id} to={`/listings/${listing.id}/edit`} className="mini-item">
                <span className="mini-item__thumb">
                  {listing.coverUrl ? <img src={listing.coverUrl} alt="" /> : <Icon name="image" size={22} />}
                </span>
                <span>
                  <span className="mini-item__title">{listing.title}</span>
                  <br />
                  <span className="mini-item__meta">
                    {itemTypeOf(listing.itemType).label} · {labelOf(EXCHANGE_FORMATS, listing.exchangeFormat)}
                  </span>
                  <br />
                  <span className={`mini-item__meta status-text--${status.tone}`}>{status.label}</span>
                </span>
              </Link>
            );
          })}
        </div>
      )}
    </section>
  );
}

export default function ProfilePage() {
  const { user, setUser, logout } = useAuth();
  const navigate = useNavigate();
  const fileInput = useRef(null);
  const [form, setForm] = useState(() => formFrom(user));
  const [errors, setErrors] = useState({});
  const [notice, setNotice] = useState(null);
  const [saving, setSaving] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [stats, setStats] = useState(null);

  useEffect(() => {
    let cancelled = false;
    api
      .getMyStats()
      .then((value) => {
        if (!cancelled) setStats(value);
      })
      .catch(() => {});
    return () => {
      cancelled = true;
    };
  }, []);

  const isUser = user.role === 'USER';
  const cities = user.city && !CITIES.includes(user.city) ? [user.city, ...CITIES] : CITIES;

  const setValue = (field, value) => {
    setForm((current) => ({ ...current, [field]: value }));
    setErrors((current) => ({ ...current, [field]: undefined }));
    setNotice(null);
  };

  const handleAvatar = async (event) => {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file) return;
    if (!IMAGE_TYPES.includes(file.type)) {
      setNotice({ type: 'error', text: 'Можно загрузить только JPG или PNG' });
      return;
    }
    if (file.size > MAX_AVATAR_BYTES) {
      setNotice({ type: 'error', text: 'Фото должно быть не больше 5 МБ' });
      return;
    }
    setUploading(true);
    try {
      setUser(await api.uploadAvatar(file));
      setNotice({ type: 'success', text: 'Фото обновлено' });
    } catch (error) {
      setNotice({ type: 'error', text: error.fieldErrors?.avatar || error.message });
    } finally {
      setUploading(false);
    }
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    const found = {};
    if (isUser && !form.name.trim()) found.name = 'Введите имя';
    if (isUser && !form.city) found.city = 'Выберите город';
    if (!form.phone) found.phone = 'Введите телефон';
    else if (!isPhoneComplete(form.phone)) found.phone = 'Введите телефон полностью';
    setErrors(found);
    if (Object.keys(found).length) return;
    setSaving(true);
    try {
      const updated = await api.updateProfile({
        name: form.name.trim(),
        city: form.city,
        phone: form.phone,
      });
      setUser(updated);
      setForm(formFrom(updated));
      setNotice({ type: 'success', text: 'Изменения сохранены' });
    } catch (error) {
      setErrors(error.fieldErrors || {});
      setNotice({ type: 'error', text: error.message });
    } finally {
      setSaving(false);
    }
  };

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  return (
    <>
      <div className="cabinet-head">
        <div>
          <h1 className="page-title">Мой профиль</h1>
          <p className="page-subtitle">Здесь живут ваши данные и добрые дела.</p>
        </div>
      </div>
      <div className="profile">
        <form className="panel profile__card" onSubmit={handleSubmit} noValidate>
          <h2 className="section-title">Личная информация</h2>
          <div className="profile__avatar-row">
            <Avatar url={user.avatarUrl} name={user.name || user.email} size={72} />
            <div>
              <button
                type="button"
                className="btn btn--soft"
                onClick={() => fileInput.current?.click()}
                disabled={uploading}
              >
                <Icon name="upload" size={16} /> {uploading ? 'Загружаем…' : 'Загрузить фото'}
              </button>
              <div className="profile__avatar-note">JPG или PNG, до 5 МБ</div>
              <input ref={fileInput} type="file" accept="image/jpeg,image/png" hidden onChange={handleAvatar} />
            </div>
          </div>
          {notice && <div className={`alert alert--${notice.type}`} style={{ marginBottom: 16 }}>{notice.text}</div>}
          <div className="form-grid">
            <Field label="Имя" required={isUser} error={errors.name}>
              {(id) => (
                <TextInput
                  id={id}
                  value={form.name}
                  onChange={(event) => setValue('name', event.target.value)}
                  invalid={errors.name}
                  placeholder="Как к вам обращаться"
                />
              )}
            </Field>
            <Field label="Город" required={isUser} error={errors.city}>
              {(id) => (
                <Select
                  id={id}
                  placeholder="Выберите город"
                  options={cities}
                  value={form.city}
                  onChange={(event) => setValue('city', event.target.value)}
                  invalid={errors.city}
                />
              )}
            </Field>
            <Field label="Email">{(id) => <TextInput id={id} value={user.email} disabled />}</Field>
            <Field label="Телефон" required error={errors.phone}>
              {(id) => (
                <PhoneInput id={id} value={form.phone} onChange={(value) => setValue('phone', value)} invalid={errors.phone} />
              )}
            </Field>
            <Field label="Дата регистрации">
              <span className="profile__readonly">{formatDate(user.createdAt)}</span>
            </Field>
          </div>
          <div className="profile__footer">
            <button type="submit" className="btn btn--primary" disabled={saving}>
              {saving ? 'Сохраняем…' : 'Сохранить'}
            </button>
            <button type="button" className="btn btn--ghost" onClick={handleLogout}>
              <Icon name="logOut" size={16} /> Выйти
            </button>
          </div>
        </form>

        <div>
          <section>
            <div className="section__head">
              <h2 className="section-title">Эко-вклад</h2>
              <Link to="/eco" className="link-arrow">
                Подробнее →
              </Link>
            </div>
            <div className="stat-tiles">
              <div className="stat-tile stat-tile--green">
                <Icon name="leaf" size={18} className="stat-tile__icon" />
                <div className="stat-tile__label">Спасено текстиля</div>
                <div className="stat-tile__value">{stats ? `${formatKg(stats.savedKg)} кг` : '—'}</div>
              </div>
              <div className="stat-tile">
                <Icon name="repeat" size={18} className="stat-tile__icon muted" />
                <div className="stat-tile__label muted">Обменов</div>
                <div className="stat-tile__value">{stats ? stats.exchangesCount : '—'}</div>
              </div>
            </div>
          </section>
          <MyListingsPreview />
        </div>
      </div>
    </>
  );
}
