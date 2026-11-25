jest.mock('../channelVideosApi.ts', () => ({
  getPublicChannelVideos: jest.fn(),
  getMyChannelVideos: jest.fn(),
}));

import { getMyChannelVideos, getPublicChannelVideos } from '../channelVideosApi.ts';

describe('channel video API', () => {
  beforeEach(() => jest.clearAllMocks());

  it('exposes public video results', async () => {
    (getPublicChannelVideos as jest.Mock).mockResolvedValue([{ id: 'v1' }]);
    await expect(getPublicChannelVideos(3)).resolves.toEqual([{ id: 'v1' }]);
    expect(getPublicChannelVideos).toHaveBeenCalledWith(3);
  });

  it.each([
    ['scheduled', 'SCHEDULED'],
    ['archived', 'ARCHIVED'],
    ['drafts', 'DRAFT'],
  ] as const)('loads owner %s videos', async (tab, status) => {
    (getMyChannelVideos as jest.Mock).mockResolvedValue([]);
    await expect(getMyChannelVideos(3, tab)).resolves.toEqual([]);
    expect(getMyChannelVideos).toHaveBeenCalledWith(3, tab);
    expect(status).toBeTruthy();
  });

  it('reports server errors and invalid JSON', async () => {
    (getPublicChannelVideos as jest.Mock).mockRejectedValueOnce(new Error('Nope'));
    await expect(getPublicChannelVideos(1)).rejects.toThrow('Nope');
    (getPublicChannelVideos as jest.Mock).mockRejectedValueOnce(new Error('No se pudieron cargar los vídeos.'));
    await expect(getPublicChannelVideos(1)).rejects.toThrow('No se pudieron cargar los vídeos.');
  });
});
