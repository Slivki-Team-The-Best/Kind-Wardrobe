import { todayHours } from './workingHours';

// 2026-11-02 — понедельник, 2026-11-08 — воскресенье
const monday = new Date(2026, 10, 2, 12, 0);
const sunday = new Date(2026, 10, 8, 12, 0);

test('рабочий день диапазона даёт «Сегодня» с временем', () => {
  expect(todayHours('Пн–Сб 10:00–20:00', monday)).toBe('Сегодня 10:00–20:00');
});

test('день вне диапазона — выходной', () => {
  expect(todayHours('Пн–Сб 10:00–20:00', sunday)).toBe('Сегодня выходной');
});

test('«Ежедневно» работает каждый день', () => {
  expect(todayHours('Ежедневно 9:00–21:00', sunday)).toBe('Сегодня 9:00–21:00');
});

test('диапазон через воскресенье и одиночный день', () => {
  expect(todayHours('Сб–Пн 11:00–18:00', monday)).toBe('Сегодня 11:00–18:00');
  expect(todayHours('Вт 11:00–18:00', monday)).toBe('Сегодня выходной');
});

test('неразобранная строка возвращается как есть', () => {
  expect(todayHours('по договорённости', monday)).toBe('по договорённости');
  expect(todayHours('', monday)).toBe('');
});
