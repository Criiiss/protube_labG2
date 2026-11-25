import type { Video } from '../data/home';
import { VideoCard } from '../components/home/VideoCard';

interface ChannelVideoGridProps {
  videos: Video[];
  emptyTitle?: string;
  emptyDescription?: string;
}

export function ChannelVideoGrid({
  videos,
  emptyTitle = 'Todavía no hay vídeos publicados',
  emptyDescription = 'Los vídeos públicos de este canal aparecerán aquí.',
}: ChannelVideoGridProps) {
  if (videos.length === 0) {
    return (
      <div className="profile-empty-content">
        <span className="profile-empty-icon" aria-hidden="true">
          ▶
        </span>
        <h3>{emptyTitle}</h3>
        <p>{emptyDescription}</p>
      </div>
    );
  }

  return (
    <div className="video-grid">
      {videos.map((video) => (
        <VideoCard
          key={video.id}
          video={video}
          onSelect={(selectedVideo) => {
            window.location.hash = `/watch?video=${encodeURIComponent(selectedVideo.id)}`;
          }}
        />
      ))}
    </div>
  );
}
