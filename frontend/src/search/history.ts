const KEY = 'protube-search-history';
const MAX = 8;

export function readHistory(): string[] {
  try {
    const data = JSON.parse(localStorage.getItem(KEY) || '[]');
    return Array.isArray(data) ? data.filter((item) => typeof item === 'string').slice(0, MAX) : [];
  } catch {
    return [];
  }
}

function save(items: string[]): string[] {
  try {
    localStorage.setItem(KEY, JSON.stringify(items));
  } catch {
    /* Storage may be unavailable in private sessions. */
  }
  return items;
}

/** Newest first, without case-insensitive duplicates. */
export function remember(query: string): string[] {
  const value = query.trim();
  if (!value) return readHistory();
  const rest = readHistory().filter((item) => item.toLocaleLowerCase('es') !== value.toLocaleLowerCase('es'));
  return save([value, ...rest].slice(0, MAX));
}

export function forget(query: string): string[] {
  return save(readHistory().filter((item) => item !== query));
}

export function clearHistory(): string[] {
  return save([]);
}
