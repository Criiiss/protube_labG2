import { getMyChannelVideos, getPublicChannelVideos } from '../channelVideosApi.ts';
import { getEnv } from '../../utils/Env.ts';

jest.mock('../../utils/Env.ts', () => {
  const env = { API_BASE_URL: 'http://api.test/api', BASE_URL: '/', PROFILE_DEMO: false };
  return { getEnv: () => env };
});

const env = getEnv();

describe('channelVideosApi', () => {
  beforeEach(() => {
    fetchMock.resetMocks();
    env.API_BASE_URL = 'http://api.test/api';
  });

  it('loads public channel videos', async () => {
    fetchMock.mockResponseOnce(JSON.stringify([{ id: '1' }]));
    await expect(getPublicChannelVideos(4)).resolves.toEqual([{ id: '1' }]);
    expect(fetchMock).toHaveBeenCalledWith('http://api.test/api/profiles/4/videos', expect.anything());
  });

  it.each([
    ['scheduled', 'SCHEDULED'],
    ['archived', 'ARCHIVED'],
    ['drafts', 'DRAFT'],
  ] as const)('loads %s videos with its backend status', async (tab, status) => {
    fetchMock.mockResponseOnce(JSON.stringify([]));
    await getMyChannelVideos(4, tab);
    expect(fetchMock.mock.calls[0][0]).toContain(`userId=4&status=${status}`);
  });

  it('works with the relative API URL of the Docker build', async () => {
    env.API_BASE_URL = '/api';
    fetchMock.mockResponseOnce(JSON.stringify([]));
    await expect(getMyChannelVideos(4, 'drafts')).resolves.toEqual([]);
    expect(fetchMock.mock.calls[0][0]).toBe('/api/profiles/me/videos?userId=4&status=DRAFT');
  });

  it('returns the server error message', async () => {
    fetchMock.mockResponseOnce(JSON.stringify({ message: 'Error de vídeos' }), { status: 500 });
    await expect(getPublicChannelVideos(4)).rejects.toThrow('Error de vídeos');
  });
});
