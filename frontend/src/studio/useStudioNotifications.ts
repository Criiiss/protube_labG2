import { useEffect, useRef, useState } from 'react';
import { studioList, statusText } from './api';
import type { Item } from './api';

export function useStudioNotifications(userId: number | undefined, notify: (message: string) => void) {
  const [latest, setLatest] = useState<{ owner: number; items: Item[] }>();
  const seen = useRef(new Set<string>());
  useEffect(() => {
    if (!userId) return;
    let active = true;
    const refresh = async () => {
      try {
        const items = await studioList();
        if (!active) return;
        const finished = items
          .filter((item) => item.finishedAt && ['READY', 'ERROR'].includes(item.status))
          .sort((a, b) => (b.finishedAt || '').localeCompare(a.finishedAt || ''));
        const recent = finished.find(
          (item) => !seen.current.has(item.id) && Date.now() - Date.parse(item.finishedAt!) < 30000
        );
        finished.forEach((item) => seen.current.add(item.id));
        if (recent)
          notify(
            `${recent.metadata.title}: ${statusText[recent.status]}. ${recent.failure || 'Ya disponible en Studio.'}`
          );
        setLatest({ owner: userId, items: finished });
      } catch {
        /* The next poll will retry. */
      }
    };
    void refresh();
    const timer = window.setInterval(() => void refresh(), 5000);
    return () => {
      active = false;
      window.clearInterval(timer);
    };
  }, [userId, notify]);
  return () =>
    notify(
      latest && latest.owner === userId && latest.items.length
        ? latest.items
            .slice(0, 3)
            .map((item) => `${item.metadata.title}: ${statusText[item.status]}`)
            .join(' · ')
        : 'No tienes avisos de procesamiento.'
    );
}
