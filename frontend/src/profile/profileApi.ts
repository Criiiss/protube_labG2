import { getEnv } from '../utils/Env';
import type { ChangeUsernameRequest, MyProfile, PublicProfile, UpdateProfileRequest } from './types';

const getProfileUrl = (path: string) => `${getEnv().API_BASE_URL}/profiles${path}`;
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

export const getMyProfile = (userId: number): Promise<MyProfile> =>
  request<MyProfile>(getProfileUrl(`/me?userId=${userId}`));
export const getPublicProfile = (username: string): Promise<PublicProfile> =>
  request<PublicProfile>(getProfileUrl(`/${encodeURIComponent(username)}`));
export const updateProfile = (userId: number, profile: UpdateProfileRequest): Promise<MyProfile> =>
  request<MyProfile>(getProfileUrl(`/me?userId=${userId}`), { method: 'PATCH', body: JSON.stringify(profile) });
export const changeUsername = (userId: number, username: ChangeUsernameRequest): Promise<MyProfile> =>
  request<MyProfile>(getProfileUrl(`/me/username?userId=${userId}`), {
    method: 'PATCH',
    body: JSON.stringify(username),
  });
export const removeAvatar = (userId: number): Promise<MyProfile> =>
  request<MyProfile>(getProfileUrl(`/me/avatar?userId=${userId}`), { method: 'DELETE' });
export const removeBanner = (userId: number): Promise<MyProfile> =>
  request<MyProfile>(getProfileUrl(`/me/banner?userId=${userId}`), { method: 'DELETE' });
