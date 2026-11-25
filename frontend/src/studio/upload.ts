import { ApiError } from '../auth/api';
import { sendBinary, studioItem } from './api';
import type { Item } from './api';

export async function fileChecksum(file: File) {
  const digest = await crypto.subtle.digest('SHA-256', await file.arrayBuffer());
  return Array.from(new Uint8Array(digest), (byte) => byte.toString(16).padStart(2, '0')).join('');
}

export function readDuration(file: File): Promise<number | undefined> {
  return new Promise((resolve) => {
    const video = document.createElement('video');
    const url = URL.createObjectURL(file);
    const complete = (duration?: number) => {
      clearTimeout(timer);
      video.onloadedmetadata = null;
      video.onerror = null;
      video.removeAttribute('src');
      URL.revokeObjectURL(url);
      resolve(duration);
    };
    const timer = window.setTimeout(() => complete(), 4000);
    video.preload = 'metadata';
    video.onloadedmetadata = () => complete(Number.isFinite(video.duration) ? video.duration : undefined);
    video.onerror = () => complete(); // MOV may need server-side probing.
    video.src = url;
  });
}

export async function transfer(
  file: File,
  initial: Item,
  chunkBytes: number,
  signal: AbortSignal,
  update: (item: Item) => void
) {
  let item = initial;
  let retries = 0;
  while (item.receivedBytes < file.size) {
    signal.throwIfAborted();
    const offset = item.receivedBytes;
    try {
      item = await sendBinary(
        `/videos/${item.id}/chunk?offset=${offset}`,
        file.slice(offset, offset + chunkBytes),
        signal
      );
      retries = 0;
    } catch (error) {
      if (!(error instanceof ApiError) || ![0, 409, 502, 503, 504].includes(error.status) || retries++ >= 2)
        throw error;
      await new Promise((resolve) => window.setTimeout(resolve, 600 * retries));
      signal.throwIfAborted();
      try {
        item = await studioItem(item.id);
      } catch (refreshError) {
        if (!(refreshError instanceof ApiError) || refreshError.status !== 0) throw refreshError;
      }
    }
    update(item);
  }
  signal.throwIfAborted();
}
