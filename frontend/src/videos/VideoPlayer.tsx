import type { Video } from '../data/home';
import { getEnv } from '../utils/Env';
import './VideoPlayer.css';

interface VideoPlayerProps {
  video: Video;
}

function resolveMediaUrl(path?: string): string | undefined {
  if (!path) {
    return undefined;
  }
  if (path.startsWith('http://') || path.startsWith('https://')) {
    return path;
  }
  if (path.startsWith('/media/')) {
    return `${getEnv().MEDIA_BASE_URL}${path.slice('/media'.length)}`;
  }
  return path;
}
export function VideoPlayer({ video }: VideoPlayerProps) {
  const videoUrl = resolveMediaUrl(video.videoUrl);
  const thumbnailUrl = resolveMediaUrl(video.thumbnail);

  return (
    <section className="video-player-page" aria-labelledby="video-player-title">
      <div className="video-player-wrapper">
        {videoUrl ? (
          <video className="video-player" src={videoUrl} poster={thumbnailUrl} controls preload="metadata">
            Tu navegador no permite reproducir este vídeo.
          </video>
        ) : (
          <div className="video-player-unavailable">
            <p>Este vídeo todavía no tiene un archivo disponible.</p>
          </div>
        )}
      </div>

      <div className="video-player-info">
        <h1 id="video-player-title">{video.title}</h1>
        <p>
          {video.channel} · {video.views}
        </p>
      </div>
    </section>
  );
}
