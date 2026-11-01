import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './auth/AuthContext';
import AppLayout from './components/AppLayout';
import CabinetPage from './pages/CabinetPage';
import HomePage from './pages/HomePage';
import ListingFormPage from './pages/ListingFormPage';
import LoginPage from './pages/LoginPage';
import MyListingsPage from './pages/MyListingsPage';
import OrganizerProfilePage from './pages/OrganizerProfilePage';
import ProfilePage from './pages/ProfilePage';
import RegisterPage from './pages/RegisterPage';

function GuestOnly({ children }) {
  const { user, loading } = useAuth();
  if (loading) return <div className="app-loading">Загрузка…</div>;
  if (user) return <Navigate to="/cabinet" replace />;
  return children;
}

// Страницы остальных участников (вход, регистрация, лента, карта, кабинет и т. д.) добавляются сюда же.
function App() {
  return (
    <Routes>
      <Route path="/" element={<HomePage />} />
      <Route
        path="/login"
        element={
          <GuestOnly>
            <LoginPage />
          </GuestOnly>
        }
      />
      <Route
        path="/register"
        element={
          <GuestOnly>
            <RegisterPage />
          </GuestOnly>
        }
      />
      <Route element={<AppLayout />}>
        <Route path="/cabinet" element={<CabinetPage />} />
      </Route>
      <Route element={<AppLayout allowedRoles={['USER']} />}>
        <Route path="/profile" element={<ProfilePage />} />
        <Route path="/my-listings" element={<MyListingsPage />} />
        <Route path="/listings/new" element={<ListingFormPage />} />
        <Route path="/listings/:id/edit" element={<ListingFormPage />} />
      </Route>
      <Route element={<AppLayout allowedRoles={['ORGANIZER']} />}>
        <Route path="/organizer/profile" element={<OrganizerProfilePage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

export default App;
