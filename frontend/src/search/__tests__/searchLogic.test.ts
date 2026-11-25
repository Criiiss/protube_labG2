import fetchMock from 'jest-fetch-mock';
import { clearHistory, forget, readHistory, remember } from '../history';
import { defaultFilters, filtersFromParams, search, searchHash, suggest, toQueryString } from '../searchApi';

beforeEach(() => {
  localStorage.clear();
  fetchMock.resetMocks();
});

describe('search history', () => {
  it('keeps the newest searches first, without duplicates and up to 8', () => {
    for (let i = 1; i <= 9; i++) remember(`busqueda ${i}`);
    remember('  BUSQUEDA 5 ');
    const history = readHistory();
    expect(history).toHaveLength(8);
    expect(history[0]).toBe('BUSQUEDA 5');
    expect(history.filter((item) => item.toLowerCase() === 'busqueda 5')).toHaveLength(1);
    expect(history).not.toContain('busqueda 1');
  });

  it('ignores blank searches, forgets one entry and clears everything', () => {
    remember('paella');
    remember('jazz');
    expect(remember('   ')).toEqual(['jazz', 'paella']);
    expect(forget('jazz')).toEqual(['paella']);
    expect(clearHistory()).toEqual([]);
    expect(readHistory()).toEqual([]);
  });

  it('survives corrupted storage', () => {
    localStorage.setItem('protube-search-history', '{not json');
    expect(readHistory()).toEqual([]);
    localStorage.setItem('protube-search-history', '{"a":1}');
    expect(readHistory()).toEqual([]);
  });
});

describe('search api', () => {
  it('reads filters from the URL and only writes non-default ones back', () => {
    const filters = filtersFromParams(new URLSearchParams('q=jazz&sort=views&unknown=1'));
    expect(filters).toEqual({ ...defaultFilters, q: 'jazz', sort: 'views' });
    expect(toQueryString(filters)).toBe('q=jazz&sort=views');
    expect(searchHash(defaultFilters)).toBe('/search');
    expect(searchHash({ ...defaultFilters, channel: 'Blue Sessions' })).toBe('/search?channel=Blue+Sessions');
  });

  it('calls the backend with filters and page', async () => {
    fetchMock.mockResponseOnce(JSON.stringify({ videos: [] }));
    await search({ ...defaultFilters, q: 'paella', type: 'video' }, 2);
    expect(fetchMock.mock.calls[0][0]).toBe('other/search?q=paella&type=video&page=2&size=12');
    fetchMock.mockResponseOnce(JSON.stringify({ videos: [] }));
    await search(defaultFilters, 0, 6);
    expect(fetchMock.mock.calls[1][0]).toBe('other/search?page=0&size=6');
  });

  it('asks for suggestions with the text encoded', async () => {
    fetchMock.mockResponseOnce(JSON.stringify(['Música']));
    await expect(suggest('mú sica')).resolves.toEqual(['Música']);
    expect(fetchMock.mock.calls[0][0]).toBe('other/search/suggest?q=m%C3%BA%20sica');
  });
});
