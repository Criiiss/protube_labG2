import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { getMyProfile, updatePrivacy } from '../profile/profileApi';
import type { MyProfile, PrivacyVisibility } from '../profile/types';
import { deleteAccount, getAccount, sendPasswordEmail } from './settingsApi.ts';
import type { AccountInfo } from './settingsApi.ts';
import '../profile/profile.css';
import './settings.css';

interface SettingsPageProps {
  userId: number;
  onLogout: () => Promise<void>;
  onAccountDeleted: () => void;
}

type Status = { ok: boolean; text: string } | null;

const errorText = (error: unknown) =>
  error instanceof Error && error.message ? error.message : 'No se ha podido completar la operación.';

function StatusMessage({ status }: { status: Status }) {
  if (!status) return null;
  return (
    <p className={status.ok ? 'settings-ok' : 'profile-form-error'} role="status">
      {status.text}
    </p>
  );
}

/**
 * Account settings: what only affects the owner (email, password, privacy, session, deleting the account).
 * The public identity (name, @username, photo, banner, biography) is edited from the channel ("Editar perfil").
 */
export function SettingsPage({ userId, onLogout, onAccountDeleted }: SettingsPageProps) {
  const [profile, setProfile] = useState<MyProfile | null>(null);
  const [account, setAccount] = useState<AccountInfo | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);

  const [subscriptions, setSubscriptions] = useState<PrivacyVisibility>('PUBLIC');
  const [liked, setLiked] = useState<PrivacyVisibility>('PUBLIC');
  const [privacyStatus, setPrivacyStatus] = useState<Status>(null);
  const [savingPrivacy, setSavingPrivacy] = useState(false);

  const [passwordStatus, setPasswordStatus] = useState<Status>(null);
  const [sendingEmail, setSendingEmail] = useState(false);

  const [confirmation, setConfirmation] = useState('');
  const [understood, setUnderstood] = useState(false);
  const [deleteStatus, setDeleteStatus] = useState<Status>(null);
  const [deleting, setDeleting] = useState(false);

  useEffect(() => {
    Promise.all([getMyProfile(userId), getAccount()])
      .then(([loadedProfile, loadedAccount]) => {
        setProfile(loadedProfile);
        setAccount(loadedAccount);
        setSubscriptions(loadedProfile.subscriptionsVisibility);
        setLiked(loadedProfile.likedVideosVisibility);
      })
      .catch((error: unknown) => setLoadError(errorText(error)));
  }, [userId]);

  const savePrivacy = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSavingPrivacy(true);
    setPrivacyStatus(null);
    try {
      setProfile(await updatePrivacy(userId, { subscriptionsVisibility: subscriptions, likedVideosVisibility: liked }));
      setPrivacyStatus({ ok: true, text: 'Privacidad actualizada.' });
    } catch (error) {
      setPrivacyStatus({ ok: false, text: errorText(error) });
    } finally {
      setSavingPrivacy(false);
    }
  };

  const requestPasswordEmail = async () => {
    setSendingEmail(true);
    setPasswordStatus(null);
    try {
      setPasswordStatus({ ok: true, text: (await sendPasswordEmail()).message });
    } catch (error) {
      setPasswordStatus({ ok: false, text: errorText(error) });
    } finally {
      setSendingEmail(false);
    }
  };

  const removeAccount = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setDeleting(true);
    setDeleteStatus(null);
    try {
      await deleteAccount(confirmation);
      onAccountDeleted();
    } catch (error) {
      setDeleteStatus({ ok: false, text: errorText(error) });
      setDeleting(false);
    }
  };

  if (loadError) {
    return (
      <p className="profile-form-error" role="alert">
        {loadError}
      </p>
    );
  }
  if (!profile || !account) return <p className="settings-loading">Cargando ajustes...</p>;

  const google = account.provider === 'GOOGLE';

  return (
    <section className="settings-page" aria-labelledby="settings-title">
      <p className="profile-eyebrow">Tu cuenta</p>
      <h1 id="settings-title">Ajustes</h1>

      <section className="settings-card" aria-labelledby="settings-account">
        <h2 id="settings-account">Cuenta</h2>
        <dl className="settings-list">
          <dt>Usuario</dt>
          <dd>@{profile.username}</dd>
          <dt>Email</dt>
          <dd>
            {account.email}{' '}
            <span className={account.emailVerified ? 'settings-badge' : 'settings-badge settings-badge--warning'}>
              {account.emailVerified ? 'Verificado' : 'Sin verificar'}
            </span>
          </dd>
          <dt>Acceso</dt>
          <dd>{google ? 'Con tu cuenta de Google' : 'Con email y contraseña'}</dd>
        </dl>
        <p className="settings-hint">
          Tu nombre, tu @usuario, la foto, el banner y la biografía son tu perfil público: se cambian desde tu canal.
        </p>
        <a className="profile-secondary-button" href="#/profile">
          Editar perfil público
        </a>
      </section>

      <form
        className="settings-card profile-privacy-settings"
        aria-labelledby="settings-privacy"
        onSubmit={savePrivacy}
      >
        <h2 id="settings-privacy">Privacidad</h2>
        <label className="profile-form-field">
          <span>Mis suscripciones</span>
          <select value={subscriptions} onChange={(event) => setSubscriptions(event.target.value as PrivacyVisibility)}>
            <option value="PUBLIC">Públicas</option>
            <option value="PRIVATE">Privadas</option>
          </select>
          <small>Decide si otros usuarios pueden ver tus suscripciones.</small>
        </label>
        <label className="profile-form-field">
          <span>Mis vídeos favoritos</span>
          <select value={liked} onChange={(event) => setLiked(event.target.value as PrivacyVisibility)}>
            <option value="PUBLIC">Públicos</option>
            <option value="PRIVATE">Privados</option>
          </select>
          <small>Decide si otros usuarios pueden ver tus vídeos favoritos.</small>
        </label>
        <StatusMessage status={privacyStatus} />
        <button type="submit" className="profile-primary-button" disabled={savingPrivacy}>
          {savingPrivacy ? 'Guardando...' : 'Guardar privacidad'}
        </button>
      </form>

      <section className="settings-card" aria-labelledby="settings-password">
        <h2 id="settings-password">Contraseña</h2>
        {google ? (
          <p className="settings-hint">Entras con Google: la contraseña se cambia en tu cuenta de Google.</p>
        ) : (
          <>
            <p className="settings-hint">Te enviaremos a {account.email} un enlace para elegir una contraseña nueva.</p>
            <StatusMessage status={passwordStatus} />
            <button
              type="button"
              className="profile-primary-button"
              disabled={sendingEmail}
              onClick={() => void requestPasswordEmail()}
            >
              {sendingEmail ? 'Enviando...' : 'Enviarme el enlace'}
            </button>
          </>
        )}
      </section>

      <section className="settings-card" aria-labelledby="settings-session">
        <h2 id="settings-session">Sesión</h2>
        <button type="button" className="profile-secondary-button" onClick={() => void onLogout()}>
          Cerrar sesión
        </button>
      </section>

      <form className="settings-card settings-danger" aria-labelledby="settings-delete" onSubmit={removeAccount}>
        <h2 id="settings-delete">Eliminar cuenta</h2>
        <p className="settings-hint">
          Se borrarán tu canal, tus vídeos, comentarios, playlists y likes. Esta acción no se puede deshacer.
        </p>
        <label className="profile-form-field">
          <span>{google ? 'Escribe tu nombre de usuario para confirmar' : 'Tu contraseña'}</span>
          <input
            type={google ? 'text' : 'password'}
            autoComplete={google ? 'off' : 'current-password'}
            required
            value={confirmation}
            onChange={(event) => setConfirmation(event.target.value)}
          />
        </label>
        <label className="settings-check">
          <input type="checkbox" checked={understood} onChange={(event) => setUnderstood(event.target.checked)} />
          Entiendo que mi cuenta se eliminará para siempre
        </label>
        <StatusMessage status={deleteStatus} />
        <button type="submit" className="settings-danger-button" disabled={!understood || deleting}>
          {deleting ? 'Eliminando...' : 'Eliminar mi cuenta'}
        </button>
      </form>
    </section>
  );
}
