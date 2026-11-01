import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import * as api from '../api';
import { useAuth } from '../auth/AuthContext';
import { StatusBadge, PageLoader } from '../components/Fields';
import Icon from '../components/Icon';
import ListingCard from '../components/ListingCard';
import { ITEM_TYPES } from '../utils/itemTypes';
import { firstName, formatKg, greeting } from '../utils/format';
import './cabinet.css';

function QuickAction({ to, icon, title, text }) {
  return (
    <Link to={to} className="quick-action">
      <span className="quick-action__icon">
        <Icon name={icon} size={18} />
      </span>
      <span>
        <span className="quick-action__title">{title}</span>
        <span className="quick-action__text">{text}</span>
      </span>
    </Link>
  );
}

export function EcoTiles({ stats }) {
  return (
    <div className="stat-tiles">
      <div className="stat-tile stat-tile--green">
        <Icon name="leaf" size={18} className="stat-tile__icon" />
        <div className="stat-tile__label">Спасено кг текстиля</div>
        <div className="stat-tile__value">{formatKg(stats.savedKg)} кг</div>
        <div className="stat-tile__note">Ваш эко-вклад · за всё время</div>
      </div>
      <div className="stat-tile">
        <Icon name="repeat" size={18} className="stat-tile__icon muted" />
        <div className="stat-tile__label muted">Состоявшиеся обмены</div>
        <div className="stat-tile__value">{stats.exchangesCount}</div>
        <div className="stat-tile__note">Спасибо, что делитесь, а не выбрасываете</div>
      </div>
    </div>
  );
}

function UserCabinet() {
  const [listings, setListings] = useState(null);
  const [stats, setStats] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    let cancelled = false;
    Promise.all([api.getMyListings('ACTIVE'), api.getMyStats()])
      .then(([items, myStats]) => {
        if (cancelled) return;
        setListings(items);
        setStats(myStats);
      })
      .catch((e) => {
        if (!cancelled) setError(e.message);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <>
      <section className="section">
        <h2 className="section-title section__head">Быстрые действия</h2>
        <div className="quick-actions">
          <QuickAction to="/listings/new" icon="plus" title="Создать объявление" text="Поделитесь тем, что больше не нужно" />
          <QuickAction to="/listings" icon="search" title="Найти вещь" text="Нужное уже есть у ваших соседей" />
          <QuickAction to="/map" icon="mapPin" title="Найти пункт обмена" text="Передайте вещи в добрые руки" />
        </div>
      </section>

      <section className="section">
        <div className="section__head">
          <h2 className="section-title">Мои активные объявления</h2>
          {listings && listings.length > 0 && (
            <Link to="/my-listings" className="link-arrow">
              Все объявления · {stats.listingsCount} →
            </Link>
          )}
        </div>
        {error && <div className="alert alert--error">Не удалось загрузить объявления: {error}</div>}
        {!listings && !error && <PageLoader />}
        {listings && listings.length === 0 && (
          <div className="empty">
            <Icon name="layers" size={28} />
            У вас пока нет активных объявлений
            <Link to="/listings/new" className="btn btn--primary">
              <Icon name="plus" size={16} /> Создать объявление
            </Link>
          </div>
        )}
        {listings && listings.length > 0 && (
          <div className="my-cards">
            {listings.slice(0, 3).map((listing) => (
              <ListingCard
                key={listing.id}
                listing={listing}
                actionLabel="Управлять"
                actionTo={`/listings/${listing.id}/edit`}
              />
            ))}
          </div>
        )}
      </section>

      {stats && (
        <section className="section">
          <EcoTiles stats={stats} />
        </section>
      )}
    </>
  );
}

function OrganizerCabinet({ point }) {
  if (!point) {
    return (
      <section className="section">
        <div className="empty">
          <Icon name="store" size={28} />
          Карточка пункта ещё не заполнена
          <Link to="/organizer/profile" className="btn btn--primary">
            Заполнить карточку
          </Link>
        </div>
      </section>
    );
  }
  const accepted = point.acceptedTypes
    .map((value) => ITEM_TYPES.find((type) => type.value === value)?.label)
    .filter(Boolean);
  return (
    <>
      <section className="section">
        <div className="section__head">
          <h2 className="section-title">Карточка пункта</h2>
          <Link to="/organizer/profile" className="link-arrow">
            Профиль организатора →
          </Link>
        </div>
        <div className="panel cabinet-point">
          <div className="cabinet-point__photo">
            {point.photoUrl ? (
              <img src={point.photoUrl} alt={point.name} />
            ) : (
              <div className="listing-card__noimage">
                <Icon name="store" size={40} />
              </div>
            )}
          </div>
          <div>
            {point.status === 'ACTIVE' ? (
              <StatusBadge tone="green">
                <Icon name="checkCircle" size={13} /> Активен
              </StatusBadge>
            ) : (
              <StatusBadge tone="amber">Временно закрыт</StatusBadge>
            )}
            <h3 className="cabinet-point__name">{point.name}</h3>
            {point.description && <p className="cabinet-point__desc">{point.description}</p>}
            <div className="cabinet-point__line">
              <Icon name="mapPin" size={16} /> {point.address}
            </div>
            <div className="cabinet-point__line">
              <Icon name="clock" size={16} /> {point.workingHours}
            </div>
            <div className="cabinet-point__line">
              <Icon name="phone" size={16} /> {point.phone}
            </div>
            <div className="cabinet-point__actions">
              <Link to="/organizer/profile" className="btn btn--primary">
                <Icon name="pencil" size={16} /> Редактировать
              </Link>
              <Link to="/map" className="btn btn--soft">
                <Icon name="map" size={16} /> Посмотреть на карте
              </Link>
            </div>
          </div>
        </div>
      </section>
      <div className="accepts">
        <Icon name="package" size={26} />
        <div>
          <div className="accepts__title">Сейчас принимаем</div>
          <div className="accepts__list">
            {accepted.length ? accepted.join(' · ') : 'Типы вещей пока не указаны'}
          </div>
        </div>
      </div>
    </>
  );
}

export default function CabinetPage() {
  const { user, point } = useAuth();
  const isOrganizer = user.role === 'ORGANIZER';
  const name = firstName(user.name);
  return (
    <>
      <div className="cabinet-head">
        <div>
          <h1 className="page-title">
            {greeting()}
            {name ? `, ${name}` : ''}!
          </h1>
          <p className="page-subtitle">
            {isOrganizer ? 'Ваш пункт помогает вещам находить новый дом.' : 'Рады видеть вас. Давайте сделаем ещё один добрый обмен.'}
          </p>
        </div>
        <StatusBadge tone="green">{isOrganizer ? 'Организатор' : 'Пользователь'}</StatusBadge>
      </div>
      {isOrganizer ? <OrganizerCabinet point={point} /> : <UserCabinet />}
    </>
  );
}
