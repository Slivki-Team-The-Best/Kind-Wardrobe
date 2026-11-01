const DAYS = ['Пн', 'Вт', 'Ср', 'Чт', 'Пт', 'Сб', 'Вс'];
const TIME_RANGE = /(\d{1,2}[:.]\d{2})\s*[–—-]\s*(\d{1,2}[:.]\d{2})/;
const DAY_RANGE = new RegExp(`(${DAYS.join('|')})(?:\\s*[–—-]\\s*(${DAYS.join('|')}))?`);

function workingDays(text) {
  if (/ежедневно|круглосуточно/i.test(text)) return new Set(DAYS.keys());
  const match = text.match(DAY_RANGE);
  if (!match) return new Set(DAYS.keys());
  const from = DAYS.indexOf(match[1]);
  const to = match[2] ? DAYS.indexOf(match[2]) : from;
  const days = new Set();
  for (let i = from; ; i = (i + 1) % 7) {
    days.add(i);
    if (i === to) break;
  }
  return days;
}

/**
 * «Пн–Сб 10:00–20:00» → «Сегодня 10:00–20:00» (или «Сегодня выходной»).
 * Если строку не удалось разобрать, возвращается как есть: организатор вводит часы в свободной форме.
 */
export function todayHours(text, now = new Date()) {
  if (!text) return '';
  const time = text.match(TIME_RANGE);
  if (!time) return text;
  const todayIndex = (now.getDay() + 6) % 7; // в JS воскресенье = 0, у нас понедельник = 0
  return workingDays(text).has(todayIndex) ? `Сегодня ${time[1]}–${time[2]}` : 'Сегодня выходной';
}
