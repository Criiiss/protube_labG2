import type { Video } from '../../data/home';
import { Icon } from './Icon';

export function VideoCard({ video, onSelect }: { video: Video; onSelect: (video: Video) => void }) {
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
          <p>{video.channel}</p>
          <p>
            {video.views} · {video.published}
          </p>
        </div>
      </div>
    </article>
  );
}
