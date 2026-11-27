import { act, renderHook } from '@testing-library/react';
import * as profileApi from '../profileApi.ts';
import { useProfile } from '../useProfile.ts';

jest.mock('../profileApi.ts', () => ({
  getMyProfile: jest.fn(),
  getPublicProfile: jest.fn(),
  updateProfile: jest.fn(),
  changeUsername: jest.fn(),
  removeAvatar: jest.fn(),
  removeBanner: jest.fn(),
}));

describe('useProfile', () => {
  const profile = {
    userId: 1,
    username: 'user',
    displayName: 'User',
    biography: null,
    avatarUrl: null,
    bannerUrl: null,
    subscriptionsVisibility: 'PRIVATE' as const,
    likedVideosVisibility: 'PRIVATE' as const,
    createdAt: '2026-01-01',
  };

  beforeEach(() => {
    jest.clearAllMocks();
    (profileApi.getMyProfile as jest.Mock).mockResolvedValue(profile);
    (profileApi.getPublicProfile as jest.Mock).mockResolvedValue(profile);
  });

  it('loads an owned profile and saves changes', async () => {
    const { result } = renderHook(() => useProfile({ userId: 1 }));
    await act(async () => {});
    expect(result.current.profile).toEqual(profile);
    (profileApi.updateProfile as jest.Mock).mockResolvedValue({ ...profile, displayName: 'New' });
    await act(async () =>
      result.current.saveProfile({ displayName: 'New', biography: null, avatarUrl: null, bannerUrl: null })
    );
    expect(result.current.profile?.displayName).toBe('New');
  });

  it('loads public profiles and rejects owner-only operations', async () => {
    const { result } = renderHook(() => useProfile({ username: 'public' }));
    await act(async () => {});
    expect(result.current.isOwner).toBe(false);
    await expect(result.current.saveUsername({ username: 'x' })).rejects.toThrow('propietario');
    await expect(result.current.deleteAvatar()).rejects.toThrow('propietario');
  });

  it('reports missing profile parameters and API failures', async () => {
    const missing = renderHook(() => useProfile({}));
    await act(async () => {});
    expect(missing.result.current.error).toContain('No se ha indicado');
    (profileApi.getMyProfile as jest.Mock).mockRejectedValue(new Error('failed'));
    const failed = renderHook(() => useProfile({ userId: 2 }));
    await act(async () => {});
    expect(failed.result.current.error).toBe('failed');
  });
});
