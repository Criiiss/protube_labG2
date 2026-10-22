import { useState } from 'react';
import type { Video } from '../../data/home';
import { VideoCard } from './VideoCard';
import type { VideoAction } from './VideoCard';
import { Icon } from './Icon';

export function VideoCarousel({
  videos,
  onSelect,
  onAction,
}: {
  videos: Video[];
  onSelect: (video: Video) => void;
  onAction?: (action: VideoAction, video: Video) => void;
}) {
  const [position, setPosition] = useState(0);
  const current = position % videos.length;
  return (
    <div className="carousel" role="region" aria-roledescription="carrusel" aria-label="Vídeos destacados">
      <button
        className="carousel-arrow previous"
        aria-label="Vídeo destacado anterior"
        onClick={() => setPosition((current + videos.length - 1) % videos.length)}
      >
        <Icon name="chevron" />
      </button>
      <div className="carousel-stage">
        {[-1, 0, 1].map((offset) => {
          const video = videos[(current + offset + videos.length) % videos.length];
          return (
            <div
              key={`${offset}-${video.id}`}
              className={`carousel-slide ${offset === 0 ? 'current' : 'side'}`}
              aria-hidden={offset !== 0}
              inert={offset !== 0}
            >
              <VideoCard video={video} onSelect={onSelect} onAction={offset === 0 ? onAction : undefined} />
            </div>
          );
        })}
      </div>
      <button
        className="carousel-arrow next"
        aria-label="Siguiente vídeo destacado"
        onClick={() => setPosition((current + 1) % videos.length)}
      >
        <Icon name="chevron" />
      </button>
      <div className="carousel-pagination">
        {videos.map((video, index) => (
          <button
            key={video.id}
            aria-label={`Mostrar destacado ${index + 1}`}
            aria-pressed={index === current}
            className={index === current ? 'selected' : ''}
            onClick={() => setPosition(index)}
          />
        ))}
      </div>
      <span className="sr-only" aria-live="polite">
        Destacado {current + 1} de {videos.length}: {videos[current].title}
      </span>
    </div>
  );
}
