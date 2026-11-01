import { useEffect, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import * as api from '../api';
import { StatusBadge, PageLoader } from '../components/Fields';
import Icon from '../components/Icon';
import { EXCHANGE_FORMATS, LISTING_STATUSES, itemTypeOf, labelOf } from '../constants';
import { formatDate } from '../utils/format';
import './cabinet.css';

const FILTERS = [
  { value: 'ALL', label: 'Все' },
  { value: 'ACTIVE', label: 'Активные' },
  { value: 'DRAFT', label: 'Черновики' },
  { value: 'HIDDEN', label: 'Скрытые' },
  { value: 'TRANSFERRED', label: 'Переданные' },
];

export default function MyListingsPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const [listings, setListings] = useState(null);
  const [error, setError] = useState('');
  const [filter, setFilter] = useState('ALL');
  const [flash, setFlash] = useState(location.state?.flash || '');
  const [deleting, setDeleting] = useState(null);

  useEffect(() => {
    if (location.state?.flash) {
      navigate(location.pathname, { replace: true, state: null });
    }
  }, [location, navigate]);

  useEffect(() => {
    let cancelled = false;
    api
      .getMyListings()
      .then((items) => {
        if (!cancelled) setListings(items);
      })
      .catch((e) => {
        if (!cancelled) setError(e.message);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const remove = async (listing) => {
    if (!window.confirm(`Удалить объявление «${listing.title}»? Это действие нельзя отменить.`)) return;
    setDeleting(listing.id);
    try {
      await api.deleteListing(listing.id);
      setListings((current) => current.filter((item) => item.id !== listing.id));
      setFlash('Объявление удалено');
    } catch (e) {
      setError(e.message);
    } finally {
      setDeleting(null);
    }
  };

  const count = (value) => (value === 'ALL' ? listings.length : listings.filter((item) => item.status === value).length);
  const visible = listings && (filter === 'ALL' ? listings : listings.filter((item) => item.status === filter));

  return (
    <>
      <div className="cabinet-head">
        <div>
          <h1 className="page-title">Мои объявления</h1>
          <p className="page-subtitle">Управляйте своими вещами: редактируйте, скрывайте и удаляйте.</p>
        </div>
        <Link to="/listings/new" className="btn btn--primary">
          <Icon name="plus" size={16} /> Создать объявление
        </Link>
      </div>
      {flash && (
        <div className="alert alert--success" style={{ marginBottom: 16 }}>
          <Icon name="checkCircle" size={16} /> {flash}
        </div>
      )}
      {error && (
        <div className="alert alert--error" style={{ marginBottom: 16 }}>
          {error}
        </div>
      )}
      {!listings && !error && <PageLoader />}
      {listings && (
        <>
          <div className="tabs" role="group" aria-label="Фильтр по статусу">
            {FILTERS.map((item) => (
              <button
                key={item.value}
                type="button"
                className="tab"
                aria-pressed={filter === item.value}
                onClick={() => setFilter(item.value)}
              >
                {item.label} · {count(item.value)}
              </button>
            ))}
          </div>
          {visible.length === 0 ? (
            <div className="empty">
              <Icon name="layers" size={28} />
              {listings.length === 0 ? 'Вы ещё не разместили ни одного объявления' : 'В этом разделе пока пусто'}
              {listings.length === 0 && (
                <Link to="/listings/new" className="btn btn--primary">
                  Создать первое объявление
                </Link>
              )}
            </div>
          ) : (
            <div className="row-list">
              {visible.map((listing) => {
                const status = LISTING_STATUSES[listing.status];
                const editable = listing.status !== 'TRANSFERRED';
                return (
                  <div key={listing.id} className="row-item">
                    <span className="mini-item__thumb">
                      {listing.coverUrl ? <img src={listing.coverUrl} alt="" /> : <Icon name="image" size={24} />}
                    </span>
                    <div style={{ minWidth: 0 }}>
                      <div className="row-item__title">{listing.title}</div>
                      <div className="row-item__meta">
                        <StatusBadge tone={status.tone}>{status.label}</StatusBadge>
                        <span>
                          {itemTypeOf(listing.itemType).label} · {labelOf(EXCHANGE_FORMATS, listing.exchangeFormat)}
                        </span>
                        <span>{listing.city}</span>
                        <span>
                          {listing.publishedAt
                            ? `Опубликовано ${formatDate(listing.publishedAt)}`
                            : `Создано ${formatDate(listing.createdAt)}`}
                        </span>
                      </div>
                    </div>
                    <div className="row-item__actions">
                      {editable && (
                        <Link to={`/listings/${listing.id}/edit`} className="btn btn--soft">
                          <Icon name="pencil" size={15} /> Редактировать
                        </Link>
                      )}
                      <button
                        type="button"
                        className="btn btn--danger"
                        onClick={() => remove(listing)}
                        disabled={deleting === listing.id}
                        aria-label={`Удалить «${listing.title}»`}
                      >
                        <Icon name="trash" size={15} />
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </>
      )}
    </>
  );
}
