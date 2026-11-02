import { useEffect, useState } from 'react';
import { changeUsername, getMyProfile, removeAvatar, removeBanner, updatePrivacy, updateProfile } from './profileApi';
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
  const [activeTab, setActiveTab] = useState<'videos' | 'analytics'>('videos');
  const [activeVideoTab, setActiveVideoTab] = useState<'published' | 'scheduled' | 'archived' | 'drafts'>('published');

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
  const savePrivacy = async (data: Parameters<typeof updatePrivacy>[1]) => {
    setSaving(true);
    setMessage(null);

    try {
      const updatedProfile = await updatePrivacy(userId, data);
      setProfile(updatedProfile);
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
          onSavePrivacy={savePrivacy}
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

      <section className="profile-tabs" aria-label="Secciones del perfil">
        <button
          type="button"
          className={`profile-tab${activeTab === 'videos' ? ' profile-tab--active' : ''}`}
          aria-selected={activeTab === 'videos'}
          onClick={() => setActiveTab('videos')}
        >
          Vídeos publicados
        </button>

        <button
          type="button"
          className={`profile-tab${activeTab === 'analytics' ? ' profile-tab--active' : ''}`}
          aria-selected={activeTab === 'analytics'}
          onClick={() => setActiveTab('analytics')}
        >
          Analíticas
        </button>
      </section>

      <section className="profile-content">
        {activeTab === 'videos' ? (
          <>
            <div className="profile-content-heading">
              <p className="profile-eyebrow">Contenido del canal</p>
              <h2>Vídeos</h2>
            </div>

            <div className="profile-video-tabs" role="tablist" aria-label="Estado de los vídeos">
              <button
                type="button"
                role="tab"
                aria-selected={activeVideoTab === 'published'}
                className={`profile-video-tab${activeVideoTab === 'published' ? ' profile-video-tab--active' : ''}`}
                onClick={() => setActiveVideoTab('published')}
              >
                Publicados
              </button>

              <button
                type="button"
                role="tab"
                aria-selected={activeVideoTab === 'scheduled'}
                className={`profile-video-tab${activeVideoTab === 'scheduled' ? ' profile-video-tab--active' : ''}`}
                onClick={() => setActiveVideoTab('scheduled')}
              >
                Programados
              </button>

              <button
                type="button"
                role="tab"
                aria-selected={activeVideoTab === 'archived'}
                className={`profile-video-tab${activeVideoTab === 'archived' ? ' profile-video-tab--active' : ''}`}
                onClick={() => setActiveVideoTab('archived')}
              >
                Archivados
              </button>

              <button
                type="button"
                role="tab"
                aria-selected={activeVideoTab === 'drafts'}
                className={`profile-video-tab${activeVideoTab === 'drafts' ? ' profile-video-tab--active' : ''}`}
                onClick={() => setActiveVideoTab('drafts')}
              >
                Borradores
              </button>
            </div>

            <div className="profile-empty-content">
              <span className="profile-empty-icon" aria-hidden="true">
                ▶
              </span>
              <h3>
                {activeVideoTab === 'published' && 'Todavía no hay vídeos publicados'}
                {activeVideoTab === 'scheduled' && 'Todavía no hay vídeos programados'}
                {activeVideoTab === 'archived' && 'Todavía no hay vídeos archivados'}
                {activeVideoTab === 'drafts' && 'Todavía no hay borradores'}
              </h3>
              <p>
                {activeVideoTab === 'published' && 'Los vídeos que publiques aparecerán aquí para otros usuarios.'}
                {activeVideoTab === 'scheduled' && 'Los vídeos programados solo serán visibles para ti.'}
                {activeVideoTab === 'archived' && 'Los vídeos archivados solo serán visibles para ti.'}
                {activeVideoTab === 'drafts' && 'Tus borradores solo serán visibles para ti.'}
              </p>
            </div>
          </>
        ) : (
          <>
            <div className="profile-content-heading">
              <p className="profile-eyebrow">Rendimiento del canal</p>
              <h2>Analíticas</h2>
            </div>

            <div className="profile-empty-content">
              <span className="profile-empty-icon" aria-hidden="true">
                ◌
              </span>
              <h3>Las analíticas estarán disponibles próximamente</h3>
              <p>Aquí podrás consultar el rendimiento de tus vídeos.</p>
            </div>
          </>
        )}
      </section>

      {message && (
        <p className="profile-form-error" role="status">
          {message}
        </p>
      )}
    </section>
  );
}
