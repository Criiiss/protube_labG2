import { Icon } from './Icon';
import type { SessionUser } from '../../auth/api';
import { SearchBox } from '../../search/SearchBox';

interface HeaderProps {
  onSearch: (query: string) => void;
  searchValue?: string;
  onAction: (message: string) => void;
  user?: SessionUser | null;
  sessionLoading?: boolean;
  onLogout?: () => void;
}

export function Header({ onSearch, searchValue, onAction, user, sessionLoading, onLogout }: HeaderProps) {
  return (
    <header className="site-header">
      <a className="brand" href={import.meta.env.BASE_URL} aria-label="protube: volver al inicio y refrescar">
        <img src={`${import.meta.env.BASE_URL}protube-imago-removebg-preview.png`} alt="" />
        <span>
          protube<span className="brand-dot">.</span>
        </span>
      </a>
      <div className="search-zone">
        <SearchBox onSearch={onSearch} value={searchValue} />
      </div>
      <div className="header-actions">
        <button
          className="icon-button"
          aria-label="Subir contenido"
          onClick={() => {
            window.location.hash = user ? '/studio' : '/login';
          }}
        >
          <Icon name="plus" />
        </button>
        <button
          className="icon-button"
          aria-label="Notificaciones"
          onClick={() => onAction('No tienes notificaciones nuevas.')}
        >
          <Icon name="bell" />
        </button>
        {user ? (
          <details className="user-menu">
            <summary className="avatar" aria-label="Menú de usuario">
              {user.avatarUrl && /^https?:\/\//.test(user.avatarUrl) ? (
                <img src={user.avatarUrl} alt="" referrerPolicy="no-referrer" />
              ) : (
                <Icon name="user" />
              )}
            </summary>
            <div className="dropdown-panel">
              <strong>{user.displayName}</strong>
              <a href="#/profile">Tu perfil</a>
              <a href="#/studio">Studio</a>
              <a href="#/settings">Ajustes</a>
              <button onClick={onLogout}>Cerrar sesión</button>
            </div>
          </details>
        ) : (
          !sessionLoading && (
            <div className="session-links">
              <a href="#/login">Acceder</a>
              <a className="register-link" href="#/register">
                Registrarse
              </a>
            </div>
          )
        )}
      </div>
    </header>
  );
}
