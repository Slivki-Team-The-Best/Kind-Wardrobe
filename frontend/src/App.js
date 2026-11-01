import { Navigate, Route, Routes } from 'react-router-dom';
import AppLayout from './components/AppLayout';
import HomePage from './pages/HomePage';
import OrganizerProfilePage from './pages/OrganizerProfilePage';

// Страницы остальных участников (вход, регистрация, лента, карта, кабинет и т. д.) добавляются сюда же.
function App() {
  return (
    <Routes>
      <Route path="/" element={<HomePage />} />
      <Route element={<AppLayout allowedRoles={['ORGANIZER']} />}>
        <Route path="/organizer/profile" element={<OrganizerProfilePage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

export default App;
