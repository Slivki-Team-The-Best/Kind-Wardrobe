import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { ApiError, TOKEN_KEY, getToken, setUnauthorizedHandler } from '../api/client';
import { getOrganizerPoint, getProfile } from '../api';

const AuthContext = createContext(null);

function storeToken(token) {
  try {
    if (token) localStorage.setItem(TOKEN_KEY, token);
    else localStorage.removeItem(TOKEN_KEY);
  } catch {
    // хранилище недоступно (приватный режим) — вход тогда живёт только до перезагрузки
  }
}

/**
 * Состояние входа: токен из localStorage, профиль (GET /api/users/me) и, для организатора, его пункт.
 * Страница входа вызывает login(accessToken) с токеном из ответа POST /api/auth/login.
 */
export function AuthProvider({ children }) {
  const [token, setToken] = useState(getToken);
  const [user, setUser] = useState(null);
  const [point, setPoint] = useState(null);
  const [loading, setLoading] = useState(Boolean(getToken()));

  const logout = useCallback(() => {
    storeToken(null);
    setToken(null);
    setUser(null);
    setPoint(null);
  }, []);

  const login = useCallback((accessToken) => {
    storeToken(accessToken);
    setLoading(true);
    setToken(accessToken);
  }, []);

  useEffect(() => {
    setUnauthorizedHandler(logout);
  }, [logout]);

  useEffect(() => {
    if (!token) {
      setLoading(false);
      return undefined;
    }
    let cancelled = false;
    (async () => {
      try {
        const profile = await getProfile();
        const organizerPoint =
          profile.role === 'ORGANIZER'
            ? await getOrganizerPoint().catch((e) => {
                if (e instanceof ApiError && e.status === 404) return null;
                throw e;
              })
            : null;
        if (!cancelled) {
          setUser(profile);
          setPoint(organizerPoint);
        }
      } catch {
        // 401 уже обработан (logout); при другой ошибке считаем пользователя не вошедшим
        if (!cancelled) {
          setUser(null);
          setPoint(null);
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [token]);

  const value = useMemo(
    () => ({ token, user, setUser, point, setPoint, loading, login, logout }),
    [token, user, point, loading, login, logout]
  );
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth нужно вызывать внутри AuthProvider');
  return context;
}
