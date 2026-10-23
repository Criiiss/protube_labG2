import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { api, ApiError } from './api';
import type { SessionUser } from './api';
import { hasMinimumAge, validatePassword } from './validation';
import { getEnv } from '../utils/Env';

const titles: Record<string, string> = {
  login: 'Te estábamos esperando.',
  register: 'Tu historia empieza aquí.',
  forgot: 'Recupera tu acceso.',
  reset: 'Una nueva contraseña.',
  verify: 'Verifica tu email.',
  resend: 'Un nuevo enlace.',
  'google-register': 'Un último paso.',
};
export function AuthPage({ mode, onLogin }: { mode: string; onLogin: (user: SessionUser) => void }) {
  const [values, setValues] = useState<Record<string, string>>({});
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [message, setMessage] = useState('');
  const [busy, setBusy] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [captcha, setCaptcha] = useState('');
  const [config, setConfig] = useState({ minimumAge: 13, googleEnabled: false });
  const token = new URLSearchParams(window.location.hash.split('?')[1]).get('token');
  const loadCaptcha = () =>
    api<{ question: string }>('/auth/captcha')
      .then((data) => setCaptcha(data.question))
      .catch(() => setCaptcha('No se pudo cargar la comprobación.'));
  useEffect(() => {
    void api<typeof config>('/auth/config')
      .then(setConfig)
      .catch(() => {});
    if (mode === 'register') void loadCaptcha();
  }, [mode]);
  const field = (name: string, label: string, type = 'text', autoComplete?: string) => (
    <label className="form-field" key={name}>
      <span>{label}</span>
      <input
        name={name}
        type={type}
        autoComplete={autoComplete}
        required
        value={values[name] || ''}
        aria-invalid={!!errors[name]}
        aria-describedby={errors[name] ? `${name}-error` : undefined}
        onChange={(event) => setValues({ ...values, [name]: event.target.value })}
      />
      {errors[name] && (
        <small id={`${name}-error`} className="field-error">
          {errors[name]}
        </small>
      )}
    </label>
  );
  async function submit(event: FormEvent) {
    event.preventDefault();
    setMessage('');
    const issues: Record<string, string> = {};
    if (mode === 'register' || mode === 'reset') {
      const passwordError = validatePassword(values.password || '');
      if (passwordError) issues.password = passwordError;
      if (values.password !== values.confirmPassword) issues.confirmPassword = 'Las contraseñas no coinciden.';
    }
    if (mode === 'register' || mode === 'google-register') {
      if (!/^[a-zA-Z0-9_]{3,30}$/.test(values.username || ''))
        issues.username = 'Usa 3–30 letras, números o guion bajo.';
      if (!hasMinimumAge(values.birthDate || '', config.minimumAge))
        issues.birthDate = `Debes tener al menos ${config.minimumAge} años.`;
      if (values.acceptTerms !== 'yes') issues.acceptTerms = 'Debes aceptar los términos y condiciones.';
    }
    setErrors(issues);
    if (Object.keys(issues).length) return;
    setBusy(true);
    try {
      if (mode === 'login') {
        const user = await api<SessionUser>('/auth/login', { ...values, remember: values.remember === 'yes' });
        onLogin(user);
        window.location.hash = '/';
      } else if (mode === 'google-register') {
        const user = await api<SessionUser>('/auth/google/complete', {
          ...values,
          acceptTerms: values.acceptTerms === 'yes',
        });
        onLogin(user);
        window.location.hash = '/';
      } else {
        const data = await api<{ message: string }>(`/auth/${mode}`, {
          ...values,
          token,
          acceptTerms: values.acceptTerms === 'yes',
        });
        setMessage(data.message);
        setValues({});
        if (mode === 'verify' || mode === 'reset')
          window.history.replaceState(null, '', `${window.location.pathname}#/${mode}`);
      }
    } catch (error) {
      if (error instanceof ApiError) {
        setMessage(error.message);
        setErrors(error.fields);
      } else setMessage('No se pudo completar la solicitud.');
    } finally {
      setBusy(false);
      if (mode === 'register') {
        setValues((current) => ({ ...current, captcha: '' }));
        void loadCaptcha();
      }
    }
  }
  const registering = mode === 'register' || mode === 'google-register';
  return (
    <section className="auth-layout">
      <div className="auth-intro">
        <p className="eyebrow">TU ESPACIO EN PROTUBE</p>
        <h1>{titles[mode] || titles.login}</h1>
        <p>Descubre, comparte y guarda lo que te inspira.</p>
        <a href="#/">Volver a explorar →</a>
      </div>
      <div className="auth-card">
        <h2>{mode === 'login' ? 'Iniciar sesión' : mode === 'register' ? 'Crear una cuenta' : titles[mode]}</h2>
        <form onSubmit={submit}>
          {mode === 'login' && (
            <>
              {field('identifier', 'Email o nombre de usuario', 'text', 'username')}
              {field('password', 'Contraseña', showPassword ? 'text' : 'password', 'current-password')}
            </>
          )}
          {mode === 'register' && field('name', 'Nombre', 'text', 'name')}
          {registering && field('username', 'Nombre de usuario', 'text', 'username')}
          {(mode === 'register' || mode === 'forgot' || mode === 'resend') && field('email', 'Email', 'email', 'email')}
          {(mode === 'register' || mode === 'reset') && (
            <>
              {field('password', 'Contraseña', showPassword ? 'text' : 'password', 'new-password')}
              {field('confirmPassword', 'Confirmar contraseña', showPassword ? 'text' : 'password', 'new-password')}
              <small className="form-hint">Mínimo 8 caracteres, una mayúscula y un número.</small>
            </>
          )}
          {['login', 'register', 'reset'].includes(mode) && (
            <button
              type="button"
              className="text-button"
              aria-pressed={showPassword}
              onClick={() => setShowPassword(!showPassword)}
            >
              {showPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'}
            </button>
          )}
          {registering && (
            <>
              {field('birthDate', `Fecha de nacimiento (mínimo ${config.minimumAge} años)`, 'date', 'bday')}
              <label className="check-field">
                <input
                  type="checkbox"
                  checked={values.acceptTerms === 'yes'}
                  onChange={(e) => setValues({ ...values, acceptTerms: e.target.checked ? 'yes' : '' })}
                />
                <span>
                  Acepto los{' '}
                  <a href="#/terms" target="_blank" rel="noreferrer">
                    términos y condiciones
                  </a>
                  .
                </span>
              </label>
              {errors.acceptTerms && <small className="field-error">{errors.acceptTerms}</small>}
            </>
          )}
          {mode === 'register' && (
            <div className="captcha-block">
              {field('captcha', captcha || 'Cargando comprobación…', 'text', 'off')}
              <button type="button" className="text-button" onClick={() => void loadCaptcha()}>
                Cambiar comprobación
              </button>
            </div>
          )}
          {mode === 'login' && (
            <label className="check-field">
              <input
                type="checkbox"
                checked={values.remember === 'yes'}
                onChange={(e) => setValues({ ...values, remember: e.target.checked ? 'yes' : '' })}
              />
              <span>Recordarme durante 30 días</span>
            </label>
          )}
          {mode === 'verify' && !message && <p>Confirma tu email para activar tu cuenta.</p>}
          {message && (
            <p className="form-message" role="status">
              {message}
            </p>
          )}
          <button className="primary-button" disabled={busy || (['verify', 'reset'].includes(mode) && !token)}>
            {busy
              ? 'Un momento…'
              : mode === 'login'
                ? 'Entrar'
                : mode === 'register'
                  ? 'Crear cuenta'
                  : mode === 'verify'
                    ? 'Verificar email'
                    : mode === 'reset'
                      ? 'Guardar contraseña'
                      : mode === 'google-register'
                        ? 'Completar cuenta'
                        : 'Enviar enlace'}
          </button>
        </form>
        {mode === 'login' && (
          <>
            <a
              className="google-button"
              aria-disabled={!config.googleEnabled}
              href={config.googleEnabled ? `${getEnv().API_BASE_URL}/auth/google` : undefined}
            >
              Continuar con Google
            </a>
            {!config.googleEnabled && (
              <small className="form-hint">El acceso con Google aún no está configurado.</small>
            )}
            <div className="auth-links">
              <a href="#/forgot">¿Has olvidado la contraseña?</a>
              <a href="#/register">Crear cuenta</a>
              <a href="#/resend">Reenviar verificación</a>
            </div>
            {window.location.hash.includes('error=google') && (
              <p className="field-error">
                No se pudo completar Google. Si el email ya tiene cuenta, utiliza tu contraseña.
              </p>
            )}
          </>
        )}
        {mode !== 'login' && (
          <div className="auth-links">
            <a href="#/login">Iniciar sesión</a>
            {['verify', 'reset'].includes(mode) && (
              <a href={mode === 'reset' ? '#/forgot' : '#/resend'}>Solicitar un nuevo enlace</a>
            )}
          </div>
        )}
      </div>
    </section>
  );
}
