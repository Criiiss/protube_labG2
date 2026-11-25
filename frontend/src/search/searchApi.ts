import { api } from '../auth/api';
import type { Video } from '../data/home';

export interface SearchFilters {
  q: string;
  type: string; // all | video | channel | playlist
  date: string; // any | today | week | month | year
  duration: string; // any | short | medium | long
  sort: string; // relevance | date | views
  channel: string;
}
export interface ChannelHit {
  name: string;
  videoCount: number;
  viewCount: number;
}
export interface PlaylistHit {
  id: string;
  title: string;
  owner: string;
  videoCount: number;
}
export interface SearchResult {
  query: string;
  videos: Video[];
  videoTotal: number;
  channels: ChannelHit[];
  channelTotal: number;
  playlists: PlaylistHit[];
  playlistTotal: number;
  page: number;
  hasMore: boolean;
  approximate: boolean;
  correctedQuery: string | null; // set when nothing matched and results are for this spelling instead
}

export const defaultFilters: SearchFilters = {
  q: '',
  type: 'all',
  date: 'any',
  duration: 'any',
  sort: 'relevance',
  channel: '',
};

export function filtersFromParams(params: URLSearchParams): SearchFilters {
  const filters = { ...defaultFilters };
  for (const key of Object.keys(defaultFilters) as (keyof SearchFilters)[])
    filters[key] = params.get(key) ?? filters[key];
  return filters;
}

/** Only non-default values go in the URL, so links stay short and shareable. */
export function toQueryString(filters: SearchFilters): string {
  const params = new URLSearchParams();
  for (const key of Object.keys(defaultFilters) as (keyof SearchFilters)[])
    if (filters[key] !== defaultFilters[key]) params.set(key, filters[key]);
  return params.toString();
}

export function searchHash(filters: SearchFilters): string {
  const query = toQueryString(filters);
  return query ? `/search?${query}` : '/search';
}

export function search(filters: SearchFilters, page: number, size = 12): Promise<SearchResult> {
  const query = toQueryString(filters);
  return api<SearchResult>(`/search?${query}${query ? '&' : ''}page=${page}&size=${size}`);
}

export function suggest(q: string): Promise<string[]> {
  return api<string[]>(`/search/suggest?q=${encodeURIComponent(q)}`);
}
