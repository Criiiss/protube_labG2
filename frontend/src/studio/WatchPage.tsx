import { useEffect, useState } from 'react';
import { studioPlayback, studioUrl } from './api';
import type { Playback } from './api';
import './studio.css';

export function WatchPage({ id }: { id: string }) {
  const [video, setVideo] = useState<Playback>();
  const [quality, setQuality] = useState('720');
  const [message, setMessage] = useState('');
  useEffect(() => {
    let active = true;
    studioPlayback(id)
      .then((data) => {
        if (active) setVideo(data);
      })
      .catch((error: Error) => {
        if (active) setMessage(error.message);
      });
    return () => {
      active = false;
    };
  }, [id]);
  if (message)
    return (
      <p className="empty-state" role="alert">
        {message}
      </p>
    );
  if (!video) return <p role="status">Cargando vídeo…</p>;
  return (
    <section className="watch-page">
      <video
        controls
        playsInline
        preload="metadata"
        src={studioUrl(`/play/${id}/${quality}.mp4`)}
        poster={studioUrl(`/play/${id}/thumbnail`)}
        aria-label={video.title}
      />
      <label className="form-field">
        Calidad
        <select value={quality} onChange={(e) => setQuality(e.target.value)}>
          <option value="360">360p</option>
          <option value="720">720p</option>
          <option value="1080">1080p</option>
        </select>
      </label>
      <h1>{video.title}</h1>
      <p>{video.channel}</p>
      {video.ageRestricted && <span className="form-hint">Mayores de 18 años</span>}
      <p className="watch-description">{video.description}</p>
      {!!video.tags.length && <p>{video.tags.map((tag) => `#${tag}`).join(' · ')}</p>}
      {!video.commentsAllowed && <p className="form-hint">Los comentarios están desactivados para este vídeo.</p>}
    </section>
  );
}
