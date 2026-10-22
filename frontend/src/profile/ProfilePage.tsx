import { useEffect, useState } from 'react';
import { changeUsername, getMyProfile, updateProfile } from './profileApi';
import type { MyProfile } from './types';
export function ProfilePage({ userId, onUpdated }: { userId: number; onUpdated: () => void }) {
  const [profile, setProfile] = useState<MyProfile | null>(null);
  const [message, setMessage] = useState('');
  const [busy, setBusy] = useState(false);
  useEffect(() => {
    let alive = true;
    getMyProfile(userId)
      .then((data) => {
        if (alive) setProfile(data);
      })
      .catch(() => {
        if (alive) setMessage('No se pudo cargar tu perfil. Comprueba la conexión y tu sesión.');
      });
    return () => {
      alive = false;
    };
  }, [userId]);
  return (
    <section className="account-page">
      <p className="eyebrow">TU ESPACIO</p>
      <h1>Tu perfil.</h1>
      {message && (
        <p role="status" className="form-message">
          {message}
        </p>
      )}
      {profile && (
        <form
          className="auth-card"
          onSubmit={async (event) => {
            event.preventDefault();
            setBusy(true);
            setMessage('');
            try {
              const current = await getMyProfile(userId);
              if (current.username !== profile.username) await changeUsername(userId, { username: profile.username });
              const updated = await updateProfile(userId, {
                displayName: profile.displayName || '',
                biography: profile.biography,
                avatarUrl: profile.avatarUrl,
                bannerUrl: profile.bannerUrl,
              });
              setProfile(updated);
              setMessage('Perfil actualizado.');
              onUpdated();
            } catch (error) {
              setMessage(error instanceof Error ? error.message : 'No se pudo guardar el perfil.');
            } finally {
              setBusy(false);
            }
          }}
        >
          {(['displayName', 'username', 'biography'] as const).map((key, index) => (
            <label className="form-field" key={key}>
              <span>{['Nombre', 'Nombre de usuario', 'Biografía'][index]}</span>
              <input
                required={key !== 'biography'}
                maxLength={key === 'biography' ? 250 : key === 'username' ? 30 : 50}
                value={profile[key] || ''}
                onChange={(e) => setProfile({ ...profile, [key]: e.target.value })}
              />
            </label>
          ))}
          <button className="primary-button" disabled={busy}>
            {busy ? 'Guardando…' : 'Guardar cambios'}
          </button>
        </form>
      )}
    </section>
  );
}
