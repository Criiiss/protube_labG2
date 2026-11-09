import { api, ApiError } from '../auth/api';
import { getEnv } from '../utils/Env';

export type Visibility = 'PUBLIC' | 'PRIVATE' | 'UNLISTED';
export type Status = 'UPLOADING' | 'QUEUED' | 'PROCESSING' | 'READY' | 'ERROR' | 'CANCELLED';
export interface Metadata {
  title: string;
  description: string;
  tags: string[];
  category: string;
  language: string;
  ageRestricted: boolean;
  commentsAllowed: boolean;
  visibility: Visibility;
  publishAt: string | null;
  draft: boolean;
  thumbnail: string;
}
export interface Item {
  id: string;
  filename: string;
  checksum: string;
  totalBytes: number;
  receivedBytes: number;
  status: Status;
  duration: number;
  failure: string | null;
  finishedAt: string | null;
  metadata: Metadata;
  thumbnails: string[];
}
export interface Limits {
  maxBytes: number;
  maxSeconds: number;
  chunkBytes: number;
  imageBytes: number;
}
export interface Playback {
  id: string;
  title: string;
  description: string;
  channel: string;
  tags: string[];
  category: string;
  language: string;
  ageRestricted: boolean;
  commentsAllowed: boolean;
}
export const studioUrl = (path: string) => `${getEnv().API_BASE_URL}/studio${path}`;
export const studioList = () => api<Item[]>('/studio/videos');
export const studioItem = (id: string) => api<Item>(`/studio/videos/${id}`);
export const studioLimits = () => api<Limits>('/studio/limits');
export const studioStart = (filename: string, bytes: number, checksum: string, metadata: Metadata) =>
  api<Item>('/studio/videos', { filename, bytes, checksum, metadata });
export const studioEdit = (id: string, metadata: Metadata) => api<Item>(`/studio/videos/${id}`, metadata, 'PATCH');
export const studioFinish = (id: string) => api<Item>(`/studio/videos/${id}/finish`, {});
export const studioCancel = (id: string) => api<void>(`/studio/videos/${id}`, undefined, 'DELETE');
export const studioPlayback = (id: string) => api<Playback>(`/studio/play/${id}`);

export async function sendBinary(path: string, body: Blob, signal?: AbortSignal): Promise<Item> {
  let response: Response;
  try {
    response = await fetch(studioUrl(path), {
      method: 'PUT',
      body,
      signal,
      credentials: 'include',
      headers: { 'Content-Type': 'application/octet-stream', 'X-Protube-Request': '1' },
    });
  } catch (error) {
    if (signal?.aborted) throw error;
    throw new ApiError('Se perdió la conexión. Puedes reanudar la subida.', 0);
  }
  const data = await response.json().catch(() => ({}));
  if (!response.ok) throw new ApiError(data.message || 'No se pudo enviar el archivo.', response.status);
  return data as Item;
}

export const statusText: Record<Status, string> = {
  UPLOADING: 'Subiendo',
  QUEUED: 'En cola',
  PROCESSING: 'Procesando',
  READY: 'Listo',
  ERROR: 'Error',
  CANCELLED: 'Cancelado',
};
