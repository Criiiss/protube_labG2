import { useEffect, useState } from 'react';
import { getPublicProfile } from './profileApi';
import ProfileHeader from './ProfileHeader';
import type { PublicProfile } from './types';

interface PublicProfilePageProps {
  username: string;
}

export function PublicProfilePage({ username }: PublicProfilePageProps) {
  const [profile, setProfile] = useState<PublicProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<'videos'>('videos');

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError(null);
    setProfile(null);

    getPublicProfile(username)
      .then((loadedProfile) => {
        if (active) setProfile(loadedProfile);
      })
      .catch((requestError: unknown) => {
        if (active) setError(requestError instanceof Error ? requestError.message : 'No se pudo cargar el perfil.');
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, [username]);

  if (loading) {
    return (
      <section className="profile-state">
        <p>Cargando perfil...</p>
      </section>
    );
  }

  if (error || !profile) {
    return (
      <section className="profile-state">
        <h1>Perfil no encontrado</h1>
        <p>{error || 'Este usuario no existe.'}</p>
        <a href="#/">Volver al inicio</a>
      </section>
    );
  }

  return (
    <section className="profile-page">
      <ProfileHeader profile={profile} isOwner={false} />
      <section className="profile-tabs" aria-label="Secciones del perfil">
        <button
          type="button"
          className="profile-tab profile-tab--active"
          aria-selected={activeTab === 'videos'}
          onClick={() => setActiveTab('videos')}
        >
          Vídeos publicados
        </button>
      </section>
      <section className="profile-content">
        <div className="profile-video-tabs" role="tablist" aria-label="Vídeos del canal">
          <button type="button" role="tab" aria-selected="true" className="profile-video-tab profile-video-tab--active">
            Publicados
          </button>
        </div>

        <div className="profile-empty-content">
          <span className="profile-empty-icon" aria-hidden="true">
            ▶
          </span>
          <h3>Todavía no hay vídeos publicados</h3>
          <p>Los vídeos publicados por este usuario aparecerán aquí.</p>
        </div>
      </section>
    </section>
  );
}
