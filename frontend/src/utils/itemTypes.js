import { Armchair, BookOpen, Blocks, Footprints, Shirt, Tv } from 'lucide-react';

/** Типы вещей, как в enum ItemType на бэкенде. Порядок = порядок чекбоксов в макете. */
export const ITEM_TYPES = [
  { value: 'CLOTHES', label: 'Одежда', Icon: Shirt },
  { value: 'SHOES', label: 'Обувь', Icon: Footprints },
  { value: 'TOYS', label: 'Игрушки', Icon: Blocks },
  { value: 'BOOKS', label: 'Книги', Icon: BookOpen },
  { value: 'FURNITURE', label: 'Мебель', Icon: Armchair },
  { value: 'ELECTRONICS', label: 'Техника', Icon: Tv },
];
