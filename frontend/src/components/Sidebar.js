import { Layers, LayoutGrid, Leaf, Map, SlidersHorizontal, Sprout, Store, User } from 'lucide-react';
import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import './Sidebar.css';

function menuFor(role) {
  const isOrganizer = role === 'ORGANIZER';
  const items = [
    { to: '/cabinet', label: 'Личный кабинет', Icon: LayoutGrid },
    { to: isOrganizer ? '/organizer/profile' : '/profile', label: 'Профиль', Icon: User },
    { to: '/my-listings', label: 'Мои объявления', Icon: Layers },
    { to: '/map', label: 'Карта пунктов', Icon: Map },
    { to: '/eco', label: 'Эко-трекер', Icon: Leaf },
    { to: '/settings', label: 'Настройки', Icon: SlidersHorizontal },
  ];
  // «Мой пункт» ведёт к форме пункта на странице профиля организатора
  if (isOrganizer) items.push({ to: '/organizer/profile#point', label: 'Мой пункт', Icon: Store });
  return items;
}

function isActive(item, location) {
  const [path, hash] = item.to.split('#');
  if (hash) return location.pathname === path && location.hash === `#${hash}`;
  return location.pathname === path && location.hash !== '#point';
}

export default function Sidebar() {
  const { user } = useAuth();
  const location = useLocation();

  return (
    <aside className="sidebar">
      <p className="sidebar__label">МОЙ ДОБРЫЙ ШКАФ</p>
      <nav aria-label="Личный кабинет">
        <ul className="sidebar__list">
          {menuFor(user?.role).map((item) => (
            <li key={item.to}>
              <Link
                to={item.to}
                className={`sidebar__link${isActive(item, location) ? ' sidebar__link--active' : ''}`}
                aria-current={isActive(item, location) ? 'page' : undefined}
              >
                <item.Icon size={20} strokeWidth={1.5} />
                {item.label}
              </Link>
            </li>
          ))}
        </ul>
      </nav>
      {/* TODO: подставлять общий вклад платформы из API, когда появится эко-трекер */}
      <div className="sidebar__card">
        <Sprout size={22} strokeWidth={1.5} />
        <p className="sidebar__card-title">Маленький обмен — большое добро</p>
        <p className="sidebar__card-text">Вместе мы сохранили 18 460 кг вещей.</p>
      </div>
    </aside>
  );
}
