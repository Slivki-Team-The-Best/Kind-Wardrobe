import { Link } from 'react-router-dom';
import { EXCHANGE_FORMATS, LISTING_STATUSES, itemTypeOf, labelOf } from '../constants';
import { relativeDay } from '../utils/format';
import { StatusBadge } from './Fields';
import Icon from './Icon';

export default function ListingCard({ listing, actionLabel, actionTo, showStatus }) {
  const type = itemTypeOf(listing.itemType);
  const status = LISTING_STATUSES[listing.status];
  return (
    <article className="listing-card">
      <div className="listing-card__image">
        {listing.coverUrl ? (
          <img src={listing.coverUrl} alt={listing.title} loading="lazy" />
        ) : (
          <div className="listing-card__noimage">
            <Icon name="image" size={32} />
          </div>
        )}
        {showStatus && status && (
          <span className="listing-card__status">
            <StatusBadge tone={status.tone}>{status.label}</StatusBadge>
          </span>
        )}
      </div>
      <div className="listing-card__body">
        <div className="listing-card__title" title={listing.title}>
          {listing.title}
        </div>
        <div className="listing-card__row">
          <span className="listing-card__meta">
            <type.Icon size={14} strokeWidth={1.75} aria-hidden="true" /> {type.label}
          </span>
          <span className={`format--${listing.exchangeFormat === 'FREE' ? 'free' : 'swap'}`}>
            {labelOf(EXCHANGE_FORMATS, listing.exchangeFormat)}
          </span>
        </div>
        <div className="listing-card__row">
          <span className="listing-card__meta">
            <Icon name="mapPin" size={14} /> {listing.city} ·{' '}
            {listing.status === 'ACTIVE' ? relativeDay(listing.publishedAt) : status?.label}
          </span>
        </div>
        <Link to={actionTo} className="btn listing-card__action">
          {actionLabel}
        </Link>
      </div>
    </article>
  );
}
