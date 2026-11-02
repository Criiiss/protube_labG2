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
          <p>Cuando publique vídeos, aparecerán aquí.</p>
        </div>
      </section>
    </section>
  );
}
