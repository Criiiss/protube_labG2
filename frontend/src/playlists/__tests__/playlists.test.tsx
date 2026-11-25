import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import fetchMock from 'jest-fetch-mock';
import type { Video } from '../../data/home';
import { PlaylistDialog } from '../PlaylistDialog';
import { MyPlaylistsPage, PlaylistPage } from '../PlaylistPages';

const video = (id: string): Video => ({
  id,
  title: `Vídeo ${id}`,
  channel: 'Blue Sessions',
  views: '1 visualizaciones',
  published: 'hoy',
  duration: '1:00',
  categoryId: 'all',
  thumbnail: '/dog.png',
});
const summary = (id: number, hasVideo = false) => ({
  id,
  title: `Lista ${id}`,
  visibility: id % 2 ? 'PUBLIC' : 'PRIVATE',
  videoCount: 2,
  hasVideo,
});
const renderVideos = (videos: Video[]) => (
  <ul>
    {videos.map((item) => (
      <li key={item.id}>{item.title}</li>
    ))}
  </ul>
);
const calls = () => fetchMock.mock.calls.map(([url, init]) => `${init?.method ?? 'GET'} ${url}`);

beforeEach(() => {
  fetchMock.resetMocks();
  window.location.hash = '';
});

describe('PlaylistDialog', () => {
  it('adds and removes the video by ticking playlists', async () => {
    fetchMock.mockResponses(
      JSON.stringify([summary(1, true), summary(2)]),
      JSON.stringify({ ok: true }),
      JSON.stringify({ ok: true })
    );
    render(<PlaylistDialog video={video('9')} onClose={jest.fn()} />);
    const first = await screen.findByRole('checkbox', { name: /Lista 1/ });
    expect(first).toBeChecked();
    fireEvent.click(first);
    await waitFor(() => expect(first).not.toBeChecked());
    fireEvent.click(screen.getByRole('checkbox', { name: /Lista 2/ }));
    await waitFor(() => expect(screen.getByRole('checkbox', { name: /Lista 2/ })).toBeChecked());
    expect(calls()).toEqual([
      'GET other/playlists/mine?videoId=9',
      'DELETE other/playlists/1/videos/9',
      'POST other/playlists/2/videos',
    ]);
  });

  it('creates a playlist with the video and reports errors', async () => {
    const onClose = jest.fn();
    fetchMock.mockResponses(
      JSON.stringify([]),
      [JSON.stringify({ message: 'El título debe tener entre 1 y 150 caracteres.' }), { status: 400 }],
      JSON.stringify(summary(3)),
      JSON.stringify({ ok: true })
    );
    render(<PlaylistDialog video={video('9')} onClose={onClose} />);
    expect(await screen.findByText('Todavía no tienes playlists.')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Crear y guardar' }));
    expect(screen.getByRole('alert')).toHaveTextContent('Escribe un nombre');
    fireEvent.change(screen.getByRole('textbox', { name: 'Nueva playlist' }), { target: { value: 'x' } });
    fireEvent.click(screen.getByRole('button', { name: 'Crear y guardar' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('El título debe tener');
    fireEvent.change(screen.getByRole('combobox', { name: 'Visibilidad' }), { target: { value: 'PRIVATE' } });
    fireEvent.click(screen.getByRole('button', { name: 'Crear y guardar' }));
    await waitFor(() => expect(onClose).toHaveBeenCalledWith('Guardado en «Lista 3».'));
    expect(JSON.parse(fetchMock.mock.calls[2][1]?.body as string)).toEqual({
      title: 'x',
      visibility: 'PRIVATE',
      description: '',
    });
  });

  it('explains that Studio uploads cannot be added and closes with Escape or the backdrop', () => {
    const onClose = jest.fn();
    const { container } = render(<PlaylistDialog video={video('abc-studio')} onClose={onClose} />);
    expect(screen.getByText(/Studio todavía no se pueden añadir/)).toBeInTheDocument();
    expect(fetchMock).not.toHaveBeenCalled();
    fireEvent.keyDown(screen.getByRole('dialog'), { key: 'Escape' });
    fireEvent.click(container.querySelector('.dialog-backdrop') as Element);
    fireEvent.click(screen.getByRole('button', { name: 'Cerrar' }));
    expect(onClose).toHaveBeenCalledTimes(3);
  });

  it('shows why the playlists could not be loaded', async () => {
    fetchMock.mockResponseOnce(JSON.stringify({ message: 'Inicia sesión para continuar.' }), { status: 401 });
    render(<PlaylistDialog video={video('9')} onClose={jest.fn()} />);
    expect(await screen.findByRole('alert')).toHaveTextContent('Inicia sesión');
  });
});

describe('PlaylistPage', () => {
  const detail = (overrides = {}) =>
    JSON.stringify({
      id: 4,
      title: 'Viaje',
      description: 'Para el tren',
      visibility: 'PUBLIC',
      owner: 'ana',
      mine: true,
      videos: [video('1')],
      ...overrides,
    });

  it('shows the playlist and lets the owner delete it', async () => {
    fetchMock.mockResponses(detail(), JSON.stringify({ ok: true }));
    render(<PlaylistPage id="4" renderVideos={renderVideos} />);
    expect(await screen.findByText('Vídeo 1')).toBeInTheDocument();
    expect(screen.getByText('Para el tren')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Eliminar playlist' }));
    fireEvent.click(screen.getByRole('button', { name: 'Cancelar' }));
    fireEvent.click(screen.getByRole('button', { name: 'Eliminar playlist' }));
    fireEvent.click(screen.getByRole('button', { name: 'Sí, eliminar' }));
    await waitFor(() => expect(window.location.hash).toBe('#/playlists'));
    expect(calls()[1]).toBe('DELETE other/playlists/4');
  });

  it('shows empty playlists of others without delete button, and errors', async () => {
    fetchMock.mockResponseOnce(detail({ mine: false, videos: [], visibility: 'PRIVATE', description: null }));
    const { unmount } = render(<PlaylistPage id="4" renderVideos={renderVideos} />);
    expect(await screen.findByText('Esta playlist está vacía')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Eliminar playlist' })).not.toBeInTheDocument();
    unmount();

    fetchMock.mockResponseOnce(JSON.stringify({ message: 'Esta playlist no existe o es privada.' }), { status: 404 });
    render(<PlaylistPage id="99" renderVideos={renderVideos} />);
    expect(await screen.findByText('Esta playlist no existe o es privada.')).toBeInTheDocument();
  });
});

describe('MyPlaylistsPage', () => {
  it('lists my playlists and creates new ones', async () => {
    fetchMock.mockResponses(JSON.stringify([summary(1)]), JSON.stringify({ ...summary(2), videoCount: 0 }));
    render(<MyPlaylistsPage />);
    expect(await screen.findByRole('link', { name: /Lista 1 2 vídeos · Pública/ })).toHaveAttribute(
      'href',
      '#/playlist?id=1'
    );
    fireEvent.click(screen.getByRole('button', { name: 'Crear' }));
    expect(screen.getByRole('alert')).toHaveTextContent('Escribe un nombre');
    fireEvent.change(screen.getByRole('textbox', { name: 'Nueva playlist' }), { target: { value: 'Lista 2' } });
    fireEvent.change(screen.getByRole('combobox', { name: 'Visibilidad' }), { target: { value: 'PRIVATE' } });
    fireEvent.click(screen.getByRole('button', { name: 'Crear' }));
    expect(await screen.findByRole('link', { name: /Lista 2 0 vídeos · Privada/ })).toBeInTheDocument();
    expect(screen.getByRole('textbox', { name: 'Nueva playlist' })).toHaveValue('');
  });

  it('shows the empty state and creation errors', async () => {
    fetchMock.mockResponses(JSON.stringify([]), [JSON.stringify({ message: 'Error del servidor' }), { status: 500 }]);
    render(<MyPlaylistsPage />);
    expect(await screen.findByText('Todavía no tienes playlists')).toBeInTheDocument();
    fireEvent.change(screen.getByRole('textbox', { name: 'Nueva playlist' }), { target: { value: 'x' } });
    fireEvent.click(screen.getByRole('button', { name: 'Crear' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Error del servidor');
  });

  it('shows why the list could not be loaded', async () => {
    fetchMock.mockResponseOnce(JSON.stringify({ message: 'Inicia sesión para continuar.' }), { status: 401 });
    render(<MyPlaylistsPage />);
    expect(await screen.findByRole('alert')).toHaveTextContent('Inicia sesión');
  });
});
