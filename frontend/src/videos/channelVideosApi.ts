import type { Video } from '../data/home';
import { getEnv } from '../utils/Env';

export type VideoTab = 'published' | 'scheduled' | 'archived' | 'drafts';

const isDemoMode = (): boolean => import.meta.env.VITE_PROFILE_DEMO === 'true';

const DEMO_CHANNEL_VIDEOS: Video[] = [
  {
    id: 'demo-public-1',
    title: 'Vídeo público del canal',
    channel: 'protube_creator',
    views: '120 visualizaciones',
    published: 'hace 2 días',
    duration: '2:15',
    categoryId: 'all',
    thumbnail: `${import.meta.env.BASE_URL}dog.png`,
    viewCount: 120,
    createdAt: '2026-10-30T10:00:00Z',
  },
  {
    id: 'demo-public-2',
    title: 'Otro vídeo publicado',
    channel: 'protube_creator',
    views: '45 visualizaciones',
    published: 'hace 5 días',
    duration: '4:30',
    categoryId: 'all',
    thumbnail: `${import.meta.env.BASE_URL}dog.png`,
    viewCount: 45,
    createdAt: '2026-10-27T10:00:00Z',
  },
];

const requestVideos = async (url: string): Promise<Video[]> => {
  const response = await fetch(url, {
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      'X-Protube-Request': '1',
    },
  });

  const data = await response.json().catch(() => null);

  if (!response.ok) {
    throw new Error(data?.message || 'No se pudieron cargar los vídeos.');
  }
  return data as Video[];
};

export async function getPublicChannelVideos(userId: number): Promise<Video[]> {
  if (isDemoMode()) {
    return DEMO_CHANNEL_VIDEOS;
  }

  return requestVideos(`${getEnv().API_BASE_URL}/profiles/${userId}/videos`);
}

export async function getMyChannelVideos(userId: number, tab: Exclude<VideoTab, 'published'>): Promise<Video[]> {
  if (isDemoMode()) {
    return [];
  }

  const statusByTab: Record<Exclude<VideoTab, 'published'>, string> = {
    scheduled: 'SCHEDULED',
    archived: 'ARCHIVED',
    drafts: 'DRAFT',
  };
  const status = statusByTab[tab];
  const url = new URL(`${getEnv().API_BASE_URL}/profiles/me/videos`);
  url.searchParams.set('userId', String(userId));
  url.searchParams.set('status', status);

  return requestVideos(url.toString());
}
