import { api } from '../../auth/api.ts';
import {
  createComment,
  deleteVideoComment,
  getVideoComments,
  likeVideoComment,
  unlikeVideoComment,
  updateVideoComment,
} from '../commentsApi.ts';

jest.mock('../../auth/api.ts', () => ({ api: jest.fn() }));
const mockedApi = api as jest.Mock;

describe('comments API', () => {
  beforeEach(() => {
    mockedApi.mockReset();
  });

  it('builds create and list requests', async () => {
    mockedApi.mockResolvedValue([]);
    await getVideoComments('a/b');
    expect(mockedApi).toHaveBeenCalledWith('/videos/a%2Fb/comments');
    await createComment('a/b', 'hello', 4);
    expect(mockedApi).toHaveBeenLastCalledWith('/videos/a%2Fb/comments', { content: 'hello', parentId: 4 }, 'POST');
  });

  it('builds update, delete, like and unlike requests', async () => {
    mockedApi.mockResolvedValue({});
    await updateVideoComment('v', 2, 'new');
    expect(mockedApi).toHaveBeenLastCalledWith('/videos/v/comments/2', { content: 'new' }, 'PATCH');
    await deleteVideoComment('v', 2);
    expect(mockedApi).toHaveBeenLastCalledWith('/videos/v/comments/2', undefined, 'DELETE');
    await likeVideoComment('v', 2);
    expect(mockedApi).toHaveBeenLastCalledWith('/videos/v/comments/2/like', undefined, 'PUT');
    await unlikeVideoComment('v', 2);
    expect(mockedApi).toHaveBeenLastCalledWith('/videos/v/comments/2/like', undefined, 'DELETE');
  });
});
