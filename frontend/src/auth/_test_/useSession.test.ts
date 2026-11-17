import { act, renderHook } from '@testing-library/react';
import { api, ApiError } from '../api.ts';
import { useSession } from '../useSession.ts';

jest.mock('../api.ts', () => ({
  api: jest.fn(),
  ApiError: class ApiError extends Error {
    status: number;
    constructor(message: string, status: number) {
      super(message);
      this.status = status;
    }
  },
}));

describe('useSession', () => {
  const user = { id: 1, username: 'user', displayName: 'User', avatarUrl: null };

  beforeEach(() => {
    jest.clearAllMocks();
    jest.useFakeTimers();
    window.location.hash = '';
  });

  afterEach(() => jest.useRealTimers());

  it('loads the current session and refreshes on focus', async () => {
    (api as jest.Mock).mockResolvedValue(user);
    const { result } = renderHook(() => useSession());
    await act(async () => {});
    expect(result.current.user).toEqual(user);
    expect(result.current.loading).toBe(false);
    act(() => window.dispatchEvent(new Event('focus')));
    expect(api).toHaveBeenCalledWith('/auth/session');
  });

  it('clears the user for an unauthorized session', async () => {
    (api as jest.Mock).mockRejectedValue(new ApiError('unauthorized', 401));
    const { result } = renderHook(() => useSession());
    await act(async () => {});
    expect(result.current.user).toBeNull();
    expect(result.current.loading).toBe(false);
  });

  it('keeps state on non-401 errors and logs out', async () => {
    (api as jest.Mock).mockRejectedValueOnce(new Error('server'));
    const { result } = renderHook(() => useSession());
    await act(async () => {});
    expect(result.current.loading).toBe(false);
    (api as jest.Mock).mockResolvedValueOnce({});
    await act(async () => result.current.logout());
    expect(result.current.user).toBeNull();
    expect(window.location.hash).toBe('#/');
    expect(api).toHaveBeenLastCalledWith('/auth/logout', {});
  });

  it('refreshes periodically and cleans up listeners', async () => {
    (api as jest.Mock).mockResolvedValue(user);
    const { unmount } = renderHook(() => useSession());
    await act(async () => {});
    const calls = (api as jest.Mock).mock.calls.length;
    act(() => jest.advanceTimersByTime(60000));
    expect((api as jest.Mock).mock.calls.length).toBeGreaterThan(calls);
    unmount();
    const afterUnmount = (api as jest.Mock).mock.calls.length;
    act(() => window.dispatchEvent(new Event('focus')));
    expect((api as jest.Mock).mock.calls.length).toBe(afterUnmount);
  });
});
