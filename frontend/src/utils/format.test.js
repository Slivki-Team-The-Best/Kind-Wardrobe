import { formatPhone, isPhoneComplete, phoneDigits, plural, relativeDay } from './format';

test('formats russian phone numbers while typing', () => {
  expect(formatPhone('9')).toBe('+7 (9');
  expect(formatPhone('8916555')).toBe('+7 (916) 555');
  expect(formatPhone('+7 (916) 555-12-34')).toBe('+7 (916) 555-12-34');
  expect(phoneDigits('8 916 555 12 34 99')).toBe('79165551234');
  expect(isPhoneComplete('+7 (916) 555-12')).toBe(false);
  expect(isPhoneComplete('89165551234')).toBe(true);
});

test('pluralizes russian words', () => {
  expect(plural(1, 'обмен', 'обмена', 'обменов')).toBe('обмен');
  expect(plural(3, 'обмен', 'обмена', 'обменов')).toBe('обмена');
  expect(plural(11, 'обмен', 'обмена', 'обменов')).toBe('обменов');
  expect(plural(25, 'обмен', 'обмена', 'обменов')).toBe('обменов');
});

test('shows today as a word', () => {
  expect(relativeDay(new Date().toISOString())).toBe('сегодня');
});
