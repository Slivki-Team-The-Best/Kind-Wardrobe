import Logo from './Logo';
import './Header.css';

export default function AuthHeader() {
  return (
    <header className="header">
      <Logo />
      <span className="auth-header__tagline">У вещей есть продолжение</span>
    </header>
  );
}
