import { useEffect, useState } from 'react';
import type { ReactNode } from 'react';
import type { Video } from '../data/home';
import { createPlaylist, deletePlaylist, getPlaylist, myPlaylists } from './playlistsApi';
import type { PlaylistDetail, PlaylistSummary } from './playlistsApi';

/** #/playlist?id=N — anyone can open a public playlist; the owner can also delete it. */
export function PlaylistPage({ id, renderVideos }: { id: string; renderVideos: (videos: Video[]) => ReactNode }) {
  const [playlist, setPlaylist] = useState<PlaylistDetail | null>(null);
  const [error, setError] = useState('');
  const [confirming, setConfirming] = useState(false);

  useEffect(() => {
    getPlaylist(id)
      .then(setPlaylist)
      .catch((reason: Error) => setError(reason.message));
  }, [id]);

  if (error)
    return (
      <div className="empty-state">
        <h1>No se puede abrir la playlist</h1>
        <p>{error}</p>
        <a href="#/playlists">Ver tus playlists →</a>
      </div>
    );
  if (!playlist) return <p role="status">Cargando playlist…</p>;
  return (
    <section aria-label="Playlist">
      <div className="page-intro">
        <div>
          <p className="eyebrow">
            PLAYLIST · {playlist.visibility === 'PRIVATE' ? 'PRIVADA' : 'PÚBLICA'} · DE {playlist.owner.toUpperCase()}
          </p>
          <h1>
            {playlist.title}
            <span>.</span>
          </h1>
          {playlist.description && <p className="dialog-subtitle">{playlist.description}</p>}
        </div>
        {playlist.mine &&
          (confirming ? (
            <div className="playlist-confirm">
              <span>¿Eliminar la playlist?</span>
              <button
                className="primary-button"
                onClick={() =>
                  void deletePlaylist(playlist.id)
                    .then(() => (window.location.hash = '/playlists'))
                    .catch((reason: Error) => setError(reason.message))
                }
              >
                Sí, eliminar
              </button>
              <button className="text-button" onClick={() => setConfirming(false)}>
                Cancelar
              </button>
            </div>
          ) : (
            <button className="text-button" onClick={() => setConfirming(true)}>
              Eliminar playlist
            </button>
          ))}
      </div>
      {playlist.videos.length ? (
        renderVideos(playlist.videos)
      ) : (
        <div className="empty-state">
          <h2>Esta playlist está vacía</h2>
          <p>Usa «Guardar en playlist» en el menú ⋮ de cualquier vídeo para añadirlo.</p>
        </div>
      )}
    </section>
  );
}

/** #/playlists — the signed-in user's playlists, plus a form to create one. */
export function MyPlaylistsPage() {
  const [playlists, setPlaylists] = useState<PlaylistSummary[] | null>(null);
  const [error, setError] = useState('');
  const [title, setTitle] = useState('');
  const [visibility, setVisibility] = useState<'PUBLIC' | 'PRIVATE'>('PUBLIC');

  useEffect(() => {
    myPlaylists()
      .then(setPlaylists)
      .catch((reason: Error) => setError(reason.message));
  }, []);

  async function create(event: React.FormEvent) {
    event.preventDefault();
    if (!title.trim()) return setError('Escribe un nombre para la playlist.');
    try {
      const created = await createPlaylist(title.trim(), visibility);
      setPlaylists((items) => [created, ...(items ?? [])]);
      setTitle('');
      setError('');
    } catch (reason) {
      setError((reason as Error).message);
    }
  }

  return (
    <section aria-label="Tus playlists">
      <div className="page-intro">
        <div>
          <p className="eyebrow">TU BIBLIOTECA</p>
          <h1>
            Tus playlists<span>.</span>
          </h1>
        </div>
      </div>
      <form className="playlist-create inline" onSubmit={(event) => void create(event)}>
        <label>
          Nueva playlist
          <input
            value={title}
            maxLength={150}
            placeholder="Nombre de la playlist"
            onChange={(event) => setTitle(event.target.value)}
          />
        </label>
        <label>
          Visibilidad
          <select value={visibility} onChange={(event) => setVisibility(event.target.value as 'PUBLIC' | 'PRIVATE')}>
            <option value="PUBLIC">Pública</option>
            <option value="PRIVATE">Privada</option>
          </select>
        </label>
        <button className="primary-button" type="submit">
          Crear
        </button>
      </form>
      {error && (
        <p className="form-message" role="alert">
          {error}
        </p>
      )}
      {playlists === null && !error && <p role="status">Cargando tus playlists…</p>}
      {playlists && playlists.length === 0 && (
        <div className="empty-state">
          <h2>Todavía no tienes playlists</h2>
          <p>Crea una aquí o desde el menú ⋮ de cualquier vídeo.</p>
        </div>
      )}
      {playlists && playlists.length > 0 && (
        <ul className="channel-results">
          {playlists.map((playlist) => (
            <li key={playlist.id}>
              <a className="channel-result" href={`#/playlist?id=${playlist.id}`}>
                <span className="channel-avatar" aria-hidden="true">
                  ≡
                </span>
                <span>
                  <strong>{playlist.title}</strong>
                  <small>
                    {playlist.videoCount} {playlist.videoCount === 1 ? 'vídeo' : 'vídeos'} ·{' '}
                    {playlist.visibility === 'PRIVATE' ? 'Privada' : 'Pública'}
                  </small>
                </span>
              </a>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
