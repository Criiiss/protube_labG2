import { useEffect, useRef, useState } from 'react';
import type { Video } from '../data/home';
import { addToPlaylist, canAddToPlaylist, createPlaylist, myPlaylists, removeFromPlaylist } from './playlistsApi';
import type { PlaylistSummary } from './playlistsApi';

/** "Guardar en playlist": tick the playlists that should contain the video, or create a new one with it. */
export function PlaylistDialog({ video, onClose }: { video: Video; onClose: (message?: string) => void }) {
  const [playlists, setPlaylists] = useState<PlaylistSummary[] | null>(null);
  const [error, setError] = useState('');
  const [title, setTitle] = useState('');
  const [visibility, setVisibility] = useState<'PUBLIC' | 'PRIVATE'>('PUBLIC');
  const [busy, setBusy] = useState(false);
  const panel = useRef<HTMLDivElement>(null);
  const allowed = canAddToPlaylist(video);

  useEffect(() => {
    panel.current?.focus();
    if (!allowed) return;
    myPlaylists(video.id)
      .then(setPlaylists)
      .catch((reason: Error) => setError(reason.message));
  }, [video.id, allowed]);

  async function toggle(playlist: PlaylistSummary) {
    setBusy(true);
    setError('');
    try {
      await (playlist.hasVideo ? removeFromPlaylist(playlist.id, video.id) : addToPlaylist(playlist.id, video.id));
      setPlaylists((items) =>
        (items ?? []).map((item) =>
          item.id === playlist.id
            ? { ...item, hasVideo: !item.hasVideo, videoCount: item.videoCount + (item.hasVideo ? -1 : 1) }
            : item
        )
      );
    } catch (reason) {
      setError((reason as Error).message);
    } finally {
      setBusy(false);
    }
  }

  async function create(event: React.FormEvent) {
    event.preventDefault();
    if (!title.trim()) return setError('Escribe un nombre para la playlist.');
    setBusy(true);
    setError('');
    try {
      const created = await createPlaylist(title.trim(), visibility);
      await addToPlaylist(created.id, video.id);
      onClose(`Guardado en «${created.title}».`);
    } catch (reason) {
      setError((reason as Error).message);
      setBusy(false);
    }
  }

  return (
    <div className="dialog-backdrop" onClick={() => onClose()}>
      <div
        ref={panel}
        className="dialog-panel"
        role="dialog"
        aria-modal="true"
        aria-labelledby="playlist-dialog-title"
        tabIndex={-1}
        onClick={(event) => event.stopPropagation()}
        onKeyDown={(event) => event.key === 'Escape' && onClose()}
      >
        <div className="dialog-head">
          <h2 id="playlist-dialog-title">Guardar en playlist</h2>
          <button className="dialog-close" aria-label="Cerrar" onClick={() => onClose()}>
            ×
          </button>
        </div>
        <p className="dialog-subtitle">{video.title}</p>
        {!allowed ? (
          <p className="form-message">Este vídeo no se puede añadir a una playlist.</p>
        ) : (
          <>
            {error && (
              <p className="form-message" role="alert">
                {error}
              </p>
            )}
            {playlists === null && !error && <p role="status">Cargando tus playlists…</p>}
            {playlists && playlists.length === 0 && <p className="dialog-subtitle">Todavía no tienes playlists.</p>}
            {playlists && playlists.length > 0 && (
              <ul className="playlist-checklist">
                {playlists.map((playlist) => (
                  <li key={playlist.id}>
                    <label>
                      <input
                        type="checkbox"
                        checked={playlist.hasVideo}
                        disabled={busy}
                        onChange={() => void toggle(playlist)}
                      />
                      <span>{playlist.title}</span>
                      <small>{playlist.visibility === 'PRIVATE' ? 'Privada' : 'Pública'}</small>
                    </label>
                  </li>
                ))}
              </ul>
            )}
            <form className="playlist-create" onSubmit={(event) => void create(event)}>
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
                <select
                  value={visibility}
                  onChange={(event) => setVisibility(event.target.value as 'PUBLIC' | 'PRIVATE')}
                >
                  <option value="PUBLIC">Pública</option>
                  <option value="PRIVATE">Privada</option>
                </select>
              </label>
              <button className="primary-button" type="submit" disabled={busy}>
                Crear y guardar
              </button>
            </form>
          </>
        )}
      </div>
    </div>
  );
}
