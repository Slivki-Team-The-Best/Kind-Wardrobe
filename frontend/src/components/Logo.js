import { Shirt } from 'lucide-react';
import { Link } from 'react-router-dom';
import './Logo.css';

export default function Logo() {
  return (
    <Link to="/" className="logo" aria-label="Добрый Шкаф — на главную">
      <span className="logo__tile">
        <Shirt size={20} strokeWidth={1.75} />
      </span>
      <span className="logo__text">Добрый Шкаф</span>
    </Link>
  );
}
