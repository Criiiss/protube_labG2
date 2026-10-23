import { useEffect, useState } from 'react';
export interface Library {
  saved: string[];
  later: string[];
  liked: string[];
  hidden: string[];
  history: string[];
  subscriptions: string[];
}
const empty: Library = { saved: [], later: [], liked: [], hidden: [], history: [], subscriptions: [] };
function read(key: string): Library {
  try {
    const data = JSON.parse(localStorage.getItem(key) || '{}');
    return Object.fromEntries(
      Object.keys(empty).map((name) => [
        name,
        Array.isArray(data[name]) ? data[name].filter((value: unknown) => typeof value === 'string') : [],
      ])
    ) as unknown as Library;
  } catch {
    return { ...empty };
  }
}
export function useLibrary(userId?: number) {
  const key = `protube-library-${userId || 'guest'}`;
  const [state, setState] = useState(() => ({ key, library: read(key) }));
  const library = state.key === key ? state.library : read(key);
  useEffect(() => {
    if (state.key === key) {
      try {
        localStorage.setItem(key, JSON.stringify(state.library));
      } catch {
        /* Storage may be unavailable in private sessions. */
      }
    }
  }, [key, state]);
  function toggle(collection: keyof Library, id: string) {
    setState((current) => {
      const previous = current.key === key ? current.library : read(key);
      return {
        key,
        library: {
          ...previous,
          [collection]: previous[collection].includes(id)
            ? previous[collection].filter((item) => item !== id)
            : [...previous[collection], id],
        },
      };
    });
  }
  function visit(id: string) {
    setState((current) => {
      const previous = current.key === key ? current.library : read(key);
      return {
        key,
        library: { ...previous, history: [id, ...previous.history.filter((item) => item !== id)].slice(0, 200) },
      };
    });
  }
  return { library, toggle, visit };
}
