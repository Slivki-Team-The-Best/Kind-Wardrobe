export const TOKEN_KEY = 'accessToken';

export class ApiError extends Error {
  constructor(status, detail, fieldErrors = {}) {
    super(detail);
    this.status = status;
    this.fieldErrors = fieldErrors;
  }
}

export function getToken() {
  try {
    return localStorage.getItem(TOKEN_KEY);
  } catch {
    return null;
  }
}

let onUnauthorized = () => {};

/** AuthProvider регистрирует сюда выход из аккаунта на случай 401 (токен истёк или невалиден). */
export function setUnauthorizedHandler(handler) {
  onUnauthorized = handler;
}

export async function apiFetch(path, { method = 'GET', body } = {}) {
  const token = getToken();
  const isForm = body instanceof FormData;
  const headers = { Accept: 'application/json' };
  if (body !== undefined && !isForm) headers['Content-Type'] = 'application/json';
  if (token) headers.Authorization = `Bearer ${token}`;

  let response;
  try {
    response = await fetch(path, {
      method,
      headers,
      body: body === undefined || isForm ? body : JSON.stringify(body),
    });
  } catch {
    throw new ApiError(0, 'Нет связи с сервером. Попробуйте позже');
  }

  const data = await response.json().catch(() => null);
  if (response.status === 401 && token) onUnauthorized();
  if (!response.ok) {
    const fallback = response.status === 413 ? 'Файлы слишком большие' : 'Что-то пошло не так. Попробуйте позже';
    throw new ApiError(response.status, data?.detail || fallback, data?.errors);
  }
  return data;
}
