import { apiFetch } from './client';

export const login = (identifier, password) =>
  apiFetch('/api/auth/login', { method: 'POST', body: { identifier, password } });

export const getProfile = () => apiFetch('/api/users/me');

/** name, city, phone — email и роль через профиль не меняются. */
export const updateProfile = ({ name, city, phone }) =>
  apiFetch('/api/users/me', { method: 'PUT', body: { name, city, phone } });

/** Пункт текущего организатора. 404, пока пункт не создан. */
export const getOrganizerPoint = () => apiFetch('/api/organizer/point');

export const saveOrganizerPoint = (point) =>
  apiFetch('/api/organizer/point', { method: 'PUT', body: point });
