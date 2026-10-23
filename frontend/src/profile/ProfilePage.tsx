import { useState } from 'react';
import ProfileEditForm from './ProfileEditForm';
import ProfileHeader from './ProfileHeader';
import ProfileStats from './ProfileStats';
import { useProfile } from './useProfile';

interface ProfilePageProps {
  userId?: number;
  username?: string;
}

function ProfilePage({ userId, username }: ProfilePageProps) {
  const [isEditing, setIsEditing] = useState(false);
  const { profile, isOwner, loading, saving, error, saveProfile, saveUsername, deleteAvatar, deleteBanner } =
    useProfile({
      userId,
      username,
    });
  if (loading) {
    return (
      <main className="profile-page profile-state">
        <p>Cargando perfil...</p>
      </main>
    );
  }
  if (error && !profile) {
    return (
      <main className="profile-page profile-state">
        <h1>No se ha podido cargar el perfil</h1>
        <p>{error}</p>
      </main>
    );
  }
  if (!profile) {
    return (
      <main className="profile-page profile-state">
        <h1>Perfil no encontrado</h1>
        <p>El canal que buscas no existe o no está disponible.</p>
      </main>
    );
  }
  if (isEditing && isOwner) {
    return (
      <main className="profile-page">
        <ProfileEditForm
          profile={profile}
          saving={saving}
          onSaveProfile={saveProfile}
          onSaveUsername={async (newUsername) => {
            await saveUsername({ username: newUsername });
          }}
          onRemoveAvatar={deleteAvatar}
          onRemoveBanner={deleteBanner}
          onCancel={() => setIsEditing(false)}
        />
      </main>
    );
  }
  return (
    <main className="profile-page">
      <ProfileHeader profile={profile} isOwner={isOwner} onEdit={() => setIsEditing(true)} />
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
          <p>Cuando se publiquen vídeos, aparecerán aquí para que otros usuarios puedan descubrirlos.</p>
        </div>
      </section>
    </main>
  );
}

export default ProfilePage;
