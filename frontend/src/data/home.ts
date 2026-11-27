export interface Category {
  id: string;
  label: string;
}
export interface Video {
  id: string;
  title: string;
  channel: string;
  displayName?: string | null;
  avatarUrl?: string | null;
  description?: string | null;
  views: string;
  published: string;
  duration: string;
  categoryId: string;
  thumbnail: string;
  videoUrl?: string;
  viewCount?: number;
  createdAt?: string;
}

export const categories: Category[] = [
  { id: 'all', label: 'Todos' },
  { id: 'music', label: 'Música' },
  { id: 'gaming', label: 'Videojuegos' },
  { id: 'sports', label: 'Deportes' },
  { id: 'technology', label: 'Tecnología' },
  { id: 'podcasts', label: 'Pódcasts' },
  { id: 'travel', label: 'Viajes' },
  { id: 'culture', label: 'Cultura' },
];
