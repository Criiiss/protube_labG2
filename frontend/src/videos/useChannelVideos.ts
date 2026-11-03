import { useEffect, useState } from 'react';
import type { Video } from '../data/home';
import { getMyChannelVideos, getPublicChannelVideos, type VideoTab } from './channelVideosApi';

interface UseChannelVideosResult {
  videos: Video[];
  loading: boolean;
  error: string | null;
}

export function useChannelVideos(userId: number, tab: VideoTab = 'published', isOwner = false): UseChannelVideosResult {
  const [videos, setVideos] = useState<Video[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    setVideos([]);
    setLoading(true);
    setError(null);

    const request = isOwner && tab !== 'published' ? getMyChannelVideos(userId, tab) : getPublicChannelVideos(userId);
    request
      .then((loadedVideos) => {
        if (active) {
          setVideos(loadedVideos);
        }
      })
      .catch((requestError: unknown) => {
        if (active) {
          setError(requestError instanceof Error ? requestError.message : 'No se pudieron cargar los vídeos.');
        }
      })
      .finally(() => {
        if (active) {
          setLoading(false);
        }
      });

    return () => {
      active = false;
    };
  }, [userId, tab, isOwner]);

  return {
    videos,
    loading,
    error,
  };
}
