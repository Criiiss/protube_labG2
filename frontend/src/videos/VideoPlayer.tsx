import { useState } from 'react';
import type { Video } from '../data/home';
import { getEnv } from '../utils/Env';
import './VideoPlayer.css';
import type { SessionUser } from '../auth/api';
import { CommentsSection } from '../comments/CommentsSection';

interface VideoPlayerProps {
  video: Video;
  user: SessionUser | null;
}

function resolveMediaUrl(path?: string | null): string | undefined {
  if (!path) {
    return undefined;
  }
  if (path.startsWith('http://') || path.startsWith('https://') || path.startsWith('data:')) {
    return path;
  }
  if (path.startsWith('/media/')) {
    return `${getEnv().MEDIA_BASE_URL}${path.slice('/media'.length)}`;
  }
  return path;
}
const getInitials = (displayName: string | null | undefined, username: string): string => {
  const source = displayName?.trim() || username.trim();
  if (!source) return '?';
  return source
    .split(/[\s_]+/)
    .filter(Boolean)
    .map((word) => word[0])
    .join('')
    .slice(0, 2)
    .toUpperCase();
};

export function VideoPlayer({ video, user }: VideoPlayerProps) {
  const [videoError, setVideoError] = useState(false);

  const videoUrl = resolveMediaUrl(video.videoUrl);
  const thumbnailUrl = resolveMediaUrl(video.thumbnail);
  const avatarUrl = resolveMediaUrl(video.avatarUrl);
  const channelName = video.displayName?.trim() || video.channel;

  return (
    <section className="video-player-page" aria-labelledby="video-player-title">
      <div className="video-player-wrapper">
        {videoUrl && !videoError ? (
          <video
            className="video-player"
            src={videoUrl}
            poster={thumbnailUrl}
            controls
            preload="metadata"
            onError={() => setVideoError(true)}
          >
            Tu navegador no permite reproducir este vídeo.
          </video>
        ) : (
          <div className="video-player-unavailable" role="alert">
            <p>
              {videoUrl ? 'No se ha podido cargar este vídeo.' : 'Este vídeo todavía no tiene un archivo disponible.'}
            </p>
          </div>
        )}
      </div>

      <div className="video-player-info">
        <h1 id="video-player-title">{video.title}</h1>

        <div className="video-player-channel">
          {avatarUrl ? (
            <img
              className="video-player-avatar"
              src={avatarUrl}
              alt={`Foto de perfil de ${channelName}`}
              onError={(event) => {
                event.currentTarget.style.display = 'none';
              }}
            />
          ) : (
            <span className="video-player-avatar video-player-avatar-fallback" aria-hidden="true">
              {getInitials(video.displayName, video.channel)}
            </span>
          )}

          <div className="video-player-channel-text">
            <strong>{channelName}</strong>
            <span>@{video.channel}</span>
          </div>
        </div>

        <div className="video-player-meta">
          <span>{video.views}</span>
          {video.published && <span>{video.published}</span>}
        </div>

        <div className="video-player-description">
          <h2>Descripción</h2>
          <p>{video.description?.trim() || 'Este vídeo no tiene descripción.'}</p>
        </div>
        <CommentsSection videoId={video.id} user={user} />
      </div>
    </section>
  );
}
