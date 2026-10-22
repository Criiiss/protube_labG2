import { useCallback, useEffect, useState } from 'react';
import { api, ApiError } from './api';
import type { SessionUser } from './api';

export function useSession() {
  const [user, setUser] = useState<SessionUser | null>(null);
  const [loading, setLoading] = useState(true);
  const refresh = useCallback(async () => {
    try {
      setUser(await api<SessionUser>('/auth/session'));
    } catch (error) {
      if (error instanceof ApiError && error.status === 401) setUser(null);
    } finally {
      setLoading(false);
    }
  }, []);
  useEffect(() => {
    void refresh();
    const timer = window.setInterval(() => void refresh(), 60000);
    window.addEventListener('focus', refresh);
    return () => {
      clearInterval(timer);
      window.removeEventListener('focus', refresh);
    };
  }, [refresh]);
  return {
    user,
    setUser,
    loading,
    refresh,
    logout: async () => {
      await api('/auth/logout', {});
      setUser(null);
      window.location.hash = '/';
    },
  };
}
