import { Bell, ChevronDown, LogOut, Search, User } from 'lucide-react';
import { useState } from 'react';
import { Link, NavLink } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import Logo from './Logo';
import './Header.css';

function initials(name, email) {
  const source = (name || email || '?').trim();
  return source.slice(0, 1).toUpperCase();
}

function UserMenu({ user, point, onLogout }) {
  const [open, setOpen] = useState(false);
  const isOrganizer = user.role === 'ORGANIZER';
  const firstName = (user.name || user.email).split(' ')[0];
  const title = isOrganizer && point ? `${firstName} · ${point.name}` : user.name || user.email;

  return (
    <div className="user-menu">
      <button
        type="button"
        className="user-menu__button"
        aria-expanded={open}
        aria-haspopup="menu"
        onClick={() => setOpen((value) => !value)}
      >
        {user.avatarUrl ? (
          <img className="avatar" src={user.avatarUrl} alt="" />
        ) : (
          <span className="avatar avatar--initials">{initials(user.name, user.email)}</span>
        )}
        <span className="user-menu__text">
          <span className="user-menu__name">{title}</span>
          <span className="user-menu__role">{isOrganizer ? 'Организатор' : 'Личный кабинет'}</span>
        </span>
        <ChevronDown size={16} className="user-menu__chevron" />
      </button>
      {open && (
        <div className="user-menu__dropdown" role="menu">
          <Link
            to={isOrganizer ? '/organizer/profile' : '/profile'}
            role="menuitem"
            className="user-menu__item"
            onClick={() => setOpen(false)}
          >
            <User size={16} /> Профиль
          </Link>
          <button type="button" role="menuitem" className="user-menu__item" onClick={onLogout}>
            <LogOut size={16} /> Выйти
          </button>
        </div>
      )}
    </div>
  );
}

/** Шапка: для гостя — «Войти / Зарегистрироваться», для вошедшего — поиск, уведомления и меню пользователя. */
export default function Header() {
  const { user, point, logout } = useAuth();

  return (
    <header className="header">
      <Logo />
      <div className="header__center">
        <nav className="header__nav" aria-label="Основная навигация">
          <NavLink to="/listings">Объявления</NavLink>
          <NavLink to="/map">Пункты обмена</NavLink>
        </nav>
        {user && (
          <label className="search">
            <Search size={18} strokeWidth={1.75} />
            <input type="search" placeholder="Что вы ищете?" aria-label="Поиск" />
          </label>
        )}
      </div>
      <div className="header__right">
        {user ? (
          <>
            <button type="button" className="icon-button" aria-label="Уведомления">
              <Bell size={20} strokeWidth={1.75} />
            </button>
            <span className="header__divider" aria-hidden="true" />
            <UserMenu user={user} point={point} onLogout={logout} />
          </>
        ) : (
          <>
            <Link to="/login" className="header__login">Войти</Link>
            <Link to="/register" className="btn btn--primary">Зарегистрироваться</Link>
          </>
        )}
      </div>
    </header>
  );
}
