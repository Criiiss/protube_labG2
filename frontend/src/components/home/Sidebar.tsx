import { useState } from 'react';
import { Icon } from './Icon';
import type { IconName } from './Icon';

const links: { label: string; icon: IconName }[] = [
  { label: 'Inicio', icon: 'home' },
  { label: 'Suscripciones', icon: 'subscriptions' },
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
  onAction,
}: {
  expanded: boolean;
  onToggle: () => void;
  onAction: (message: string) => void;
}) {
  const [showAll, setShowAll] = useState(false);
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
            <a key={link.label} className="nav-item active" href={import.meta.env.BASE_URL} aria-current="page">
              <Icon name={link.icon} />
              <span>{link.label}</span>
            </a>
          ) : (
            <button
              key={link.label}
              className={`nav-item ${index > 2 ? 'expanded-only' : ''}`}
              onClick={() => onAction(`${link.label}: disponible cuando conectemos tu cuenta.`)}
            >
              <Icon name={link.icon} />
              <span>{link.label}</span>
            </button>
          )
        )}
        <div className="subscription-list expanded-only">
          <h2>Suscripciones</h2>
          {(showAll
            ? ['ProTube Studio', 'Blue Sessions', 'Curiosos', 'La ruta', 'En juego']
            : ['ProTube Studio', 'Blue Sessions', 'Curiosos']
          ).map((name, index) => (
            <button
              className="channel-link"
              key={name}
              onClick={() => onAction(`El canal ${name} estará disponible próximamente.`)}
            >
              <span className={`channel-avatar tone-${index % 3}`}>{name[0]}</span>
              {name}
            </button>
          ))}
          <button className="channel-link" onClick={() => setShowAll(!showAll)}>
            <span aria-hidden="true">{showAll ? '−' : '+'}</span>
            {showAll ? 'Mostrar menos' : 'Mostrar más'}
          </button>
        </div>
      </nav>
    </aside>
  );
}
