import { api } from '../auth/api';

export interface Comment {
  id: number;
  videoId: string;
  authorId: number;
  username: string;
  displayName: string | null;
  avatarUrl: string | null;
  content: string;
  parentId: number | null;
  likeCount: number;
  createdAt: string;
}

export function createComment(videoId: string, content: string): Promise<Comment> {
  return api<Comment>(`/videos/${encodeURIComponent(videoId)}/comments`, { content }, 'POST');
}

export function getVideoComments(videoId: string): Promise<Comment[]> {
  return api<Comment[]>(`/videos/${encodeURIComponent(videoId)}/comments`);
}
