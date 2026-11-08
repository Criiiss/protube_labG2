import { useState } from 'react';
import type { FormEvent } from 'react';
import type { SessionUser } from '../auth/api';
import { useComments } from './useComments';
import './CommentsSection.css';

interface CommentsSectionProps {
  videoId: string;
  user: SessionUser | null;
}
function getInitials(displayName: string | null, username: string): string {
  const source = displayName?.trim() || username.trim();
  if (!source) {
    return '?';
  }
  return source
    .split(/[\s_]+/)
    .filter(Boolean)
    .map((word) => word[0])
    .join('')
    .slice(0, 2)
    .toUpperCase();
}

function formatCommentDate(createdAt: string): string {
  const date = new Date(createdAt);
  if (Number.isNaN(date.getTime())) {
    return 'Fecha no disponible';
  }
  return new Intl.DateTimeFormat('es-ES', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(date);
}

function resolveAvatarUrl(avatarUrl: string | null): string | undefined {
  if (!avatarUrl) {
    return undefined;
  }
  if (avatarUrl.startsWith('http://') || avatarUrl.startsWith('https://') || avatarUrl.startsWith('data:')) {
    return avatarUrl;
  }
  return avatarUrl;
}

export function CommentsSection({ videoId, user }: CommentsSectionProps) {
  const [content, setContent] = useState('');
  const { comments, loading, submitting, error, submitError, addComment } = useComments(videoId);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const normalizedContent = content.trim();
    if (!normalizedContent) {
      return;
    }
    if (!user) {
      window.location.hash = `/login?redirect=${encodeURIComponent(`/watch?video=${videoId}`)}`;
      return;
    }
    try {
      await addComment(normalizedContent);
      setContent('');
    } catch {
      //mostrado en el hook
    }
  }

  return (
    <section className="comments-section" aria-labelledby="comments-title">
      <div className="comments-heading">
        <h2 id="comments-title">Comentarios</h2>
        <span>{comments.length}</span>
      </div>
      <form className="comment-form" onSubmit={handleSubmit}>
        <textarea
          onChange={(event) => setContent(event.target.value)}
          placeholder="Escribe un comentario..."
          maxLength={2000}
          aria-label="Escribe un comentario"
          disabled={submitting}
        />
        <div className="comment-form-footer">
          <small>{content.length}/2000</small>
          <button type="submit" className="primary-button" disabled={submitting || !content.trim()}>
            {submitting ? 'Publicando...' : 'Comentar'}
          </button>
        </div>
        {submitError && (
          <p className="comments-error" role="alert">
            {submitError}
          </p>
        )}
      </form>

      {loading && (
        <p className="comments-status" role="status">
          Cargando comentarios...
        </p>
      )}

      {error && (
        <p className="comments-error" role="alert">
          {error}
        </p>
      )}
      {!loading && !error && comments.length === 0 && (
        <p className="comments-empty">Todavía no hay comentarios. Sé el primero en comentar.</p>
      )}

      <div className="comments-list">
        {comments.map((comment) => {
          const avatarUrl = resolveAvatarUrl(comment.avatarUrl);
          const authorName = comment.displayName?.trim() || comment.username;
          return (
            <article className="comment-item" key={comment.id}>
              {avatarUrl ? (
                <img className="comment-avatar" src={avatarUrl} alt={`Foto de perfil de ${authorName}`} />
              ) : (
                <span className="comment-avatar comment-avatar-fallback" aria-hidden="true">
                  {getInitials(comment.displayName, comment.username)}
                </span>
              )}
              <div className="comment-body">
                <div className="comment-author">
                  <strong>{authorName}</strong>
                  <span>@{comment.username}</span>
                  <time dateTime={comment.createdAt}>{formatCommentDate(comment.createdAt)}</time>
                </div>
                <p>{comment.content}</p>
              </div>
            </article>
          );
        })}
      </div>
    </section>
  );
}
