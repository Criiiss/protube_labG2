import type { Video } from '../../data/home';
import { Icon } from './Icon';
import { useState } from 'react';

export type VideoAction = 'saved' | 'later' | 'liked' | 'hidden' | 'share' | 'subscriptions';
export function VideoCard({
  video,
  onSelect,
  onAction,
  selectedActions = [],
}: {
  video: Video;
  onSelect: (video: Video) => void;
  onAction?: (action: VideoAction, video: Video) => void;
  selectedActions?: string[];
}) {
  const [menu, setMenu] = useState(false);
  return (
    <article className="video-card">
      <button className="video-cover" onClick={() => onSelect(video)} aria-label={`Ver ${video.title}`}>
        <img src={video.thumbnail} alt="" loading="lazy" />
        <span className="cover-play">
          <Icon name="play" />
        </span>
        <span className="duration">{video.duration}</span>
      </button>
      <div className="video-info">
        <span className="channel-avatar">{video.channel[0]}</span>
        <div>
          <button className="video-title" onClick={() => onSelect(video)}>
            {video.title}
          </button>
          <a className="video-channel" href={`#/channel?name=${encodeURIComponent(video.channel)}`}>
            {video.channel}
          </a>
          <p>
            {video.views} ·{' '}
            {video.createdAt
              ? new Intl.RelativeTimeFormat('es', { numeric: 'auto' }).format(
                  -Math.max(0, Math.floor((Date.now() - Date.parse(video.createdAt)) / 86400000)),
                  'day'
                )
              : video.published}
          </p>
        </div>
        {onAction && (
          <div className="video-menu">
            <button
              className="more-button"
              aria-label={`Opciones de ${video.title}`}
              aria-expanded={menu}
              onClick={() => setMenu(!menu)}
            >
              ⋮
            </button>
            {menu && (
              <>
                <button className="menu-backdrop" aria-label="Cerrar opciones" onClick={() => setMenu(false)} />
                <div className="dropdown-panel">
                  {(
                    [
                      ['saved', 'Guardar en biblioteca'],
                      ['later', 'Ver más tarde'],
                      ['liked', 'Me gusta'],
                      ['share', 'Compartir'],
                      ['hidden', 'No me interesa'],
                      ['subscriptions', `Suscribirse a ${video.channel}`],
                    ] as [VideoAction, string][]
                  ).map(([action, label]) => (
                    <button
                      key={action}
                      onClick={() => {
                        onAction(action, video);
                        setMenu(false);
                      }}
                    >
                      {selectedActions.includes(action) ? '✓ ' : ''}
                      {label}
                    </button>
                  ))}
                </div>
              </>
            )}
          </div>
        )}
      </div>
    </article>
  );
}
