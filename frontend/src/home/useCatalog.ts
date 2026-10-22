import { useEffect, useState } from 'react';
import { api } from '../auth/api';
import type { Video } from '../data/home';
export function useCatalog() {
  const [videos, setVideos] = useState<Video[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  useEffect(() => {
    let alive = true;
    api<Video[]>('/catalog')
      .then((data) => {
        if (alive) setVideos(data);
      })
      .catch(() => {
        if (alive) setError('No se pudo cargar el catálogo. Puedes explorar el contenido de muestra.');
      })
      .finally(() => {
        if (alive) setLoading(false);
      });
    return () => {
      alive = false;
    };
  }, []);
  return { videos, loading, error };
}
