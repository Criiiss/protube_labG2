import { fireEvent, render, screen } from '@testing-library/react';
import { SettingsPage } from '../SettingsPage';

const profile = {
  userId: 7,
  username: 'prueba1_test1',
  displayName: 'Prueba1',
  biography: null,
  avatarUrl: null,
  bannerUrl: null,
  createdAt: '2026-11-27T10:00:00Z',
  subscriptionsVisibility: 'PUBLIC',
  likedVideosVisibility: 'PRIVATE',
};
const localAccount = { email: 'ana@test.com', emailVerified: true, provider: 'LOCAL' };

const renderPage = (account: object = localAccount) => {
  fetchMock.mockResponseOnce(JSON.stringify(profile));
  fetchMock.mockResponseOnce(JSON.stringify(account));
  const onLogout = jest.fn().mockResolvedValue(undefined);
  const onAccountDeleted = jest.fn();
  render(<SettingsPage userId={7} onLogout={onLogout} onAccountDeleted={onAccountDeleted} />);
  return { onLogout, onAccountDeleted };
};

const lastBody = () => JSON.parse(String(fetchMock.mock.calls[fetchMock.mock.calls.length - 1][1]?.body));

describe('SettingsPage', () => {
  beforeEach(() => fetchMock.resetMocks());

  it('shows the account and the current privacy settings', async () => {
    renderPage();
    expect(await screen.findByRole('heading', { name: 'Ajustes' })).toBeInTheDocument();
    expect(screen.getByText('@prueba1_test1')).toBeInTheDocument();
    expect(screen.getByText('ana@test.com')).toBeInTheDocument();
    expect(screen.getByText('Verificado')).toBeInTheDocument();
    expect(screen.getByText('Con email y contraseña')).toBeInTheDocument();
    expect(screen.getByLabelText('Mis suscripciones', { exact: false })).toHaveValue('PUBLIC');
    expect(screen.getByLabelText('Mis vídeos favoritos', { exact: false })).toHaveValue('PRIVATE');
    expect(screen.getByRole('link', { name: 'Editar perfil público' })).toHaveAttribute('href', '#/profile');
  });

  it('saves the privacy settings', async () => {
    renderPage();
    fireEvent.change(await screen.findByLabelText('Mis suscripciones', { exact: false }), {
      target: { value: 'PRIVATE' },
    });
    fetchMock.mockResponseOnce(JSON.stringify({ ...profile, subscriptionsVisibility: 'PRIVATE' }));
    fireEvent.click(screen.getByRole('button', { name: 'Guardar privacidad' }));
    expect(await screen.findByText('Privacidad actualizada.')).toBeInTheDocument();
    expect(fetchMock.mock.calls[2][0]).toContain('/profiles/me/privacy?userId=7');
    expect(lastBody()).toEqual({ subscriptionsVisibility: 'PRIVATE', likedVideosVisibility: 'PRIVATE' });
  });

  it('shows the error when privacy cannot be saved', async () => {
    renderPage();
    await screen.findByLabelText('Mis suscripciones', { exact: false });
    fetchMock.mockResponseOnce(JSON.stringify({ message: 'Error del servidor' }), { status: 500 });
    fireEvent.click(screen.getByRole('button', { name: 'Guardar privacidad' }));
    expect(await screen.findByText('Error del servidor')).toBeInTheDocument();
  });

  it('emails the password link to the account email', async () => {
    renderPage();
    expect(await screen.findByText(/Te enviaremos a ana@test.com/)).toBeInTheDocument();
    fetchMock.mockResponseOnce(JSON.stringify({ message: 'Te hemos enviado un enlace.' }));
    fireEvent.click(screen.getByRole('button', { name: 'Enviarme el enlace' }));
    expect(await screen.findByText('Te hemos enviado un enlace.')).toBeInTheDocument();
    expect(fetchMock.mock.calls[2][0]).toContain('/auth/account/password-email');
  });

  it('shows the error when the password email cannot be sent', async () => {
    renderPage();
    await screen.findByRole('button', { name: 'Enviarme el enlace' });
    fetchMock.mockResponseOnce(JSON.stringify({ message: 'Demasiados intentos' }), { status: 429 });
    fireEvent.click(screen.getByRole('button', { name: 'Enviarme el enlace' }));
    expect(await screen.findByText('Demasiados intentos')).toBeInTheDocument();
  });

  it('explains that Google accounts change their password in Google and confirm with the username', async () => {
    renderPage({ email: 'ana@gmail.com', emailVerified: true, provider: 'GOOGLE' });
    expect(await screen.findByText('Con tu cuenta de Google')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Enviarme el enlace' })).not.toBeInTheDocument();
    expect(screen.getByLabelText('Escribe tu nombre de usuario para confirmar')).toHaveAttribute('type', 'text');
  });

  it('warns when the email is not verified', async () => {
    renderPage({ ...localAccount, emailVerified: false });
    expect(await screen.findByText('Sin verificar')).toBeInTheDocument();
  });

  it('logs out', async () => {
    const { onLogout } = renderPage();
    fireEvent.click(await screen.findByRole('button', { name: 'Cerrar sesión' }));
    expect(onLogout).toHaveBeenCalled();
  });

  it('deletes the account only after confirming it', async () => {
    const { onAccountDeleted } = renderPage();
    const button = await screen.findByRole('button', { name: 'Eliminar mi cuenta' });
    expect(button).toBeDisabled();
    fireEvent.change(screen.getByLabelText('Tu contraseña'), { target: { value: 'ValidPass1' } });
    fireEvent.click(screen.getByLabelText('Entiendo que mi cuenta se eliminará para siempre'));
    fetchMock.mockResponseOnce(JSON.stringify({ message: 'Tu cuenta se ha eliminado.' }));
    fireEvent.click(button);
    await screen.findByRole('button', { name: 'Eliminando...' });
    await new Promise((resolve) => setTimeout(resolve, 0));
    expect(onAccountDeleted).toHaveBeenCalled();
    expect(fetchMock.mock.calls[2][1]?.method).toBe('DELETE');
    expect(lastBody()).toEqual({ confirmation: 'ValidPass1' });
  });

  it('keeps the account when the password is wrong', async () => {
    const { onAccountDeleted } = renderPage();
    fireEvent.change(await screen.findByLabelText('Tu contraseña'), { target: { value: 'Wrong1' } });
    fireEvent.click(screen.getByLabelText('Entiendo que mi cuenta se eliminará para siempre'));
    fetchMock.mockResponseOnce(JSON.stringify({ message: 'La contraseña no es correcta.' }), { status: 400 });
    fireEvent.click(screen.getByRole('button', { name: 'Eliminar mi cuenta' }));
    expect(await screen.findByText('La contraseña no es correcta.')).toBeInTheDocument();
    expect(onAccountDeleted).not.toHaveBeenCalled();
  });

  it('shows an error when the settings cannot be loaded', async () => {
    fetchMock.mockResponseOnce(JSON.stringify({ message: 'Sesión caducada' }), { status: 401 });
    fetchMock.mockResponseOnce(JSON.stringify(localAccount));
    render(<SettingsPage userId={7} onLogout={jest.fn()} onAccountDeleted={jest.fn()} />);
    expect(await screen.findByRole('alert')).toHaveTextContent('Sesión caducada');
  });
});
