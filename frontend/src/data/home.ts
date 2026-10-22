export interface Category {
  id: string;
  label: string;
}
export interface Video {
  id: string;
  title: string;
  channel: string;
  views: string;
  published: string;
  duration: string;
  categoryId: string;
  thumbnail: string;
  viewCount?: number;
  createdAt?: string;
}

// Replace these arrays with API data without changing the presentation components.
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

const titles = [
  'Un día cualquiera puede ser una gran aventura',
  'Dale al play: música para desconectar',
  'Pequeñas historias, grandes momentos',
  'El mundo desde otra perspectiva',
  'Una partida más y nos vamos',
  'Ideas que merecen ser compartidas',
  'Fuera de la rutina: descubre algo nuevo',
  'Conversaciones sin prisa',
  'La creatividad empieza con una pregunta',
  'El viaje también es el destino',
  'Así suena una tarde perfecta',
  'Todo lo que pasa detrás de la cámara',
  'Lo mejor está por descubrir',
  'Tecnología para el día a día',
  'Más allá del resultado',
  'Una nueva forma de mirar',
  'Tu próxima inspiración',
];
export const demoVideos: Video[] = titles.map((title, index) => ({
  id: `demo-${index + 1}`,
  title,
  channel: ['ProTube Studio', 'Blue Sessions', 'Curiosos'][index % 3],
  views: `${12 + index * 7},${index % 10} mil visualizaciones`,
  published: index % 6 === 0 ? 'hace 1 día' : `hace ${(index % 6) + 1} días`,
  duration: `${8 + index}:24`,
  categoryId: categories[(index % (categories.length - 1)) + 1].id,
  thumbnail: `${import.meta.env.BASE_URL}dog.png`,
}));
