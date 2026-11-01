import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import App from './App';
import { AuthProvider } from './auth/AuthContext';
import * as api from './api';
import { ApiError } from './api/client';

jest.mock('./api');

const point = {
  id: 1,
  name: 'Тёплый круг',
  address: 'Москва, ул. Покровка, 27',
  city: 'Москва',
  workingHours: 'Пн–Сб 10:00–20:00',
  phone: '+7 (495) 123-45-67',
  description: 'Соседский пункт обмена вещами',
  photoUrl: null,
  status: 'ACTIVE',
  acceptedTypes: ['CLOTHES', 'SHOES', 'TOYS', 'BOOKS'],
};
const organizer = { id: 7, email: 'anna@example.com', name: 'Анна Смирнова', role: 'ORGANIZER' };

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
});

describe('главная страница', () => {
  test('гость видит вход, регистрацию и все блоки макета', () => {
    renderApp('/');

    expect(screen.getByRole('link', { name: 'Войти' })).toHaveAttribute('href', '/login');
    expect(screen.getByRole('link', { name: 'Зарегистрироваться' })).toHaveAttribute('href', '/register');
    expect(screen.getByRole('heading', { name: 'Добрый Шкаф. Делитесь добром.' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Как это работает' })).toBeInTheDocument();
    expect(screen.getByText('Найди вещь или пункт')).toBeInTheDocument();
    expect(screen.getByText('Свяжись через мессенджер')).toBeInTheDocument();
    expect(screen.getByText('Отметь обмен')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Популярные объявления' })).toBeInTheDocument();
    expect(screen.getAllByRole('link', { name: 'Подробнее' })).toHaveLength(4);
    expect(screen.getByRole('heading', { name: 'Ближайшие пункты обмена' })).toBeInTheDocument();
    expect(screen.getByText('© 2026 Добрый Шкаф')).toBeInTheDocument();
  });
});

describe('профиль организатора', () => {
  beforeEach(() => localStorage.setItem('accessToken', 'token'));

  test('показывает данные пункта в режиме просмотра', async () => {
    api.getProfile.mockResolvedValue(organizer);
    api.getOrganizerPoint.mockResolvedValue(point);

    renderApp('/organizer/profile');

    expect(await screen.findByRole('heading', { name: 'Профиль организатора' })).toBeInTheDocument();
    expect(screen.getByLabelText('Название пункта')).toHaveValue('Тёплый круг');
    expect(screen.getByLabelText('Название пункта')).toHaveAttribute('readonly');
    expect(screen.getByLabelText('Одежда')).toBeChecked();
    expect(screen.getByLabelText('Мебель')).not.toBeChecked();
    expect(screen.getByText('Анна · Тёплый круг')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Редактировать/ })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Посмотреть на карте/ })).toBeInTheDocument();
  });

  test('редактирование сохраняет пункт через API', async () => {
    api.getProfile.mockResolvedValue(organizer);
    api.getOrganizerPoint.mockResolvedValue(point);
    api.saveOrganizerPoint.mockImplementation(async (body) => ({ ...point, ...body }));

    renderApp('/organizer/profile');
    userEvent.click(await screen.findByRole('button', { name: /Редактировать/ }));

    const name = screen.getByLabelText('Название пункта');
    expect(name).not.toHaveAttribute('readonly');
    userEvent.clear(name);
    userEvent.type(name, 'Тёплый круг 2');
    userEvent.click(screen.getByLabelText('Мебель'));
    userEvent.click(screen.getByRole('button', { name: 'Сохранить' }));

    await waitFor(() => expect(api.saveOrganizerPoint).toHaveBeenCalledTimes(1));
    expect(api.saveOrganizerPoint).toHaveBeenCalledWith(
      expect.objectContaining({
        name: 'Тёплый круг 2',
        address: 'Москва, ул. Покровка, 27',
        acceptedTypes: expect.arrayContaining(['CLOTHES', 'FURNITURE']),
      })
    );
    expect(await screen.findByText('Данные пункта сохранены')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Редактировать/ })).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 2, name: 'Тёплый круг 2' })).toBeInTheDocument();
  });

  test('не отправляет форму с пустыми обязательными полями', async () => {
    api.getProfile.mockResolvedValue(organizer);
    api.getOrganizerPoint.mockResolvedValue(point);

    renderApp('/organizer/profile');
    userEvent.click(await screen.findByRole('button', { name: /Редактировать/ }));
    userEvent.clear(screen.getByLabelText('Контактный телефон'));
    userEvent.click(screen.getByRole('button', { name: 'Сохранить' }));

    expect(await screen.findByText('Укажите контактный телефон')).toBeInTheDocument();
    expect(api.saveOrganizerPoint).not.toHaveBeenCalled();
  });

  test('показывает ошибку сервера, например про неизвестный адрес', async () => {
    api.getProfile.mockResolvedValue(organizer);
    api.getOrganizerPoint.mockResolvedValue(point);
    const error = Object.assign(new Error('Не удалось определить адрес. Укажите город, улицу и дом'), {
      status: 400,
      fieldErrors: {},
    });
    api.saveOrganizerPoint.mockRejectedValue(error);

    renderApp('/organizer/profile');
    userEvent.click(await screen.findByRole('button', { name: /Редактировать/ }));
    userEvent.click(screen.getByRole('button', { name: 'Сохранить' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Не удалось определить адрес');
    expect(screen.getByRole('button', { name: 'Сохранить' })).toBeInTheDocument();
  });

  test('организатор без пункта сразу видит форму создания', async () => {
    api.getProfile.mockResolvedValue(organizer);
    api.getOrganizerPoint.mockRejectedValue(new ApiError(404, 'Пункт ещё не создан'));

    renderApp('/organizer/profile');

    expect(await screen.findByRole('button', { name: 'Сохранить' })).toBeInTheDocument();
    expect(screen.getByLabelText('Название пункта')).toHaveValue('');
    expect(screen.getByText(/Заполните данные пункта/)).toBeInTheDocument();
  });

  test('роль USER не попадает на страницу организатора', async () => {
    api.getProfile.mockResolvedValue({ ...organizer, role: 'USER' });

    renderApp('/organizer/profile');

    expect(await screen.findByRole('heading', { name: 'Добрый Шкаф. Делитесь добром.' })).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Профиль организатора' })).not.toBeInTheDocument();
  });

  test('боковое меню организатора содержит «Мой пункт», активен «Профиль»', async () => {
    api.getProfile.mockResolvedValue(organizer);
    api.getOrganizerPoint.mockResolvedValue(point);

    renderApp('/organizer/profile');

    const menu = await screen.findByRole('navigation', { name: 'Личный кабинет' });
    expect(within(menu).getByRole('link', { name: 'Мой пункт' })).toBeInTheDocument();
    expect(within(menu).getByRole('link', { name: 'Профиль' })).toHaveAttribute('aria-current', 'page');
  });
});

test('гость на странице организатора уходит на главную', () => {
  renderApp('/organizer/profile');
  expect(screen.getByRole('heading', { name: 'Добрый Шкаф. Делитесь добром.' })).toBeInTheDocument();
});
