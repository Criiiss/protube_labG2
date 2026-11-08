import { useCallback, useEffect, useState } from 'react';
import { ApiError } from '../auth/api';
import { createComment, getVideoComments } from './commentsApi';
import type { Comment } from './commentsApi';

const isDemoMode = import.meta.env.VITE_PROFILE_DEMO === 'true';

const demoComments: Comment[] = [
  {
    id: 1,
    videoId: 'demo-1',
    authorId: 2,
    username: 'otro_usuario',
    displayName: 'Otro usuario',
    avatarUrl: null,
    content: 'Este es un comentario de prueba para comprobar cómo aparece en el vídeo.',
    parentId: null,
    likeCount: 0,
    createdAt: new Date().toISOString(),
  },
];

export function useComments(videoId: string) {
  const [comments, setComments] = useState<Comment[]>(() =>
    isDemoMode ? demoComments.filter((comment) => String(comment.videoId) === String(videoId)) : []
  );

  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [submitError, setSubmitError] = useState('');

  const loadComments = useCallback(async () => {
    if (isDemoMode) {
      setComments(demoComments.filter((comment) => String(comment.videoId) === String(videoId)));
      setLoading(false);
      setError('');
      return;
    }
    setLoading(true);
    setError('');
    try {
      setComments(await getVideoComments(videoId));
    } catch (requestError) {
      setError(requestError instanceof ApiError ? requestError.message : 'No se pudieron cargar los comentarios.');
    } finally {
      setLoading(false);
    }
  }, [videoId]);
  useEffect(() => {
    void loadComments();
  }, [loadComments]);

  const addComment = async (content: string, parentId: string | number | null = null) => {
    setSubmitting(true);
    setSubmitError('');
    try {
      if (isDemoMode) {
        const demoComment: Comment = {
          id: Date.now(),
          videoId,
          authorId: 1,
          username: 'usuario_demo',
          displayName: 'Usuario Demo',
          avatarUrl: null,
          content,
          parentId,
          likeCount: 0,
          createdAt: new Date().toISOString(),
        };
        setComments((currentComments) => [...currentComments, demoComment]);
        return demoComment;
      }

      const createdComment = await createComment(videoId, content, parentId);
      setComments((currentComments) => [...currentComments, createdComment]);
      return createdComment;
    } catch (requestError) {
      const message = requestError instanceof ApiError ? requestError.message : 'No se pudo publicar el comentario.';

      setSubmitError(message);
      throw requestError;
    } finally {
      setSubmitting(false);
    }
  };

  return {
    comments,
    loading,
    submitting,
    error,
    submitError,
    addComment,
    reload: loadComments,
  };
}
