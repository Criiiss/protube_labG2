import type { Category } from '../../data/home';

export function CategoryBar({
  categories,
  selected,
  onSelect,
}: {
  categories: Category[];
  selected: string;
  onSelect: (id: string) => void;
}) {
  return (
    <nav className="category-bar" aria-label="Categorías de vídeo">
      {categories.map((category) => (
        <button
          key={category.id}
          className={`category-chip ${selected === category.id ? 'selected' : ''}`}
          aria-pressed={selected === category.id}
          onClick={() => onSelect(category.id)}
        >
          {category.label}
        </button>
      ))}
    </nav>
  );
}
