import { getEnv } from '../utils/Env';
import type { PublicProfile } from './types';
import { JSX } from 'react';

interface ProfileHeaderProps {
  profile: PublicProfile;
  isOwner: boolean;
  onEdit?: () => void;
}

const DEFAULT_AVATAR = '/protube-imago-removebg-preview.png';
const DEFAULT_BANNER = '/protube-logo-removebg-preview.png';

const getImageUrl = (imageUrl: string | null | undefined, fallback: string): string => {
  if (!imageUrl) {
    return fallback;
  }

  if (imageUrl.startsWith('http://') || imageUrl.startsWith('https://') || imageUrl.startsWith('data:')) {
    return imageUrl;
  }

  return `${getEnv().MEDIA_BASE_URL}${imageUrl}`;
};

const formatCreationDate = (createdAt: string): string => {
  const date = new Date(createdAt);

  if (Number.isNaN(date.getTime())) {
    return 'Fecha no disponible';
  }

  return new Intl.DateTimeFormat('es-ES', {
    month: 'long',
    year: 'numeric',
  }).format(date);
};

const getInitials = (displayName: string | null): string => {
  if (!displayName?.trim()) {
    return '?';
  }

  return displayName
    .trim()
    .split(/\s+/)
    .map((word) => word[0])
    .join('')
    .slice(0, 2)
    .toUpperCase();
};

function ProfileHeader({ profile, isOwner, onEdit }: ProfileHeaderProps): JSX.Element {
  const avatarUrl = getImageUrl(profile.avatarUrl, DEFAULT_AVATAR);
  const bannerUrl = getImageUrl(profile.bannerUrl, DEFAULT_BANNER);
  const displayName = profile.displayName || 'Canal sin nombre';

  return (
    <section className="profile-header" aria-labelledby="profile-name">
      <div
        className="profile-banner"
        style={{ backgroundImage: `url("${bannerUrl}")` }}
        role="img"
        aria-label={`Banner del canal de ${displayName}`}
      />

      <div className="profile-header-content">
        <div className="profile-avatar-wrapper">
          <img
            className="profile-avatar"
            src={avatarUrl}
            alt={`Foto de perfil de ${displayName}`}
            onError={(event) => {
              event.currentTarget.src = DEFAULT_AVATAR;
            }}
          />

          {!profile.avatarUrl && (
            <span className="profile-avatar-initials" aria-hidden="true">
              {getInitials(profile.displayName)}
            </span>
          )}
        </div>

        <div className="profile-main-info">
          <div className="profile-title-row">
            <div>
              <h1 id="profile-name">{displayName}</h1>
              <p className="profile-username">@{profile.username}</p>
            </div>

            {isOwner && onEdit && (
              <button type="button" className="profile-edit-button" onClick={onEdit}>
                Editar perfil
              </button>
            )}
          </div>

          <p className="profile-biography">{profile.biography || 'Este canal todavía no tiene una biografía.'}</p>

          <p className="profile-created-date">Canal creado en {formatCreationDate(profile.createdAt)}</p>
        </div>
      </div>
    </section>
  );
}

export default ProfileHeader;
