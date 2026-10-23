import { useEffect, useState } from 'react';
import { changeUsername, getMyProfile, removeAvatar, removeBanner, updateProfile } from './profileApi';
import ProfileEditForm from './ProfileEditForm';
import ProfileHeader from './ProfileHeader';
import ProfileStats from './ProfileStats';
import type { MyProfile } from './types';

interface ProfilePageProps {
  userId: number;
  onUpdated: () => void;
}

export function ProfilePage({ userId, onUpdated }: ProfilePageProps) {
  const [profile, setProfile] = useState<MyProfile | null>(null);
  const [editing, setEditing] = useState(false);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState<string | null>(null);

  useEffect(() => {
    let active = true;

    setLoading(true);
    setMessage(null);

    getMyProfile(userId)
      .then((loadedProfile) => {
        if (active) {
          setProfile(loadedProfile);
        }
      })
      .catch((error: unknown) => {
        if (active) {
          setMessage(error instanceof Error ? error.message : 'No se pudo cargar el perfil.');
        }
      })
      .finally(() => {
        if (active) {
          setLoading(false);
        }
      });

    return () => {
      active = false;
    };
  }, [userId]);

  if (loading) {
    return (
      <section className="profile-state">
        <p>Cargando perfil...</p>
      </section>
    );
  }

  if (message && !profile) {
    return (
      <section className="profile-state">
        <h1>No se pudo cargar el perfil</h1>
        <p>{message}</p>
      </section>
    );
  }

  if (!profile) {
    return (
      <section className="profile-state">
        <h1>Perfil no encontrado</h1>
      </section>
    );
  }

  const saveProfile = async (data: Parameters<typeof updateProfile>[1]) => {
    setSaving(true);
    setMessage(null);

    try {
      const updatedProfile = await updateProfile(userId, data);
      setProfile(updatedProfile);
      onUpdated();
    } finally {
      setSaving(false);
    }
  };

  const saveUsername = async (username: string) => {
    setSaving(true);
    setMessage(null);

    try {
      const updatedProfile = await changeUsername(userId, { username });
      setProfile(updatedProfile);
      onUpdated();
    } finally {
      setSaving(false);
    }
  };

  const handleRemoveAvatar = async () => {
    const updatedProfile = await removeAvatar(userId);
    setProfile(updatedProfile);
  };

  const handleRemoveBanner = async () => {
    const updatedProfile = await removeBanner(userId);
    setProfile(updatedProfile);
  };

  if (editing) {
    return (
      <section className="profile-page">
        <ProfileEditForm
          profile={profile}
          saving={saving}
          onSaveProfile={saveProfile}
          onSaveUsername={saveUsername}
          onRemoveAvatar={handleRemoveAvatar}
          onRemoveBanner={handleRemoveBanner}
          onCancel={() => setEditing(false)}
        />
      </section>
    );
  }

  return (
    <section className="profile-page">
      <ProfileHeader profile={profile} isOwner onEdit={() => setEditing(true)} />

      <ProfileStats
        stats={{
          subscriptionsCount: 0,
          videosCount: 0,
        }}
      />

      <section className="profile-content">
        <div className="profile-content-heading">
          <p className="profile-eyebrow">Contenido del canal</p>
          <h2>Vídeos</h2>
        </div>

        <div className="profile-empty-content">
          <span className="profile-empty-icon" aria-hidden="true">
            ▶
          </span>
          <h3>Este canal todavía no tiene vídeos</h3>
          <p>Cuando publiques vídeos, aparecerán aquí para otros usuarios.</p>
        </div>
      </section>

      {message && (
        <p className="profile-form-error" role="status">
          {message}
        </p>
      )}
    </section>
  );
}
