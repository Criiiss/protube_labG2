import { api } from '../auth/api';
import type { Video } from '../data/home';

export interface PlaylistSummary {
  id: number;
  title: string;
  visibility: 'PUBLIC' | 'PRIVATE';
  videoCount: number;
  hasVideo: boolean;
}
export interface PlaylistDetail {
  id: number;
  title: string;
  description: string | null;
  visibility: 'PUBLIC' | 'PRIVATE';
  owner: string;
  mine: boolean;
  videos: Video[];
}

/** Studio uploads have text ids and can't be added to playlists yet. */
export const canAddToPlaylist = (video: Video) => /^\d+$/.test(video.id);

export const myPlaylists = (videoId?: string) =>
  api<PlaylistSummary[]>(`/playlists/mine${videoId ? `?videoId=${encodeURIComponent(videoId)}` : ''}`);
export const createPlaylist = (title: string, visibility: 'PUBLIC' | 'PRIVATE', description = '') =>
  api<PlaylistSummary>('/playlists', { title, visibility, description });
export const getPlaylist = (id: string) => api<PlaylistDetail>(`/playlists/${encodeURIComponent(id)}`);
export const deletePlaylist = (id: number) => api<{ ok: boolean }>(`/playlists/${id}`, undefined, 'DELETE');
export const addToPlaylist = (id: number, videoId: string) =>
  api<{ ok: boolean }>(`/playlists/${id}/videos`, { videoId });
export const removeFromPlaylist = (id: number, videoId: string) =>
  api<{ ok: boolean }>(`/playlists/${id}/videos/${encodeURIComponent(videoId)}`, undefined, 'DELETE');
