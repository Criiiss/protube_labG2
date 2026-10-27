import { getEnv } from '../utils/Env';
import type { ChangeUsernameRequest, MyProfile, PublicProfile, UpdateProfileRequest } from './types';

const DEMO_PROFILE_KEY = 'protube-demo-profile';

const DEFAULT_DEMO_PROFILE: MyProfile = {
  userId: 1,
  username: 'protube_creator',
  displayName: 'ProTube Creator',
  biography: 'Bienvenido a mi canal. Aquí comparto vídeos, tutoriales y contenido para la comunidad de ProTube.',
  avatarUrl: null,
  bannerUrl: null,
  subscriptionsVisibility: 'PUBLIC',
  likedVideosVisibility: 'PRIVATE',
  createdAt: '2025-03-15T10:30:00Z',
};

const DEMO_PUBLIC_PROFILE: PublicProfile = {
  userId: 2,
  username: 'otro_creador',
  displayName: 'Otro creador',
  biography: 'Este es un perfil público de prueba.',
  avatarUrl: null,
  bannerUrl: null,
  createdAt: '2024-09-20T12:00:00Z',
};

const isDemoMode = (): boolean => import.meta.env.VITE_PROFILE_DEMO === 'true';

const getDemoProfile = (): MyProfile => {
  const storedProfile = localStorage.getItem(DEMO_PROFILE_KEY);

  if (!storedProfile) {
    localStorage.setItem(DEMO_PROFILE_KEY, JSON.stringify(DEFAULT_DEMO_PROFILE));

    return DEFAULT_DEMO_PROFILE;
  }

  return JSON.parse(storedProfile) as MyProfile;
};

const saveDemoProfile = (profile: MyProfile): MyProfile => {
  localStorage.setItem(DEMO_PROFILE_KEY, JSON.stringify(profile));
  return profile;
};

const getProfileUrl = (path: string): string => `${getEnv().API_BASE_URL}/profiles${path}`;

const request = async <T>(url: string, options?: RequestInit): Promise<T> => {
  const response = await fetch(url, {
    headers: {
      'Content-Type': 'application/json',
      ...options?.headers,
    },
    ...options,
  });

  if (!response.ok) {
    const errorMessage = await response.text();
    throw new Error(errorMessage || `Request failed with status ${response.status}`);
  }

  return response.json() as Promise<T>;
};

export const getMyProfile = (userId: number): Promise<MyProfile> => {
  if (isDemoMode()) {
    return Promise.resolve({
      ...getDemoProfile(),
      userId,
    });
  }

  return request<MyProfile>(getProfileUrl(`/me?userId=${userId}`));
};

export const getPublicProfile = (username: string): Promise<PublicProfile> => {
  if (isDemoMode()) {
    return Promise.resolve({
      ...DEMO_PUBLIC_PROFILE,
      username,
    });
  }

  return request<PublicProfile>(getProfileUrl(`/${encodeURIComponent(username)}`));
};

export const updateProfile = (userId: number, profile: UpdateProfileRequest): Promise<MyProfile> => {
  if (isDemoMode()) {
    const updatedProfile = {
      ...getDemoProfile(),
      ...profile,
      userId,
    };

    return Promise.resolve(saveDemoProfile(updatedProfile));
  }

  return request<MyProfile>(getProfileUrl(`/me?userId=${userId}`), {
    method: 'PATCH',
    body: JSON.stringify(profile),
  });
};

export const changeUsername = (userId: number, username: ChangeUsernameRequest): Promise<MyProfile> => {
  if (isDemoMode()) {
    const updatedProfile = {
      ...getDemoProfile(),
      ...username,
      userId,
    };

    return Promise.resolve(saveDemoProfile(updatedProfile));
  }

  return request<MyProfile>(getProfileUrl(`/me/username?userId=${userId}`), {
    method: 'PATCH',
    body: JSON.stringify(username),
  });
};

export const removeAvatar = (userId: number): Promise<MyProfile> => {
  if (isDemoMode()) {
    return Promise.resolve(
      saveDemoProfile({
        ...getDemoProfile(),
        userId,
        avatarUrl: null,
      })
    );
  }

  return request<MyProfile>(getProfileUrl(`/me/avatar?userId=${userId}`), {
    method: 'DELETE',
  });
};

export const removeBanner = (userId: number): Promise<MyProfile> => {
  if (isDemoMode()) {
    return Promise.resolve(
      saveDemoProfile({
        ...getDemoProfile(),
        userId,
        bannerUrl: null,
      })
    );
  }

  return request<MyProfile>(getProfileUrl(`/me/banner?userId=${userId}`), {
    method: 'DELETE',
  });
};