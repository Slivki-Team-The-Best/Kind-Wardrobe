import { CircleCheck, Clock, Info, Store } from 'lucide-react';
import { useEffect } from 'react';
import { useLocation } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import PointForm from '../components/PointForm';
import { todayHours } from '../utils/workingHours';
import './OrganizerProfilePage.css';

const STATUS = {
  ACTIVE: { label: 'Активен', className: 'point-status' },
  TEMPORARILY_CLOSED: { label: 'Временно закрыт', className: 'point-status point-status--closed' },
};

function PointPreview({ point }) {
  if (!point) {
    return (
      <section className="preview preview--empty">
        <Store size={32} strokeWidth={1.5} />
        <p>Заполните данные пункта, чтобы он появился на карте.</p>
      </section>
    );
  }
  const status = STATUS[point.status] ?? STATUS.ACTIVE;
  return (
    <section className="preview" aria-label="Так пункт видят посетители">
      {point.photoUrl ? (
        <img className="preview__photo" src={point.photoUrl} alt={point.name} />
      ) : (
        <div className="preview__photo preview__photo--empty" aria-hidden="true">
          <Store size={40} strokeWidth={1.25} />
        </div>
      )}
      <div className="preview__body">
        <span className={status.className}>
          <CircleCheck size={14} strokeWidth={1.75} /> {status.label}
        </span>
        <h2 className="preview__name">{point.name}</h2>
        <p className="preview__address">{point.address}</p>
        <p className="preview__hours">
          <Clock size={16} strokeWidth={1.75} /> {todayHours(point.workingHours)}
        </p>
      </div>
    </section>
  );
}

/** «Профиль организатора»: форма данных пункта слева, карточка для посетителей справа. */
export default function OrganizerProfilePage() {
  const { point, setPoint } = useAuth();
  const { hash } = useLocation();

  // «Мой пункт» в боковом меню ведёт на #point — к форме пункта
  useEffect(() => {
    if (hash === '#point') document.getElementById('point')?.scrollIntoView({ block: 'start' });
  }, [hash]);

  return (
    <>
      <div className="page-head">
        <div>
          <h1 className="page-head__title">Профиль организатора</h1>
          <p className="page-head__subtitle">Информация, которую видят посетители вашего пункта.</p>
        </div>
        <span className="role-badge">Организатор</span>
      </div>

      <div className="organizer">
        <PointForm point={point} onSaved={setPoint} />
        <div className="organizer__side">
          <PointPreview point={point} />
          <p className="hint">
            <Info size={16} strokeWidth={1.75} />
            Проверяйте адрес и часы работы: так посетители смогут заранее спланировать свой визит.
          </p>
        </div>
      </div>
    </>
  );
}
