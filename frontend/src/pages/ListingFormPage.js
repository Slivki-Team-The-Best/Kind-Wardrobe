import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import * as api from '../api';
import { useAuth } from '../auth/AuthContext';
import { Field, Select, StatusBadge, TextArea, TextInput, PageLoader } from '../components/Fields';
import Icon from '../components/Icon';
import {
  CATEGORIES,
  CITIES,
  CONDITIONS,
  EXCHANGE_FORMATS,
  IMAGE_TYPES,
  ITEM_TYPES,
  LISTING_STATUSES,
  MAX_PHOTOS,
  MAX_PHOTO_BYTES,
} from '../constants';
import './cabinet.css';
import './listing-form.css';

const DESCRIPTION_LIMIT = 500;

const EMPTY = {
  title: '',
  itemType: '',
  category: '',
  condition: '',
  description: '',
  exchangeFormat: '',
  weightKg: '',
  city: '',
  telegramUrl: '',
  whatsappUrl: '',
  maxUrl: '',
};

function formFrom(listing) {
  return {
    title: listing.title,
    itemType: listing.itemType,
    category: listing.category,
    condition: listing.condition,
    description: listing.description || '',
    exchangeFormat: listing.exchangeFormat,
    weightKg: String(listing.weightKg).replace('.', ','),
    city: listing.city,
    telegramUrl: listing.telegramUrl || '',
    whatsappUrl: listing.whatsappUrl || '',
    maxUrl: listing.maxUrl || '',
  };
}

function parseWeight(value) {
  const normalized = String(value).trim().replace(',', '.');
  if (!/^\d+(\.\d{1,2})?$/.test(normalized)) return null;
  return Number(normalized);
}

function validate(form, photoCount, publishing) {
  const errors = {};
  if (!form.title.trim()) errors.title = 'Введите название вещи';
  if (!form.itemType) errors.itemType = 'Выберите тип вещи';
  if (!form.category) errors.category = 'Выберите категорию';
  if (!form.condition) errors.condition = 'Выберите состояние';
  if (!form.description.trim()) errors.description = 'Добавьте описание';
  else if (form.description.length > DESCRIPTION_LIMIT) errors.description = `Не длиннее ${DESCRIPTION_LIMIT} символов`;
  if (!form.exchangeFormat) errors.exchangeFormat = 'Выберите формат обмена';
  const weight = parseWeight(form.weightKg);
  if (!String(form.weightKg).trim()) errors.weightKg = 'Укажите вес';
  else if (weight === null) errors.weightKg = 'Число, например 0,8';
  else if (weight <= 0) errors.weightKg = 'Вес должен быть больше 0';
  else if (weight > 9999.99) errors.weightKg = 'Слишком большой вес';
  if (!form.city) errors.city = 'Выберите город';
  if (publishing) {
    if (photoCount === 0) errors.photos = 'Загрузите хотя бы одно фото';
    if (!form.telegramUrl.trim() && !form.whatsappUrl.trim() && !form.maxUrl.trim()) {
      errors.contacts = 'Укажите ссылку на Telegram, WhatsApp или MAX';
    }
  }
  return errors;
}

let photoKey = 0;

function PhotoPicker({ photos, error, onAdd, onRemove }) {
  const input = useRef(null);
  const [main, ...rest] = photos;
  const full = photos.length >= MAX_PHOTOS;
  const open = () => input.current?.click();

  return (
    <div>
      <div className="photos__head">
        <span className="field__label">Фотографии *</span>
        <span className="photos__counter">
          {photos.length} из {MAX_PHOTOS}
        </span>
      </div>
      {main ? (
        <div className="photo-main">
          <img src={main.url} alt="Обложка объявления" />
          <span className="photo-cover-label">
            <StatusBadge tone="blue">Обложка</StatusBadge>
          </span>
          <button type="button" className="photo-remove" onClick={() => onRemove(main)} aria-label="Удалить фото">
            <Icon name="x" size={14} />
          </button>
        </div>
      ) : (
        <button type="button" className={`photo-main photo-main--empty ${error ? 'photo-main--invalid' : ''}`} onClick={open}>
          <Icon name="camera" size={28} />
          Добавить фото
        </button>
      )}
      <div className="photo-slots">
        {Array.from({ length: MAX_PHOTOS - 1 }, (_, index) => {
          const photo = rest[index];
          if (photo) {
            return (
              <div key={photo.key} className="photo-slot">
                <img src={photo.url} alt={`Фото ${index + 2}`} />
                <button type="button" className="photo-remove" onClick={() => onRemove(photo)} aria-label="Удалить фото">
                  <Icon name="x" size={12} />
                </button>
              </div>
            );
          }
          return (
            <button
              key={`empty-${index}`}
              type="button"
              className="photo-slot photo-slot--empty"
              onClick={open}
              disabled={full}
              aria-label="Добавить фото"
            >
              <Icon name="plus" size={18} />
            </button>
          );
        })}
      </div>
      <input
        ref={input}
        type="file"
        accept="image/jpeg,image/png"
        multiple
        hidden
        onChange={(event) => {
          onAdd(Array.from(event.target.files || []));
          event.target.value = '';
        }}
      />
      {error ? <div className="field__error" style={{ marginTop: 8 }}>{error}</div> : null}
      <div className="photos__hint">
        Добавьте от 1 до {MAX_PHOTOS} фото.
        <br />
        JPG или PNG, до 10 МБ каждое.
      </div>
      <div className="photos__tip">
        <Icon name="camera" size={18} />
        Снимайте при дневном свете и покажите все особенности вещи.
      </div>
    </div>
  );
}

export default function ListingFormPage() {
  const { id } = useParams();
  const isEdit = Boolean(id);
  const navigate = useNavigate();
  const { user } = useAuth();
  const [listing, setListing] = useState(null);
  const [loadError, setLoadError] = useState('');
  const [form, setForm] = useState(() => ({ ...EMPTY, city: user.city || '' }));
  const [photos, setPhotos] = useState([]);
  const [removed, setRemoved] = useState([]);
  const [errors, setErrors] = useState({});
  const [message, setMessage] = useState('');
  const [busy, setBusy] = useState(false);
  const photosRef = useRef(photos);
  photosRef.current = photos;

  useEffect(() => {
    if (!isEdit) return undefined;
    let cancelled = false;
    api
      .getListing(id)
      .then((data) => {
        if (cancelled) return;
        setListing(data);
        setForm(formFrom(data));
        setPhotos(data.photos.map((photo) => ({ key: `existing-${photo.id}`, id: photo.id, url: photo.url })));
      })
      .catch((error) => {
        if (!cancelled) setLoadError(error.status === 404 ? 'Объявление не найдено' : error.message);
      });
    return () => {
      cancelled = true;
    };
  }, [id, isEdit]);

  useEffect(
    () => () => {
      photosRef.current.forEach((photo) => photo.file && URL.revokeObjectURL(photo.url));
    },
    [],
  );

  const setValue = (field, value) => {
    setForm((current) => ({ ...current, [field]: value }));
    setErrors((current) => {
      const next = { ...current, [field]: undefined };
      if (['telegramUrl', 'whatsappUrl', 'maxUrl'].includes(field)) next.contacts = undefined;
      return next;
    });
    setMessage('');
  };
  const update = (field) => (event) => setValue(field, event.target.value);

  const addPhotos = (files) => {
    const accepted = [];
    let problem = '';
    for (const file of files) {
      if (!IMAGE_TYPES.includes(file.type)) {
        problem = 'Можно загружать только JPG или PNG';
      } else if (file.size > MAX_PHOTO_BYTES) {
        problem = `Файл «${file.name}» больше 10 МБ`;
      } else if (photos.length + accepted.length >= MAX_PHOTOS) {
        problem = `Можно загрузить не больше ${MAX_PHOTOS} фото`;
      } else {
        accepted.push({ key: `new-${photoKey++}`, file, url: URL.createObjectURL(file) });
      }
    }
    setPhotos((current) => [...current, ...accepted]);
    setErrors((current) => ({ ...current, photos: problem || undefined }));
  };

  const removePhoto = (photo) => {
    setPhotos((current) => current.filter((item) => item.key !== photo.key));
    if (photo.file) URL.revokeObjectURL(photo.url);
    else setRemoved((current) => [...current, photo.id]);
  };

  const payload = (status) => ({
    title: form.title.trim(),
    itemType: form.itemType,
    category: form.category,
    condition: form.condition,
    description: form.description.trim(),
    exchangeFormat: form.exchangeFormat,
    weightKg: parseWeight(form.weightKg),
    city: form.city,
    telegramUrl: form.telegramUrl.trim() || null,
    whatsappUrl: form.whatsappUrl.trim() || null,
    maxUrl: form.maxUrl.trim() || null,
    status,
  });

  const syncPhotos = async () => {
    const newFiles = photos.filter((photo) => photo.file).map((photo) => photo.file);
    const keptExisting = photos.filter((photo) => !photo.file).length;
    const toDelete = [...removed];
    const deferred = listing.status === 'ACTIVE' && keptExisting === 0 && toDelete.length > 0 ? toDelete.pop() : null;
    for (const photoId of toDelete) {
      await api.deleteListingPhoto(listing.id, photoId);
    }
    const firstBatch = deferred ? newFiles.slice(0, MAX_PHOTOS - 1) : newFiles;
    if (firstBatch.length) await api.uploadListingPhotos(listing.id, firstBatch);
    if (deferred) {
      await api.deleteListingPhoto(listing.id, deferred);
      const secondBatch = newFiles.slice(MAX_PHOTOS - 1);
      if (secondBatch.length) await api.uploadListingPhotos(listing.id, secondBatch);
    }
  };

  const submit = async (status) => {
    const found = validate(form, photos.length, status === 'ACTIVE');
    setErrors(found);
    setMessage('');
    if (Object.keys(found).length) {
      setMessage('Проверьте выделенные поля');
      return;
    }
    setBusy(true);
    try {
      if (isEdit) {
        await syncPhotos();
        await api.updateListing(listing.id, payload(status));
      } else {
        await api.createListing(
          payload(status),
          photos.map((photo) => photo.file),
        );
      }
      const flash = {
        ACTIVE: isEdit ? 'Объявление сохранено' : 'Объявление опубликовано',
        DRAFT: 'Черновик сохранён',
        HIDDEN: 'Объявление скрыто из ленты',
      }[status];
      navigate('/my-listings', { state: { flash } });
    } catch (error) {
      setErrors(error.fieldErrors || {});
      setMessage(error.message);
      setBusy(false);
      if (isEdit) {
        api
          .getListing(listing.id)
          .then((fresh) => {
            photos.forEach((photo) => photo.file && URL.revokeObjectURL(photo.url));
            setListing(fresh);
            setRemoved([]);
            setPhotos(fresh.photos.map((photo) => ({ key: `existing-${photo.id}`, id: photo.id, url: photo.url })));
          })
          .catch(() => {});
      }
    }
  };

  const remove = async () => {
    if (!window.confirm('Удалить объявление? Это действие нельзя отменить.')) return;
    setBusy(true);
    try {
      await api.deleteListing(listing.id);
      navigate('/my-listings', { state: { flash: 'Объявление удалено' } });
    } catch (error) {
      setMessage(error.message);
      setBusy(false);
    }
  };

  if (isEdit && loadError) {
    return (
      <div className="empty">
        {loadError}
        <Link to="/my-listings" className="btn btn--soft">
          К моим объявлениям
        </Link>
      </div>
    );
  }
  if (isEdit && !listing) return <PageLoader />;

  if (isEdit && listing.status === 'TRANSFERRED') {
    return (
      <div className="empty">
        Это объявление уже передано — изменить его нельзя.
        <Link to="/my-listings" className="btn btn--soft">
          К моим объявлениям
        </Link>
      </div>
    );
  }

  const cities = form.city && !CITIES.includes(form.city) ? [form.city, ...CITIES] : CITIES;
  const status = listing ? LISTING_STATUSES[listing.status] : null;

  const select = (field, label, options, placeholder) => (
    <Field label={label} required error={errors[field]}>
      {(fieldId) => (
        <Select
          id={fieldId}
          placeholder={placeholder}
          options={options}
          value={form[field]}
          onChange={update(field)}
          invalid={errors[field]}
        />
      )}
    </Field>
  );

  return (
    <>
      <div className="cabinet-head">
        <div>
          <h1 className="page-title">{isEdit ? 'Редактирование объявления' : 'Новое объявление'}</h1>
          <p className="page-subtitle">Расскажите о вещи — и она обязательно кому-нибудь пригодится.</p>
        </div>
        {status && <StatusBadge tone={status.tone}>{status.label}</StatusBadge>}
      </div>
      <form className="panel listing-form" onSubmit={(event) => event.preventDefault()} noValidate>
        {message && (
          <div className="alert alert--error" style={{ marginBottom: 16 }}>
            {message}
          </div>
        )}
        <div className="listing-form__grid">
          <PhotoPicker photos={photos} error={errors.photos} onAdd={addPhotos} onRemove={removePhoto} />
          <div className="listing-form__fields">
            <Field label="Название вещи" required error={errors.title}>
              {(fieldId) => (
                <TextInput
                  id={fieldId}
                  maxLength={100}
                  placeholder="Например, куртка-рубашка, размер M"
                  value={form.title}
                  onChange={update('title')}
                  invalid={errors.title}
                />
              )}
            </Field>
            <div className="form-grid form-grid--3">
              {select('itemType', 'Тип вещи', ITEM_TYPES, 'Выберите тип')}
              {select('category', 'Категория', CATEGORIES, 'Выберите')}
              {select('condition', 'Состояние', CONDITIONS, 'Выберите')}
            </div>
            <Field
              label="Описание"
              required
              error={errors.description}
              hint={`${form.description.length} / ${DESCRIPTION_LIMIT}`}
              hintRight
            >
              {(fieldId) => (
                <TextArea
                  id={fieldId}
                  rows={3}
                  maxLength={DESCRIPTION_LIMIT}
                  placeholder="Размер, материал, особенности, дефекты"
                  value={form.description}
                  onChange={update('description')}
                  invalid={errors.description}
                />
              )}
            </Field>
            <div className="form-grid form-grid--3">
              {select('exchangeFormat', 'Формат обмена', EXCHANGE_FORMATS, 'Выберите')}
              <Field label="Вес товара, кг" required error={errors.weightKg}>
                {(fieldId) => (
                  <TextInput
                    id={fieldId}
                    inputMode="decimal"
                    placeholder="0,8"
                    value={form.weightKg}
                    onChange={update('weightKg')}
                    invalid={errors.weightKg}
                  />
                )}
              </Field>
              {select('city', 'Город', cities, 'Выберите город')}
            </div>
            <div className="form-grid form-grid--3">
              <Field label="Telegram" error={errors.telegramUrl}>
                {(fieldId) => (
                  <TextInput
                    id={fieldId}
                    placeholder="t.me/username"
                    value={form.telegramUrl}
                    onChange={update('telegramUrl')}
                    invalid={errors.telegramUrl || errors.contacts}
                  />
                )}
              </Field>
              <Field label="WhatsApp" error={errors.whatsappUrl}>
                {(fieldId) => (
                  <TextInput
                    id={fieldId}
                    placeholder="wa.me/7… или номер"
                    value={form.whatsappUrl}
                    onChange={update('whatsappUrl')}
                    invalid={errors.whatsappUrl || errors.contacts}
                  />
                )}
              </Field>
              <Field label="MAX" error={errors.maxUrl}>
                {(fieldId) => (
                  <TextInput
                    id={fieldId}
                    placeholder="Ссылка на профиль"
                    value={form.maxUrl}
                    onChange={update('maxUrl')}
                    invalid={errors.maxUrl || errors.contacts}
                  />
                )}
              </Field>
            </div>
            {errors.contacts ? (
              <span className="field__error">{errors.contacts}</span>
            ) : (
              <span className="field__hint">Для публикации нужна хотя бы одна ссылка на мессенджер.</span>
            )}
          </div>
        </div>
        <div className="listing-form__footer">
          <span className="listing-form__note">* Обязательные поля. Проверьте данные перед публикацией.</span>
          {isEdit && (
            <button type="button" className="btn btn--danger" onClick={remove} disabled={busy}>
              <Icon name="trash" size={16} /> Удалить
            </button>
          )}
          {isEdit && listing.status === 'ACTIVE' && (
            <button type="button" className="btn btn--ghost" onClick={() => submit('HIDDEN')} disabled={busy}>
              <Icon name="eyeOff" size={16} /> Скрыть
            </button>
          )}
          {isEdit && listing.status === 'HIDDEN' && (
            <button type="button" className="btn btn--ghost" onClick={() => submit('HIDDEN')} disabled={busy}>
              Сохранить скрытым
            </button>
          )}
          {(!isEdit || listing.status === 'DRAFT') && (
            <button type="button" className="btn btn--soft" onClick={() => submit('DRAFT')} disabled={busy}>
              Сохранить как черновик
            </button>
          )}
          <button type="button" className="btn btn--primary" onClick={() => submit('ACTIVE')} disabled={busy}>
            {busy ? 'Сохраняем…' : isEdit && listing.status === 'ACTIVE' ? 'Сохранить' : 'Опубликовать'}
          </button>
        </div>
      </form>
    </>
  );
}
