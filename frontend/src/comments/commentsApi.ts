import { api } from '../auth/api';

export interface Comment {
  id: number;
  videoId: string | number;
  authorId: number;
  username: string;
  displayName: string | null;
  avatarUrl: string | null;
  content: string;
  parentId: string | number | null;
  likeCount: number;
  createdAt: string;
}

export interface CreateCommentRequest {
  content: string;
  parentId: string | number | null;
}

export function createComment(
  videoId: string,
  content: string,
  parentId: string | number | null = null
): Promise<Comment> {
  return api<Comment>(`/videos/${encodeURIComponent(videoId)}/comments`, { content, parentId }, 'POST');
}

export function getVideoComments(videoId: string): Promise<Comment[]> {
  return api<Comment[]>(`/videos/${encodeURIComponent(videoId)}/comments`);
}
