import {
  changeUsername,
  getMyProfile,
  getPublicProfile,
  removeAvatar,
  removeBanner,
  updatePrivacy,
  updateProfile,
} from '../profileApi.ts';

const profile = {
  userId: 1,
  username: 'user',
  displayName: 'User',
  biography: null,
  avatarUrl: null,
  bannerUrl: null,
  subscriptionsVisibility: 'PUBLIC' as const,
  likedVideosVisibility: 'PRIVATE' as const,
  createdAt: '2024-01-01',
};

describe('profileApi', () => {
  beforeEach(() => {
    fetchMock.resetMocks();
  });

  it.each([
    ['getMyProfile', () => getMyProfile(1), 'GET', 'other/profiles/me?userId=1'],
    ['getPublicProfile', () => getPublicProfile('a user'), 'GET', 'other/profiles/a%20user'],
    ['updateProfile', () => updateProfile(1, { displayName: 'New' }), 'PATCH', 'other/profiles/me?userId=1'],
    [
      'changeUsername',
      () => changeUsername(1, { username: 'new_user' }),
      'PATCH',
      'other/profiles/me/username?userId=1',
    ],
    ['removeAvatar', () => removeAvatar(1), 'DELETE', 'other/profiles/me/avatar?userId=1'],
    ['removeBanner', () => removeBanner(1), 'DELETE', 'other/profiles/me/banner?userId=1'],
    [
      'updatePrivacy',
      () => updatePrivacy(1, { subscriptionsVisibility: 'PRIVATE' }),
      'PATCH',
      'other/profiles/me/privacy?userId=1',
    ],
  ])('%s sends the expected request', async (_name, operation, method, url) => {
    fetchMock.mockResponseOnce(JSON.stringify(profile));
    await operation();
    const options = fetchMock.mock.calls[0][1] as RequestInit;
    expect(fetchMock.mock.calls[0][0]).toBe(url);
    expect(options.credentials).toBe('include');
    if (method !== 'GET') expect(options.method).toBe(method);
  });

  it('returns the server error message', async () => {
    fetchMock.mockResponseOnce(JSON.stringify({ message: 'No autorizado' }), { status: 403 });
    await expect(getMyProfile(1)).rejects.toThrow('No autorizado');
  });

  it('handles an invalid JSON error response', async () => {
    fetchMock.mockResponseOnce('not-json', { status: 500 });
    await expect(getPublicProfile('user')).rejects.toThrow('No se pudo completar la solicitud.');
  });
});
