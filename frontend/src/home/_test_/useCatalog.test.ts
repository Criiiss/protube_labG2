import { act, renderHook } from '@testing-library/react';
import { api } from '../../auth/api.ts';
import { useCatalog } from '../useCatalog.ts';

jest.mock('../../auth/api.ts', () => ({ api: jest.fn() }));

describe('useCatalog', () => {
  beforeEach(() => jest.clearAllMocks());

  it('loads catalog data', async () => {
    (api as jest.Mock).mockResolvedValue([{ id: 'v1' }]);
    const { result } = renderHook(() => useCatalog());
    await act(async () => {});
    expect(result.current.videos).toEqual([{ id: 'v1' }]);
    expect(result.current.loading).toBe(false);
    expect(result.current.error).toBe('');
  });

  it('reports loading errors', async () => {
    (api as jest.Mock).mockRejectedValue(new Error('offline'));
    const { result } = renderHook(() => useCatalog());
    await act(async () => {});
    expect(result.current.loading).toBe(false);
    expect(result.current.error).toContain('No se pudo cargar');
  });

  it('does not update after unmount', async () => {
    let resolve!: (value: unknown) => void;
    (api as jest.Mock).mockReturnValue(
      new Promise((r) => {
        resolve = r;
      })
    );
    const { result, unmount } = renderHook(() => useCatalog());
    unmount();
    await act(async () => resolve([{ id: 'late' }]));
    expect(result.current.videos).toEqual([]);
  });
});
