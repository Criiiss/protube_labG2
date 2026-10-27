import { Icon } from './Icon';
import type { IconName } from './Icon';

const links: { label: string; icon: IconName }[] = [
  { label: 'Inicio', icon: 'home' },
  { label: 'Suscripciones', icon: 'subscriptions' },
  { label: 'Biblioteca', icon: 'library' },
  { label: 'Tu perfil', icon: 'user' },
  { label: 'Canal', icon: 'play' },
  { label: 'Historial', icon: 'history' },
  { label: 'Listas de reproducción', icon: 'library' },
  { label: 'Me gusta', icon: 'like' },
  { label: 'Ver más tarde', icon: 'clock' },
  { label: 'Interacciones', icon: 'bell' },
];

export function Sidebar({
  expanded,
  onToggle,
  subscriptions = [],
  route = '/',
}: {
  expanded: boolean;
  onToggle: () => void;
  subscriptions?: string[];
  route?: string;
}) {
  const routes: Record<string, string> = {
    Inicio: '/',
    Suscripciones: '/subscriptions',
    Biblioteca: '/library',
    'Tu perfil': '/profile',
    Canal: '/profile',
    Historial: '/history',
    'Listas de reproducción': '/library',
    'Me gusta': '/liked',
    'Ver más tarde': '/later',
    Interacciones: '/liked',
  };
  return (
    <aside className={`sidebar ${expanded ? 'expanded' : ''}`}>
      <button
        className="sidebar-toggle icon-button"
        aria-label={expanded ? 'Contraer menú' : 'Expandir menú'}
        aria-expanded={expanded}
        aria-controls="main-navigation"
        onClick={onToggle}
      >
        <Icon name="menu" />
      </button>
      <nav id="main-navigation" aria-label="Navegación principal">
        {links.map((link, index) =>
          index === 0 ? (
            <a
              key={link.label}
              className={`nav-item ${route === '/' ? 'active' : ''}`}
              href="#/"
              aria-current={route === '/' ? 'page' : undefined}
            >
              <Icon name={link.icon} />
              <span>{link.label}</span>
            </a>
          ) : (
            <a
              key={link.label}
              className={`nav-item ${index > 2 ? 'expanded-only' : ''}`}
              href={`#${routes[link.label]}`}
            >
              <Icon name={link.icon} />
              <span>{link.label}</span>
            </a>
          )
        )}
        <div className="subscription-list expanded-only">
          <h2>Suscripciones</h2>
          {subscriptions.map((name, index) => (
            <a className="channel-link" key={name} href={`#/channel?name=${encodeURIComponent(name)}`}>
              <span className={`channel-avatar tone-${index % 3}`}>{name[0]}</span>
              {name}
            </a>
          ))}
          {!subscriptions.length && <p className="form-hint">Tus canales suscritos aparecerán aquí.</p>}
        </div>
      </nav>
    </aside>
  );
}
