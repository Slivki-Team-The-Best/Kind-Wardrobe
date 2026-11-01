import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import App from '../App';
import { AuthProvider } from '../auth/AuthContext';
import * as api from '../api';
import { ApiError } from '../api/client';

jest.mock('../api');

const anna = { id: 1, email: 'anna@example.com', name: 'Анна Смирнова', city: 'Москва', role: 'USER' };
const stats = { savedKg: 12.4, exchangesCount: 18, activeListingsCount: 1, listingsCount: 3 };
const listing = {
  id: 5,
  title: 'Куртка-рубашка, размер M',
  itemType: 'CLOTHES',
  exchangeFormat: 'FREE',
  city: 'Москва',
  status: 'ACTIVE',
  coverUrl: null,
  publishedAt: new Date().toISOString(),
  createdAt: new Date().toISOString(),
};

function renderApp(route) {
  return render(
    <MemoryRouter initialEntries={[route]}>
      <AuthProvider>
        <App />
      </AuthProvider>
    </MemoryRouter>
  );
}

beforeEach(() => {
  localStorage.clear();
  jest.resetAllMocks();
  api.getMyListings.mockResolvedValue([listing]);
  api.getMyStats.mockResolvedValue(stats);
});

test('вход открывает личный кабинет с активными объявлениями и эко-вкладом', async () => {
  api.login.mockResolvedValue({ accessToken: 'token' });
  api.getProfile.mockResolvedValue(anna);

  renderApp('/login');
  userEvent.type(screen.getByLabelText('Email'), 'anna@example.com');
  userEvent.type(screen.getByLabelText('Пароль'), 'secret-123');
  userEvent.click(screen.getByRole('button', { name: 'Войти' }));

  expect(await screen.findByRole('heading', { name: /, Анна!$/ })).toBeInTheDocument();
  expect(api.login).toHaveBeenCalledWith('anna@example.com', 'secret-123');
  expect(await screen.findByText('Куртка-рубашка, размер M')).toBeInTheDocument();
  expect(screen.getByText('12,4 кг')).toBeInTheDocument();
  expect(screen.getByRole('link', { name: 'Все объявления · 3 →' })).toHaveAttribute('href', '/my-listings');
});

test('неверный пароль показывает понятную ошибку', async () => {
  api.login.mockRejectedValue(new ApiError(401, 'Неверные учётные данные'));

  renderApp('/login');
  userEvent.type(screen.getByLabelText('Email'), 'anna@example.com');
  userEvent.type(screen.getByLabelText('Пароль'), 'wrong');
  userEvent.click(screen.getByRole('button', { name: 'Войти' }));

  expect(await screen.findByText('Неверный email или пароль')).toBeInTheDocument();
  expect(api.getProfile).not.toHaveBeenCalled();
});

test('пустая регистрация подсвечивает поля и не уходит на сервер', () => {
  renderApp('/register');
  userEvent.click(screen.getByRole('button', { name: 'Зарегистрироваться' }));

  expect(screen.getByText('Введите имя')).toBeInTheDocument();
  expect(screen.getAllByText('Выберите город')).toHaveLength(2);
  expect(screen.getByText('Нужно согласие с условиями использования')).toBeInTheDocument();
  expect(api.register).not.toHaveBeenCalled();
});

test('ошибки сервера при регистрации показываются у полей', async () => {
  api.register.mockRejectedValue(
    new ApiError(409, 'Email уже зарегистрирован', { email: 'Email уже зарегистрирован' })
  );

  renderApp('/register');
  userEvent.type(screen.getByLabelText('Имя *'), 'Анна');
  userEvent.selectOptions(screen.getByLabelText('Город *'), 'Москва');
  userEvent.type(screen.getByLabelText('Email *'), 'anna@example.com');
  userEvent.type(screen.getByLabelText('Телефон *'), '9165551234');
  userEvent.type(screen.getByLabelText('Пароль *'), 'secret-123');
  userEvent.type(screen.getByLabelText('Подтверждение пароля *'), 'secret-123');
  userEvent.click(screen.getByLabelText('Согласен с условиями использования'));
  userEvent.click(screen.getByRole('button', { name: 'Зарегистрироваться' }));

  expect(await screen.findByText('Email уже зарегистрирован')).toBeInTheDocument();
  expect(api.register).toHaveBeenCalledWith(
    expect.objectContaining({ role: 'USER', phone: '+7 (916) 555-12-34', city: 'Москва' })
  );
});

test('в меню организатора нет «Мои объявления», у пользователя есть', async () => {
  localStorage.setItem('accessToken', 'token');
  api.getProfile.mockResolvedValue({ ...anna, role: 'ORGANIZER' });
  api.getOrganizerPoint.mockResolvedValue(null);

  const { unmount } = renderApp('/cabinet');
  const organizerMenu = await screen.findByRole('navigation', { name: 'Личный кабинет' });
  expect(within(organizerMenu).queryByRole('link', { name: 'Мои объявления' })).not.toBeInTheDocument();
  unmount();

  api.getProfile.mockResolvedValue(anna);
  renderApp('/cabinet');
  const userMenu = await screen.findByRole('navigation', { name: 'Личный кабинет' });
  expect(within(userMenu).getByRole('link', { name: 'Мои объявления' })).toHaveAttribute('href', '/my-listings');
});

test('в шапке организатора без имени показывается название пункта', async () => {
  localStorage.setItem('accessToken', 'token');
  api.getProfile.mockResolvedValue({ id: 9, email: 'org@example.com', name: null, role: 'ORGANIZER' });
  api.getOrganizerPoint.mockResolvedValue({ id: 1, name: 'Детский добрый шкаф', acceptedTypes: [], status: 'ACTIVE' });

  renderApp('/cabinet');

  expect(await screen.findByText('Детский добрый шкаф', { selector: '.user-menu__name' })).toBeInTheDocument();
  expect(screen.queryByText(/org@example\.com ·/)).not.toBeInTheDocument();
});

test('организатор не попадает на страницы объявлений', async () => {
  localStorage.setItem('accessToken', 'token');
  api.getProfile.mockResolvedValue({ ...anna, role: 'ORGANIZER' });
  api.getOrganizerPoint.mockResolvedValue(null);

  renderApp('/listings/new');

  await waitFor(() =>
    expect(screen.getByRole('heading', { name: 'Добрый Шкаф. Делитесь добром.' })).toBeInTheDocument()
  );
  expect(screen.queryByRole('heading', { name: 'Новое объявление' })).not.toBeInTheDocument();
});
