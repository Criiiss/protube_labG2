import { useEffect, useState } from 'react';
import type { ReactNode } from 'react';
import type { Video } from '../data/home';
import { filtersFromParams, search, searchHash } from './searchApi';
import type { ChannelHit, SearchFilters, SearchResult } from './searchApi';

const options: Record<'type' | 'date' | 'duration' | 'sort', [string, string][]> = {
  type: [
    ['all', 'Todo'],
    ['video', 'Vídeos'],
    ['channel', 'Canales'],
    ['playlist', 'Playlists'],
  ],
  date: [
    ['any', 'Cualquier fecha'],
    ['today', 'Hoy'],
    ['week', 'Esta semana'],
    ['month', 'Este mes'],
    ['year', 'Este año'],
  ],
  duration: [
    ['any', 'Cualquier duración'],
    ['short', 'Menos de 4 min'],
    ['medium', 'De 4 a 20 min'],
    ['long', 'Más de 20 min'],
  ],
  sort: [
    ['relevance', 'Relevancia'],
    ['date', 'Fecha de subida'],
    ['views', 'Visualizaciones'],
  ],
};
const labels = { type: 'Tipo', date: 'Fecha de subida', duration: 'Duración', sort: 'Ordenar por' };

function go(filters: SearchFilters) {
  window.location.hash = searchHash(filters);
}

export function SearchPage({
  params,
  renderVideos,
}: {
  params: URLSearchParams;
  renderVideos: (videos: Video[]) => ReactNode;
}) {
  const filters = filtersFromParams(params);
  const key = searchHash(filters);
  const [result, setResult] = useState<SearchResult | null>(null);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    let alive = true;
    setLoading(true);
    setError('');
    search(filtersFromParams(new URLSearchParams(key.split('?')[1])), page)
      .then((next) => {
        if (!alive) return;
        setResult((previous) =>
          page === 0 || !previous
            ? next
            : {
                ...next,
                videos: [...previous.videos, ...next.videos],
                channels: [...previous.channels, ...next.channels],
                playlists: [...previous.playlists, ...next.playlists],
              }
        );
      })
      .catch((reason: Error) => alive && setError(reason.message))
      .finally(() => alive && setLoading(false));
    return () => {
      alive = false;
    };
  }, [key, page, attempt]); // App remounts this page (key) when the filters change, so page starts at 0 again

  const nothing = result && !result.videos.length && !result.channels.length && !result.playlists.length;
  const title = filters.channel || (filters.q ? `Resultados para «${filters.q}»` : 'Explora todo');
  // Date and duration only make sense for videos
  const shown = ['channel', 'playlist'].includes(filters.type)
    ? (['type', 'sort'] as const)
    : (['type', 'date', 'duration', 'sort'] as const);
  const moreLabels: Record<string, string> = { channel: 'Cargar más canales', playlist: 'Cargar más playlists' };
  const more = moreLabels[filters.type] ?? 'Cargar más vídeos';

  return (
    <section className="search-page" aria-label="Resultados de búsqueda">
      <div className="page-intro">
        <div>
          <p className="eyebrow">{filters.channel ? 'CANAL' : 'BÚSQUEDA'}</p>
          <h1>
            {title}
            <span>.</span>
          </h1>
        </div>
        {filters.channel && (
          <button className="text-button" onClick={() => go({ ...filters, channel: '' })}>
            Buscar en todos los canales
          </button>
        )}
      </div>

      <div className="search-filters" role="group" aria-label="Filtros de búsqueda">
        {shown.map((name) => (
          <label key={name}>
            {labels[name]}
            <select value={filters[name]} onChange={(event) => go({ ...filters, [name]: event.target.value })}>
              {options[name].map(([value, text]) => (
                <option key={value} value={value}>
                  {text}
                </option>
              ))}
            </select>
          </label>
        ))}
      </div>

      {result?.correctedQuery && !nothing && (
        <p className="form-message" role="status">
          No hay resultados para «{filters.q}». Te mostramos los de «
          <button className="inline-link" onClick={() => go({ ...filters, q: result.correctedQuery ?? '' })}>
            {result.correctedQuery}
          </button>
          ».
        </p>
      )}

      {error ? (
        <div className="empty-state">
          <h2>No se pudo completar la búsqueda</h2>
          <p>{error}</p>
          <button className="primary-button" onClick={() => setAttempt(attempt + 1)}>
            Reintentar
          </button>
        </div>
      ) : !result && loading ? (
        <div className="video-grid" aria-label="Buscando" role="status">
          {Array.from({ length: 6 }, (_, index) => (
            <div key={index} className="video-skeleton">
              <div />
              <span />
              <span />
            </div>
          ))}
        </div>
      ) : nothing ? (
        <div className="empty-state">
          <h2>Sin resultados{filters.q ? ` para «${filters.q}»` : ''}</h2>
          <p>Revisa la ortografía, usa otras palabras o quita algún filtro.</p>
        </div>
      ) : result ? (
        <>
          {result.channels.length > 0 && (
            <section aria-label="Canales">
              <h2 className="search-section-title">Canales</h2>
              <ul className="channel-results">
                {result.channels.map((channel) => (
                  <ChannelCard
                    key={channel.name}
                    channel={channel}
                    onOpen={() => go({ ...filters, q: '', type: 'video', channel: channel.name })}
                  />
                ))}
              </ul>
            </section>
          )}
          {result.playlists.length > 0 && (
            <section aria-label="Playlists">
              <h2 className="search-section-title">Playlists</h2>
              <ul className="channel-results">
                {result.playlists.map((playlist) => (
                  <li key={playlist.id}>
                    <a className="channel-result" href={`#/playlist?id=${encodeURIComponent(playlist.id)}`}>
                      <span className="channel-avatar" aria-hidden="true">
                        ≡
                      </span>
                      <span>
                        <strong>{playlist.title}</strong>
                        <small>
                          De {playlist.owner} · {playlist.videoCount} {playlist.videoCount === 1 ? 'vídeo' : 'vídeos'}
                        </small>
                      </span>
                    </a>
                  </li>
                ))}
              </ul>
            </section>
          )}
          {result.videos.length > 0 && (
            <section aria-label="Vídeos">
              <h2 className="search-section-title">
                Vídeos <span>{result.videoTotal}</span>
              </h2>
              {renderVideos(result.videos)}
            </section>
          )}
          {result.hasMore && (
            <button className="load-more" disabled={loading} onClick={() => setPage(page + 1)}>
              {loading ? 'Cargando…' : more}
            </button>
          )}
        </>
      ) : null}
    </section>
  );
}

function ChannelCard({ channel, onOpen }: { channel: ChannelHit; onOpen: () => void }) {
  return (
    <li>
      <button className="channel-result" onClick={onOpen}>
        <span className="channel-avatar" aria-hidden="true">
          {channel.name[0]}
        </span>
        <span>
          <strong>{channel.name}</strong>
          <small>
            {channel.videoCount} {channel.videoCount === 1 ? 'vídeo' : 'vídeos'} ·{' '}
            {channel.viewCount.toLocaleString('es-ES')} visualizaciones
          </small>
        </span>
      </button>
    </li>
  );
}
