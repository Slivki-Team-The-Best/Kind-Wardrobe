import {
  ArrowRight,
  ArrowUpRight,
  BookOpen,
  Armchair,
  CircleCheck,
  Footprints,
  Heart,
  Leaf,
  MapPin,
  MessageSquare,
  Search,
  Shirt,
  Store,
} from 'lucide-react';
import { Link } from 'react-router-dom';
import Header from '../components/Header';
import jacket from '../assets/demo/jacket.jpg';
import books from '../assets/demo/books.jpg';
import chair from '../assets/demo/chair.jpg';
import sneakers from '../assets/demo/sneakers.jpg';
import './HomePage.css';

// TODO: статистику, объявления и пункты брать из API, когда появятся ленточные эндпоинты.
const stats = [
  { value: '1 250', label: 'пользователей' },
  { value: '3 400', label: 'обменов' },
  { value: '8 700 кг', label: 'спасённых вещей', accent: true },
];

const steps = [
  { Icon: Search, title: 'Найди вещь или пункт', text: 'Выбирайте рядом с домом' },
  { Icon: MessageSquare, title: 'Свяжись через мессенджер', text: 'Договоритесь об удобной встрече' },
  { Icon: CircleCheck, title: 'Отметь обмен', text: 'Добавьте результат в свой эко-вклад' },
];

const popular = [
  { id: 1, photo: jacket, title: 'Куртка-рубашка, размер M', type: 'Одежда', TypeIcon: Shirt, format: 'Бесплатно' },
  { id: 2, photo: books, title: 'Книги для маленьких читателей', type: 'Книги', TypeIcon: BookOpen, format: 'Обмен' },
  { id: 3, photo: chair, title: 'Стул из натурального дерева', type: 'Мебель', TypeIcon: Armchair, format: 'Бесплатно' },
  { id: 4, photo: sneakers, title: 'Белые кеды, размер 38', type: 'Обувь', TypeIcon: Footprints, format: 'Обмен' },
];

const nearest = [
  { id: 1, name: 'Тёплый круг', place: 'ул. Покровка, 27 · 1,2 км', info: 'Открыто до 20:00 · Одежда, обувь, книги' },
  { id: 2, name: 'Полка добра', place: 'ул. Маросейка, 8 · 1,8 км', info: 'Открыто до 20:00 · Одежда, обувь, книги' },
  { id: 3, name: 'Соседский обмен', place: 'Чистопрудный б-р, 12 · 2,1 км', info: 'Открыто до 20:00 · Одежда, обувь, книги' },
];

export default function HomePage() {
  return (
    <div className="home">
      <Header />

      <main className="home__main">
        <section className="hero">
          <div className="hero__intro">
            <p className="hero__eyebrow">
              <Leaf size={16} strokeWidth={1.75} /> ВЕЩАМ — ВТОРУЮ ЖИЗНЬ
            </p>
            <h1 className="hero__title">Добрый Шкаф. Делитесь добром.</h1>
            <p className="hero__text">
              Отдавайте ненужное, находите нужное и обменивайтесь с соседями.
              <br />
              Вещи продолжают служить, а вы видите свой экологический вклад.
            </p>
            <ul className="hero__stats">
              {stats.map((item) => (
                <li key={item.label} className="hero__stat">
                  <span className={item.accent ? 'hero__stat-value hero__stat-value--accent' : 'hero__stat-value'}>
                    {item.value}
                  </span>
                  <span className="hero__stat-label">{item.label}</span>
                </li>
              ))}
            </ul>
          </div>

          <aside className="how" aria-labelledby="how-title">
            <h2 id="how-title" className="how__title">Как это работает</h2>
            <ol className="how__list">
              {steps.map(({ Icon, title, text }) => (
                <li key={title} className="how__item">
                  <Icon size={22} strokeWidth={1.5} className="how__icon" />
                  <div>
                    <p className="how__item-title">{title}</p>
                    <p className="how__item-text">{text}</p>
                  </div>
                </li>
              ))}
            </ol>
          </aside>
        </section>

        <section className="block" aria-labelledby="popular-title">
          <div className="block__head">
            <h2 id="popular-title" className="block__title">Популярные объявления</h2>
            <Link to="/listings" className="link-blue block__link">
              Все объявления <ArrowRight size={14} />
            </Link>
          </div>
          <ul className="cards">
            {popular.map(({ id, photo, title, type, TypeIcon, format }) => (
              <li key={id} className="listing">
                <div className="listing__photo">
                  <img src={photo} alt={title} />
                  <button type="button" className="listing__like" aria-label="Добавить в избранное">
                    <Heart size={16} strokeWidth={1.75} />
                  </button>
                </div>
                <h3 className="listing__title">{title}</h3>
                <p className="listing__row">
                  <span className="listing__meta">
                    <TypeIcon size={15} strokeWidth={1.5} /> {type}
                  </span>
                  <span className="listing__format">{format}</span>
                </p>
                <p className="listing__meta">
                  <MapPin size={15} strokeWidth={1.5} /> Москва · сегодня
                </p>
                <Link to={`/listings/${id}`} className="listing__more">Подробнее</Link>
              </li>
            ))}
          </ul>
        </section>

        <section className="block" aria-labelledby="points-title">
          <div className="block__head">
            <h2 id="points-title" className="block__title">Ближайшие пункты обмена</h2>
            <Link to="/map" className="link-blue block__link">
              Открыть карту <ArrowRight size={14} />
            </Link>
          </div>
          <ul className="points">
            {nearest.map((point) => (
              <li key={point.id}>
                <Link to="/map" className="point-card">
                  <span className="point-card__icon">
                    <Store size={22} strokeWidth={1.5} />
                  </span>
                  <span className="point-card__body">
                    <span className="point-card__name">{point.name}</span>
                    <span className="point-card__place">{point.place}</span>
                    <span className="point-card__info">{point.info}</span>
                  </span>
                  <ArrowUpRight size={16} className="point-card__arrow" />
                </Link>
              </li>
            ))}
          </ul>
        </section>
      </main>

      <footer className="home-footer">
        <span>© 2026 Добрый Шкаф</span>
        <nav className="home-footer__links" aria-label="Контакты">
          <a href="/terms">Условия использования</a>
          <a href="mailto:hello@dobryshkaf.ru">hello@dobryshkaf.ru</a>
          {/* TODO: заменить на реальные ссылки сообществ проекта */}
          <a href="https://vk.com" className="link-blue" target="_blank" rel="noopener noreferrer">
            ВКонтакте <ArrowUpRight size={12} />
          </a>
          <a href="https://t.me" className="link-blue" target="_blank" rel="noopener noreferrer">
            Telegram <ArrowUpRight size={12} />
          </a>
        </nav>
      </footer>
    </div>
  );
}
