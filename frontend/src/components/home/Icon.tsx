export type IconName =
  | 'search'
  | 'plus'
  | 'bell'
  | 'menu'
  | 'home'
  | 'subscriptions'
  | 'user'
  | 'history'
  | 'library'
  | 'like'
  | 'clock'
  | 'chevron'
  | 'play';

const paths: Record<IconName, string> = {
  search: 'M21 21l-5-5M19 10.5a8.5 8.5 0 1 1-17 0 8.5 8.5 0 0 1 17 0',
  plus: 'M12 5v14M5 12h14',
  bell: 'M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M10 21h4',
  menu: 'M4 6h16M4 12h16M4 18h16',
  home: 'M3 10l9-7 9 7M5 9v12h5v-7h4v7h5V9',
  subscriptions: 'M7 3h10M4 6h16M3 10h18v11H3zM10 13l5 3-5 3z',
  user: 'M16 8a4 4 0 1 1-8 0 4 4 0 0 1 8 0M4 21v-2a8 8 0 0 1 16 0v2',
  history: 'M3 5v5h5M3 10a9 9 0 1 1 1 8M12 7v5l3 2',
  library: 'M4 3v18M8 3v18M12 4l5-1 4 17-5 1z',
  like: 'M7 10v11H3V10zM7 10l5-8c3 0 2 5 2 7h5a2 2 0 0 1 2 3l-2 7a3 3 0 0 1-3 2H7',
  clock: 'M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0M12 6v6l4 2',
  chevron: 'M9 5l7 7-7 7',
  play: 'M9 5l11 7-11 7z',
};

export function Icon({ name }: { name: IconName }) {
  return (
    <svg
      width="22"
      height="22"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.7"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <path d={paths[name]} />
    </svg>
  );
}
