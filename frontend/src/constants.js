import { Package } from 'lucide-react';
import { ITEM_TYPES } from './utils/itemTypes';

export { ITEM_TYPES };

export const CATEGORIES = [
  { value: 'ADULT', label: 'Взрослое' },
  { value: 'CHILDREN', label: 'Детское' },
];

export const CONDITIONS = [
  { value: 'NEW', label: 'Новое' },
  { value: 'GOOD', label: 'Хорошее' },
  { value: 'NEEDS_REPAIR', label: 'Требует ремонта' },
];

export const EXCHANGE_FORMATS = [
  { value: 'FREE', label: 'Бесплатно' },
  { value: 'SWAP', label: 'Обмен' },
];

export const LISTING_STATUSES = {
  ACTIVE: { label: 'Активно', tone: 'green' },
  DRAFT: { label: 'Черновик', tone: 'gray' },
  HIDDEN: { label: 'Скрыто', tone: 'amber' },
  TRANSFERRED: { label: 'Передано', tone: 'blue' },
};

export const CITIES = [
  'Москва',
  'Санкт-Петербург',
  'Екатеринбург',
  'Новосибирск',
  'Казань',
  'Нижний Новгород',
  'Челябинск',
  'Самара',
  'Омск',
  'Ростов-на-Дону',
  'Уфа',
  'Красноярск',
  'Пермь',
  'Воронеж',
  'Волгоград',
  'Краснодар',
  'Тюмень',
  'Пенза',
  'Тверь',
  'Череповец',
  'Петрозаводск',
];

export const MAX_PHOTOS = 5;
export const MAX_PHOTO_BYTES = 10 * 1024 * 1024;
export const MAX_AVATAR_BYTES = 5 * 1024 * 1024;
export const IMAGE_TYPES = ['image/jpeg', 'image/png'];

export function labelOf(list, value) {
  return list.find((item) => item.value === value)?.label ?? '';
}

export function itemTypeOf(value) {
  return ITEM_TYPES.find((item) => item.value === value) ?? { label: '', Icon: Package };
}
