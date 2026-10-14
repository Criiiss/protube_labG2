import { useRef, useState } from 'react';
import { Icon } from './Icon';

interface HeaderProps {
  onSearch: (query: string) => void;
  onAction: (message: string) => void;
}

export function Header({ onSearch, onAction }: HeaderProps) {
  const [query, setQuery] = useState('');
  const [pinned, setPinned] = useState(false);
  const input = useRef<HTMLInputElement>(null);
  return (
    <header className="site-header">
      <a className="brand" href={import.meta.env.BASE_URL} aria-label="protube: volver al inicio y refrescar">
        <img src={`${import.meta.env.BASE_URL}protube-imago-removebg-preview.png`} alt="" />
        <span>
          protube<span className="brand-dot">.</span>
        </span>
      </a>
      <div className="search-zone">
        <form
          className={`search-shell ${pinned || query ? 'is-open' : ''}`}
          role="search"
          onSubmit={(event) => {
            event.preventDefault();
            setPinned(true);
            input.current?.focus();
            onSearch(query.trim());
          }}
          onFocus={() => setPinned(true)}
          onClick={() => setPinned(true)}
        >
          <input
            ref={input}
            type="search"
            aria-label="Buscar vídeos"
            placeholder="Busca algo que te inspire"
            value={query}
            onChange={(event) => {
              setQuery(event.target.value);
              if (!event.target.value) onSearch('');
            }}
          />
          <button type="submit" className="search-button" aria-label="Buscar">
            <Icon name="search" />
          </button>
        </form>
      </div>
      <div className="header-actions">
        <button
          className="icon-button"
          aria-label="Subir contenido"
          onClick={() => onAction('La subida de contenido estará disponible próximamente.')}
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
        <button
          className="avatar"
          aria-label="Perfil de usuario"
          onClick={() => onAction('El perfil estará disponible al conectar tu cuenta.')}
        >
          <Icon name="user" />
        </button>
      </div>
    </header>
  );
}
