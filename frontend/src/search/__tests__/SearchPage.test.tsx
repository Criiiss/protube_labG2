import { fireEvent, render, screen } from '@testing-library/react';
import fetchMock from 'jest-fetch-mock';
import type { Video } from '../../data/home';
import { SearchPage } from '../SearchPage';

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
const result = (overrides = {}) =>
  JSON.stringify({
    query: 'jazz',
    videos: [video('1')],
    videoTotal: 2,
    channels: [{ name: 'Blue Sessions', videoCount: 1, viewCount: 1500 }],
    channelTotal: 1,
    playlists: [],
    playlistTotal: 0,
    page: 0,
    hasMore: false,
    approximate: false,
    correctedQuery: null,
    ...overrides,
  });
const renderVideos = (videos: Video[]) => (
  <ul aria-label="lista de vídeos">
    {videos.map((item) => (
      <li key={item.id}>{item.title}</li>
    ))}
  </ul>
);
const show = (query: string) => render(<SearchPage params={new URLSearchParams(query)} renderVideos={renderVideos} />);

beforeEach(() => {
  fetchMock.resetMocks();
  window.location.hash = '';
});

describe('SearchPage', () => {
  it('shows channels and videos for the query and opens a channel', async () => {
    fetchMock.mockResponseOnce(result());
    show('q=jazz');
    expect(screen.getByRole('status', { name: 'Buscando' })).toBeInTheDocument();
    expect(await screen.findByText('Vídeo 1')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: /Resultados para «jazz»/ })).toBeInTheDocument();
    expect(fetchMock.mock.calls[0][0]).toBe('other/search?q=jazz&page=0&size=12');
    fireEvent.click(screen.getByRole('button', { name: /Blue Sessions/ }));
    expect(window.location.hash).toBe('#/search?type=video&channel=Blue+Sessions');
  });

  it('loads more results and appends them', async () => {
    fetchMock.mockResponseOnce(result({ hasMore: true }));
    fetchMock.mockResponseOnce(result({ videos: [video('2')], channels: [], page: 1 }));
    show('q=jazz');
    fireEvent.click(await screen.findByRole('button', { name: 'Cargar más vídeos' }));
    expect(await screen.findByText('Vídeo 2')).toBeInTheDocument();
    expect(screen.getByText('Vídeo 1')).toBeInTheDocument();
    expect(fetchMock.mock.calls[1][0]).toBe('other/search?q=jazz&page=1&size=12');
    expect(screen.queryByRole('button', { name: /Cargar más/ })).not.toBeInTheDocument();
  });

  it('shows a friendly message when nothing matches', async () => {
    fetchMock.mockResponseOnce(result({ videos: [], channels: [], videoTotal: 0 }));
    show('q=zzzz');
    expect(await screen.findByRole('heading', { name: 'Sin resultados para «zzzz»' })).toBeInTheDocument();
  });

  it('tells the user when the results are for a corrected spelling', async () => {
    fetchMock.mockResponseOnce(result({ approximate: true, correctedQuery: 'jazz' }));
    show('q=jaaz');
    expect(await screen.findByText(/No hay resultados para «jaaz»/)).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'jazz' }));
    expect(window.location.hash).toBe('#/search?q=jazz');
  });

  it('shows playlists and loads more of them', async () => {
    const playlist = (id: string, videoCount: number) => ({ id, title: `Lista ${id}`, owner: 'ana', videoCount });
    fetchMock.mockResponseOnce(
      result({ videos: [], channels: [], playlists: [playlist('5', 1)], playlistTotal: 2, hasMore: true })
    );
    fetchMock.mockResponseOnce(result({ videos: [], channels: [], playlists: [playlist('6', 3)], page: 1 }));
    show('q=lista&type=playlist');
    expect(await screen.findByRole('link', { name: /Lista 5 De ana · 1 vídeo$/ })).toHaveAttribute(
      'href',
      '#/playlist?id=5'
    );
    expect(screen.queryByRole('combobox', { name: 'Fecha de subida' })).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Cargar más playlists' }));
    expect(await screen.findByRole('link', { name: /Lista 6 De ana · 3 vídeos/ })).toBeInTheDocument();
  });

  it('changes filters through the URL', async () => {
    fetchMock.mockResponseOnce(result());
    show('q=jazz');
    await screen.findByText('Vídeo 1');
    fireEvent.change(screen.getByRole('combobox', { name: 'Duración' }), { target: { value: 'short' } });
    expect(window.location.hash).toBe('#/search?q=jazz&duration=short');
    fireEvent.change(screen.getByRole('combobox', { name: 'Ordenar por' }), { target: { value: 'views' } });
    expect(window.location.hash).toBe('#/search?q=jazz&sort=views');
  });

  it('only shows type and sort filters when searching channels', async () => {
    fetchMock.mockResponseOnce(result({ videos: [], hasMore: true }));
    show('q=blue&type=channel');
    expect(await screen.findByRole('button', { name: 'Cargar más canales' })).toBeInTheDocument();
    expect(screen.queryByRole('combobox', { name: 'Duración' })).not.toBeInTheDocument();
    expect(screen.getByRole('combobox', { name: 'Tipo' })).toHaveValue('channel');
  });

  it('shows a channel page and can go back to every channel', async () => {
    fetchMock.mockResponseOnce(result({ channels: [] }));
    show('type=video&channel=Blue+Sessions');
    expect(await screen.findByRole('heading', { name: /^Blue Sessions/ })).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Buscar en todos los canales' }));
    expect(window.location.hash).toBe('#/search?type=video');
  });

  it('shows the error and retries', async () => {
    fetchMock.mockResponseOnce(JSON.stringify({ message: 'El buscador no está disponible' }), { status: 503 });
    fetchMock.mockResponseOnce(result());
    show('');
    expect(await screen.findByText('El buscador no está disponible')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Reintentar' }));
    expect(await screen.findByText('Vídeo 1')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: /Explora todo/ })).toBeInTheDocument();
  });
});
