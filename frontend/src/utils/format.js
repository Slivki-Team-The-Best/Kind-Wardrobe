export function phoneDigits(value) {
  let digits = (value || '').replace(/\D/g, '');
  if (!digits) return '';
  if (digits[0] === '8') digits = `7${digits.slice(1)}`;
  if (digits[0] !== '7') digits = `7${digits}`;
  return digits.slice(0, 11);
}

export function formatPhone(value) {
  const digits = phoneDigits(value);
  if (!digits) return '';
  const rest = digits.slice(1);
  let result = '+7';
  if (rest.length > 0) result += ` (${rest.slice(0, 3)}`;
  if (rest.length >= 3) result += ')';
  if (rest.length > 3) result += ` ${rest.slice(3, 6)}`;
  if (rest.length > 6) result += `-${rest.slice(6, 8)}`;
  if (rest.length > 8) result += `-${rest.slice(8, 10)}`;
  return result;
}

export function isPhoneComplete(value) {
  return phoneDigits(value).length === 11;
}

export function isEmail(value) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test((value || '').trim());
}

export function formatKg(value) {
  const number = Number(value) || 0;
  return number.toLocaleString('ru-RU', { maximumFractionDigits: 1 });
}

export function formatNumber(value) {
  return (Number(value) || 0).toLocaleString('ru-RU');
}

export function formatDate(value) {
  if (!value) return '';
  return new Date(value).toLocaleDateString('ru-RU', { day: 'numeric', month: 'long', year: 'numeric' });
}

export function formatMonthYear(value) {
  if (!value) return '';
  return new Date(value).toLocaleDateString('ru-RU', { month: 'long', year: 'numeric' });
}

export function relativeDay(value) {
  if (!value) return '';
  const date = new Date(value);
  const today = new Date();
  const startOf = (d) => new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime();
  const diffDays = Math.round((startOf(today) - startOf(date)) / 86400000);
  if (diffDays === 0) return 'сегодня';
  if (diffDays === 1) return 'вчера';
  return date.toLocaleDateString('ru-RU', { day: 'numeric', month: 'long' });
}

export function greeting(date = new Date()) {
  const hour = date.getHours();
  if (hour < 5) return 'Доброй ночи';
  if (hour < 12) return 'Доброе утро';
  if (hour < 18) return 'Добрый день';
  return 'Добрый вечер';
}

export function plural(count, one, few, many) {
  const n = Math.abs(count) % 100;
  const n1 = n % 10;
  if (n > 10 && n < 20) return many;
  if (n1 > 1 && n1 < 5) return few;
  if (n1 === 1) return one;
  return many;
}

export function firstName(name) {
  return (name || '').trim().split(/\s+/)[0] || '';
}

export function displayName(user) {
  if (!user) return '';
  if (user.role === 'ORGANIZER') {
    const pointName = user.point?.name;
    if (user.name && pointName) return `${firstName(user.name)} · ${pointName}`;
    return pointName || user.name || user.email;
  }
  return user.name || user.email;
}
