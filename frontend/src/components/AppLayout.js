import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import Header from './Header';
import Sidebar from './Sidebar';
import './AppLayout.css';

/**
 * Каркас страниц личного кабинета: шапка + боковое меню. Только для вошедших;
 * allowedRoles ограничивает роли (настоящая проверка прав — на бэкенде, здесь только маршрутизация).
 */
export default function AppLayout({ allowedRoles }) {
  const { user, loading } = useAuth();

  if (loading) return <div className="app-loading">Загрузка…</div>;
  if (!user) return <Navigate to="/login" replace />;
  if (allowedRoles && !allowedRoles.includes(user.role)) return <Navigate to="/" replace />;

  return (
    <div className="app-layout">
      <Header />
      <div className="app-layout__body">
        <Sidebar />
        <main className="app-layout__main">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
