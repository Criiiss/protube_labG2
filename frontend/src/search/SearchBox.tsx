import { useEffect, useRef, useState } from 'react';
import { Icon } from '../components/home/Icon';
import { clearHistory, forget, readHistory, remember } from './history';
import { suggest } from './searchApi';

interface SearchBoxProps {
  onSearch: (query: string) => void;
  value?: string;
}

export function SearchBox({ onSearch, value = '' }: SearchBoxProps) {
  const [query, setQuery] = useState(value);
  const [open, setOpen] = useState(false);
  const [pinned, setPinned] = useState(false);
  const [history, setHistory] = useState(readHistory);
  const [suggestions, setSuggestions] = useState<string[]>([]);
  const [active, setActive] = useState(-1);
  const input = useRef<HTMLInputElement>(null);

  useEffect(() => setQuery(value), [value]);
  useEffect(() => {
    const text = query.trim();
    if (!text) {
      setSuggestions([]);
      return;
    }
    let alive = true;
    const timer = setTimeout(() => {
      suggest(text)
        .then((items) => alive && setSuggestions(items))
        .catch(() => alive && setSuggestions([])); // suggestions are optional; search still works
    }, 200);
    return () => {
      alive = false;
      clearTimeout(timer);
    };
  }, [query]);

  const showingHistory = !query.trim();
  const items = showingHistory ? history : suggestions;
  const expanded = open && items.length > 0;

  function submit(text: string) {
    setQuery(text);
    setOpen(false);
    setActive(-1);
    if (text.trim()) setHistory(remember(text));
    input.current?.blur();
    onSearch(text.trim());
  }

  return (
    <form
      className={`search-shell ${pinned || query ? 'is-open' : ''}`}
      role="search"
      onSubmit={(event) => {
        event.preventDefault();
        submit(active >= 0 && expanded ? items[active] : query);
      }}
      onFocus={() => {
        setPinned(true);
        setOpen(true);
      }}
      onBlur={(event) => {
        if (!event.currentTarget.contains(event.relatedTarget as Node | null)) {
          setOpen(false);
          setActive(-1);
        }
      }}
      onClick={() => setPinned(true)}
    >
      <input
        ref={input}
        type="search"
        role="combobox"
        aria-label="Buscar vídeos y canales"
        aria-autocomplete="list"
        aria-expanded={expanded}
        aria-controls="search-options"
        aria-activedescendant={expanded && active >= 0 ? `search-option-${active}` : undefined}
        placeholder="Busca algo que te inspire"
        value={query}
        maxLength={100}
        onChange={(event) => {
          setQuery(event.target.value);
          setOpen(true);
          setActive(-1);
        }}
        onKeyDown={(event) => {
          if (event.key === 'ArrowDown' && items.length) {
            event.preventDefault();
            setOpen(true);
            setActive((active + 1) % items.length);
          } else if (event.key === 'ArrowUp' && items.length) {
            event.preventDefault();
            setActive(active <= 0 ? items.length - 1 : active - 1);
          } else if (event.key === 'Escape') {
            setOpen(false);
            setActive(-1);
          }
        }}
      />
      {query && (
        <button
          type="button"
          className="search-clear"
          aria-label="Borrar texto"
          onClick={() => {
            setQuery('');
            setActive(-1);
            input.current?.focus();
          }}
        >
          ×
        </button>
      )}
      <button type="submit" className="search-button" aria-label="Buscar">
        <Icon name="search" />
      </button>
      {expanded && (
        <div className="search-dropdown">
          {showingHistory && (
            <div className="search-dropdown-head">
              <span>Búsquedas recientes</span>
              <button
                type="button"
                onMouseDown={(event) => event.preventDefault()}
                onClick={() => setHistory(clearHistory())}
              >
                Borrar historial
              </button>
            </div>
          )}
          <ul id="search-options" role="listbox" aria-label={showingHistory ? 'Búsquedas recientes' : 'Sugerencias'}>
            {items.map((item, index) => (
              <li
                key={item}
                id={`search-option-${index}`}
                role="option"
                aria-selected={index === active}
                className={index === active ? 'is-active' : ''}
                onMouseDown={(event) => event.preventDefault()}
                onClick={() => submit(item)}
              >
                <Icon name={showingHistory ? 'history' : 'search'} />
                <span>{item}</span>
                {showingHistory && (
                  <button
                    type="button"
                    className="search-forget"
                    aria-label={`Quitar «${item}» del historial`}
                    onClick={(event) => {
                      event.stopPropagation();
                      setHistory(forget(item));
                    }}
                  >
                    ×
                  </button>
                )}
              </li>
            ))}
          </ul>
        </div>
      )}
    </form>
  );
}
