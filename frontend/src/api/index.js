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

export const register = (data) => apiFetch('/api/auth/register', { method: 'POST', body: data });

export const getMyStats = () => apiFetch('/api/users/me/stats');

export const uploadAvatar = (file) => {
  const form = new FormData();
  form.append('avatar', file);
  return apiFetch('/api/users/me/avatar', { method: 'POST', body: form });
};

function photosForm(files) {
  const form = new FormData();
  files.forEach((file) => form.append('photos', file));
  return form;
}

export const createListing = (data, files) => {
  const form = photosForm(files);
  form.append('listing', new Blob([JSON.stringify(data)], { type: 'application/json' }));
  return apiFetch('/api/listings', { method: 'POST', body: form });
};

export const getListing = (id) => apiFetch(`/api/listings/${id}`);

export const updateListing = (id, data) => apiFetch(`/api/listings/${id}`, { method: 'PUT', body: data });

export const deleteListing = (id) => apiFetch(`/api/listings/${id}`, { method: 'DELETE' });

export const uploadListingPhotos = (id, files) =>
  apiFetch(`/api/listings/${id}/photos`, { method: 'POST', body: photosForm(files) });

export const deleteListingPhoto = (id, photoId) =>
  apiFetch(`/api/listings/${id}/photos/${photoId}`, { method: 'DELETE' });

export const getMyListings = (status) => apiFetch(status ? `/api/listings/my?status=${status}` : '/api/listings/my');
