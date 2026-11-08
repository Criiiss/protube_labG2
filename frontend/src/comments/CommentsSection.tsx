import { useState } from 'react';
import type { FormEvent } from 'react';
import type { SessionUser } from '../auth/api';
import { useComments } from './useComments';
import './CommentsSection.css';
import type { Comment } from './commentsApi';

interface CommentsSectionProps {
  videoId: string;
  user: SessionUser | null;
}

interface CommentItemProps {
  comment: Comment;
  comments: Comment[];
  user: SessionUser | null;
  submitting: boolean;
  onReply: (content: string, parentId: string | number) => Promise<void>;
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

  async function handleReply(replyContent: string, parentId: string | number) {
    await addComment(replyContent, parentId);
  }

  const rootComments = comments
    .filter((comment) => comment.parentId === null)
    .sort((first, second) => new Date(second.createdAt).getTime() - new Date(first.createdAt).getTime());

  return (
    <section className="comments-section" aria-labelledby="comments-title">
      <div className="comments-heading">
        <h2 id="comments-title">Comentarios</h2>
        <span>{comments.length}</span>
      </div>
      <form className="comment-form" onSubmit={handleSubmit}>
        <textarea
          value={content}
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
        {rootComments.map((comment) => (
          <CommentItem
            key={comment.id}
            comment={comment}
            comments={comments}
            user={user}
            submitting={submitting}
            onReply={handleReply}
          />
        ))}
      </div>
    </section>
  );
}
function isSameId(first: string | number | null, second: string | number | null): boolean {
  return String(first) === String(second);
}

function getReplies(comments: Comment[], parentId: string | number): Comment[] {
  return comments
    .filter((comment) => isSameId(comment.parentId, parentId))
    .sort((first, second) => new Date(second.createdAt).getTime() - new Date(first.createdAt).getTime());
}

function CommentItem({ comment, comments, user, submitting, onReply }: CommentItemProps) {
  const [replying, setReplying] = useState(false);
  const [replyContent, setReplyContent] = useState('');

  const authorName = comment.displayName?.trim() || comment.username;

  const avatarUrl = resolveAvatarUrl(comment.avatarUrl);
  const replies = getReplies(comments, comment.id);

  async function handleReplySubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    const normalizedContent = replyContent.trim();

    if (!normalizedContent) {
      return;
    }

    if (!user) {
      window.location.hash = `/login?redirect=${encodeURIComponent(`/watch?video=${comment.videoId}`)}`;
      return;
    }

    await onReply(normalizedContent, comment.id);
    setReplyContent('');
    setReplying(false);
  }

  return (
    <article className="comment-thread">
      <div className="comment-item">
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

          <button type="button" className="comment-reply-button" onClick={() => setReplying(!replying)}>
            {replying ? 'Cancelar' : 'Responder'}
          </button>

          {replying && (
            <form className="reply-form" onSubmit={handleReplySubmit}>
              <textarea
                value={replyContent}
                onChange={(event) => setReplyContent(event.target.value)}
                placeholder="Escribe una respuesta..."
                maxLength={2000}
                disabled={submitting}
                aria-label={`Responder a ${authorName}`}
              />

              <button type="submit" className="primary-button" disabled={submitting || !replyContent.trim()}>
                {submitting ? 'Publicando...' : 'Responder'}
              </button>
            </form>
          )}
        </div>
      </div>

      {replies.length > 0 && (
        <div className="comment-replies">
          {replies.map((reply) => (
            <CommentItem
              key={reply.id}
              comment={reply}
              comments={comments}
              user={user}
              submitting={submitting}
              onReply={onReply}
            />
          ))}
        </div>
      )}
    </article>
  );
}
