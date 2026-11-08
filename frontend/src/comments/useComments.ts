import { useCallback, useEffect, useState } from 'react';
import { ApiError } from '../auth/api';
import {
  createComment,
  getVideoComments,
  deleteVideoComment,
  updateVideoComment,
  likeVideoComment,
  unlikeVideoComment,
} from './commentsApi';
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
    likedByCurrentUser: false,
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
          likedByCurrentUser: false,
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

  const editComment = async (commentId: string | number, content: string): Promise<Comment> => {
    setSubmitError('');
    const normalizedContent = content.trim();
    if (!normalizedContent) {
      throw new Error('El comentario no puede estar vacío.');
    }
    if (normalizedContent.length > 2000) {
      throw new Error('El comentario no puede superar los 2000 caracteres.');
    }
    const currentComment = comments.find((comment) => String(comment.id) === String(commentId));
    if (!currentComment) {
      throw new Error('El comentario no existe.');
    }
    try {
      let updatedComment: Comment;

      if (isDemoMode) {
        updatedComment = {
          ...currentComment,
          content: normalizedContent,
        };
      } else {
        updatedComment = await updateVideoComment(videoId, commentId, normalizedContent);
      }

      setComments((currentComments) =>
        currentComments.map((comment) => (String(comment.id) === String(commentId) ? updatedComment : comment))
      );

      return updatedComment;
    } catch (requestError) {
      const message = requestError instanceof ApiError ? requestError.message : 'No se pudo editar el comentario.';

      setSubmitError(message);
      throw new Error(message);
    }
  };
  const removeComment = async (commentId: string | number): Promise<void> => {
    setSubmitError('');
    if (isDemoMode) {
      setComments((currentComments) =>
        currentComments.filter(
          (comment) => String(comment.id) !== String(commentId) && String(comment.parentId) !== String(commentId)
        )
      );
      return;
    }
    await deleteVideoComment(videoId, commentId);
    setComments((currentComments) =>
      currentComments.filter(
        (comment) => String(comment.id) !== String(commentId) && String(comment.parentId) !== String(commentId)
      )
    );
  };

  const toggleLike = async (commentId: string | number, liked: boolean): Promise<Comment> => {
    setSubmitError('');
    if (isDemoMode) {
      const currentComment = comments.find((comment) => String(comment.id) === String(commentId));
      if (!currentComment) {
        throw new Error('El comentario no existe.');
      }
      const updatedComment: Comment = {
        ...currentComment,
        likedByCurrentUser: !liked,
        likeCount: Math.max(0, currentComment.likeCount + (liked ? -1 : 1)),
      };

      setComments((currentComments) =>
        currentComments.map((comment) => (String(comment.id) === String(commentId) ? updatedComment : comment))
      );

      return updatedComment;
    }
    const updatedComment = liked
      ? await unlikeVideoComment(videoId, commentId)
      : await likeVideoComment(videoId, commentId);
    setComments((currentComments) =>
      currentComments.map((comment) => (String(comment.id) === String(commentId) ? updatedComment : comment))
    );
    return updatedComment;
  };

  return {
    comments,
    loading,
    submitting,
    error,
    submitError,
    addComment,
    editComment,
    removeComment,
    toggleLike,
    reload: loadComments,
  };
}
