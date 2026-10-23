import { useEffect, useState } from 'react';
import type { PublicProfile, UpdateProfileRequest } from './types';
import ProfileImagePicker from './ProfileImagePicker';

interface ProfileEditFormProps {
  profile: PublicProfile;
  saving?: boolean;
  onSaveProfile: (data: UpdateProfileRequest) => Promise<void>;
  onSaveUsername: (username: string) => Promise<void>;
  onRemoveAvatar: () => Promise<void>;
  onRemoveBanner: () => Promise<void>;
  onCancel: () => void;
}

const MAX_DISPLAY_NAME_LENGTH = 50;
const MAX_USERNAME_LENGTH = 20;
const MIN_USERNAME_LENGTH = 3;
const MAX_BIOGRAPHY_LENGTH = 250;

function ProfileEditForm({
  profile,
  saving = false,
  onSaveProfile,
  onSaveUsername,
  onRemoveAvatar,
  onRemoveBanner,
  onCancel,
}: ProfileEditFormProps) {
  const [displayName, setDisplayName] = useState(profile.displayName ?? '');
  const [username, setUsername] = useState(profile.username);
  const [biography, setBiography] = useState(profile.biography ?? '');
  const [avatarUrl, setAvatarUrl] = useState<string | null>(profile.avatarUrl);
  const [bannerUrl, setBannerUrl] = useState<string | null>(profile.bannerUrl);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setDisplayName(profile.displayName ?? '');
    setUsername(profile.username);
    setBiography(profile.biography ?? '');
    setAvatarUrl(profile.avatarUrl);
    setBannerUrl(profile.bannerUrl);
  }, [profile]);

  const validateForm = (): string | null => {
    const normalizedUsername = username.trim().toLowerCase();
    if (!displayName.trim()) return 'El nombre del canal es obligatorio.';
    if (displayName.trim().length > MAX_DISPLAY_NAME_LENGTH) return `El nombre no puede superar los ${MAX_DISPLAY_NAME_LENGTH} caracteres.`;
    if (normalizedUsername.length < MIN_USERNAME_LENGTH || normalizedUsername.length > MAX_USERNAME_LENGTH) {
      return `El nombre de usuario debe tener entre ${MIN_USERNAME_LENGTH} y ${MAX_USERNAME_LENGTH} caracteres.`;
    }
    if (!/^[a-z0-9_]+$/.test(normalizedUsername)) return 'El nombre de usuario solo puede contener letras minúsculas, números y guiones bajos.';
    if (biography.length > MAX_BIOGRAPHY_LENGTH) return `La biografía no puede superar los ${MAX_BIOGRAPHY_LENGTH} caracteres.`;
    return null;
  };

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>): Promise<void> => {
    event.preventDefault();
    setError(null);
    const validationError = validateForm();

    if (validationError) {
      setError(validationError);
      return;
    }
    try {
      const normalizedUsername = username.trim().toLowerCase();
      const profileData: UpdateProfileRequest = {
        displayName: displayName.trim(),
        biography: biography.trim() || null,
        avatarUrl,
        bannerUrl,
      };

      await onSaveProfile(profileData);
      if (normalizedUsername !== profile.username) await onSaveUsername(normalizedUsername);
      onCancel();
    } catch (saveError) {
      setError(saveError instanceof Error ? saveError.message : 'No se han podido guardar los cambios.');
    }
  };

  const handleRemoveAvatar = async (): Promise<void> => {
    await onRemoveAvatar();
    setAvatarUrl(null);
  };

  const handleRemoveBanner = async (): Promise<void> => {
    await onRemoveBanner();
    setBannerUrl(null);
  };

  return (
    <form className="profile-edit-form" onSubmit={handleSubmit} noValidate>
      <div className="profile-edit-form-header">
        <div>
          <p className="profile-eyebrow">Configuración del canal</p>
          <h2>Editar perfil</h2>
        </div>
        <button type="button" className="profile-secondary-button" onClick={onCancel} disabled={saving}>
          Cancelar
        </button>
      </div>
      {error && (
        <p className="profile-form-error" role="alert">
          {error}
        </p>
      )}

      <div className="profile-form-grid">
        <label className="profile-form-field">
          <span>Nombre del canal</span>
          <input
            type="text"
            value={displayName}
            maxLength={MAX_DISPLAY_NAME_LENGTH}
            disabled={saving}
            onChange={(event) => setDisplayName(event.target.value)}
            required
          />
          <small>
            {displayName.length}/{MAX_DISPLAY_NAME_LENGTH}
          </small>
        </label>

        <label className="profile-form-field">
          <span>Nombre de usuario</span>
          <div className="profile-username-input">
            <span>@</span>
            <input
              type="text"
              value={username}
              maxLength={MAX_USERNAME_LENGTH}
              disabled={saving}
              onChange={(event) => setUsername(event.target.value.toLowerCase())}
              required
            />
          </div>
          <small>Entre 3 y 20 caracteres: letras, números o _.</small>
        </label>
      </div>

      <label className="profile-form-field">
        <span>Biografía</span>
        <textarea
          value={biography}
          maxLength={MAX_BIOGRAPHY_LENGTH}
          rows={5}
          disabled={saving}
          onChange={(event) => setBiography(event.target.value)}
        />
        <small>
          {biography.length}/{MAX_BIOGRAPHY_LENGTH}
        </small>
      </label>

      <div className="profile-image-picker-grid">
        <ProfileImagePicker
          label="Foto de perfil"
          value={avatarUrl}
          aspectRatio="avatar"
          disabled={saving}
          onChange={setAvatarUrl}
          onRemove={() => {
            void handleRemoveAvatar();
          }}
        />

        <ProfileImagePicker
          label="Banner del canal"
          value={bannerUrl}
          aspectRatio="banner"
          disabled={saving}
          onChange={setBannerUrl}
          onRemove={() => {
            void handleRemoveBanner();
          }}
        />
      </div>

      <div className="profile-edit-form-actions">
        <button type="button" className="profile-secondary-button" onClick={onCancel} disabled={saving}>
          Cancelar
        </button>

        <button type="submit" className="profile-primary-button" disabled={saving}>
          {saving ? 'Guardando...' : 'Guardar cambios'}
        </button>
      </div>
    </form>
  );
}

export default ProfileEditForm;
