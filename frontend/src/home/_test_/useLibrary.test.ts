import { act, renderHook } from '@testing-library/react';
import { useLibrary } from '../useLibrary.ts';

describe('useLibrary', () => {
  beforeEach(() => localStorage.clear());

  it('reads valid collections and filters invalid values', () => {
    localStorage.setItem(
      'protube-library-2',
      JSON.stringify({
        saved: ['a', 1],
        later: ['b'],
        liked: null,
        hidden: [],
        history: ['c'],
        subscriptions: ['channel'],
      })
    );
    const { result } = renderHook(() => useLibrary(2));
    expect(result.current.library).toEqual({
      saved: ['a'],
      later: ['b'],
      liked: [],
      hidden: [],
      history: ['c'],
      subscriptions: ['channel'],
    });
  });

  it('recovers from invalid storage and toggles collections', () => {
    localStorage.setItem('protube-library-guest', '{');
    const { result } = renderHook(() => useLibrary());
    expect(result.current.library.saved).toEqual([]);
    act(() => result.current.toggle('saved', 'v1'));
    expect(result.current.library.saved).toEqual(['v1']);
    act(() => result.current.toggle('saved', 'v1'));
    expect(result.current.library.saved).toEqual([]);
  });

  it('keeps history unique and capped at 200 items', () => {
    localStorage.setItem(
      'protube-library-1',
      JSON.stringify({
        saved: [],
        later: [],
        liked: [],
        hidden: [],
        history: Array.from({ length: 200 }, (_, i) => `v${i}`),
        subscriptions: [],
      })
    );
    const { result } = renderHook(() => useLibrary(1));
    act(() => result.current.visit('v1'));
    expect(result.current.library.history[0]).toBe('v1');
    expect(result.current.library.history).toHaveLength(200);
    expect(result.current.library.history.filter((id) => id === 'v1')).toHaveLength(1);
  });

  it('switches storage keys when the user changes', () => {
    const { result, rerender } = renderHook(({ id }) => useLibrary(id), {
      initialProps: { id: 1 as number | undefined },
    });
    act(() => result.current.toggle('liked', 'v1'));
    rerender({ id: 2 });
    expect(result.current.library.liked).toEqual([]);
    act(() => result.current.toggle('liked', 'v2'));
    expect(JSON.parse(localStorage.getItem('protube-library-2') || '{}').liked).toEqual(['v2']);
  });
});
