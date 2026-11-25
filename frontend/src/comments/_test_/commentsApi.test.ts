import {
  createComment, deleteVideoComment, getVideoComments, likeVideoComment, unlikeVideoComment, updateVideoComment,
} from '../commentsApi.ts';

describe('comments API', () => {
  beforeEach(() => {
    jest.restoreAllMocks();
  });

  it('builds create and list requests', async () => {
    const api = jest.spyOn(require('../../auth/api.ts'), 'api').mockResolvedValue([]);
    await getVideoComments('a/b');
    expect(api).toHaveBeenCalledWith('/videos/a%2Fb/comments');
    await createComment('a/b', 'hello', 4);
    expect(api).toHaveBeenLastCalledWith('/videos/a%2Fb/comments', { content: 'hello', parentId: 4 }, 'POST');
  });

  it('builds update, delete, like and unlike requests', async () => {
    const api = jest.spyOn(require('../../auth/api.ts'), 'api').mockResolvedValue({});
    await updateVideoComment('v', 2, 'new');
    expect(api).toHaveBeenLastCalledWith('/videos/v/comments/2', { content: 'new' }, 'PATCH');
    await deleteVideoComment('v', 2);
    expect(api).toHaveBeenLastCalledWith('/videos/v/comments/2', undefined, 'DELETE');
    await likeVideoComment('v', 2);
    expect(api).toHaveBeenLastCalledWith('/videos/v/comments/2/like', undefined, 'PUT');
    await unlikeVideoComment('v', 2);
    expect(api).toHaveBeenLastCalledWith('/videos/v/comments/2/like', undefined, 'DELETE');
  });
});
