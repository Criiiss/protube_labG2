import { fireEvent, render, screen } from '@testing-library/react';
import { CategoryBar } from '../home/CategoryBar.tsx';
import { Sidebar } from '../home/Sidebar.tsx';
import { VideoCard } from '../home/VideoCard.tsx';
import { VideoCarousel } from '../home/VideoCarousel.tsx';

describe('home components', () => {
  const categories = [
    { id: 'all', label: 'Todos' },
    { id: 'music', label: 'Música' },
  ];
  const video = {
    id: 'v1',
    title: 'Vídeo de prueba',
    channel: 'canal',
    views: '10 visualizaciones',
    published: 'hace 1 día',
    duration: '1:00',
    categoryId: 'all',
    thumbnail: '/thumb.png',
  };

  it('selects categories', () => {
    const onSelect = jest.fn();
    render(<CategoryBar categories={categories.slice(0, 2)} selected="all" onSelect={onSelect} />);
    expect(screen.getByRole('button', { name: 'Todos' })).toHaveAttribute('aria-pressed', 'true');
    fireEvent.click(screen.getByRole('button', { name: 'Música' }));
    expect(onSelect).toHaveBeenCalledWith('music');
  });

  it('toggles sidebar and renders subscriptions', () => {
    const onToggle = jest.fn();
    render(<Sidebar expanded={false} onToggle={onToggle} subscriptions={['Mi canal']} route="/library" />);
    fireEvent.click(screen.getByRole('button', { name: 'Expandir menú' }));
    expect(onToggle).toHaveBeenCalled();
    expect(screen.getByRole('link', { name: /Mi canal/ })).toHaveAttribute('href', '#/channel?name=Mi%20canal');
    expect(screen.getByRole('link', { name: 'Biblioteca' })).toHaveAttribute('href', '#/library');
  });

  it('selects videos and invokes menu actions', () => {
    const select = jest.fn();
    const action = jest.fn();
    render(<VideoCard video={video} onSelect={select} onAction={action} selectedActions={['saved']} />);
    fireEvent.click(screen.getByRole('button', { name: `Ver ${video.title}` }));
    expect(select).toHaveBeenCalledWith(video);
    fireEvent.click(screen.getByRole('button', { name: `Opciones de ${video.title}` }));
    expect(screen.getByRole('button', { name: /Guardar en biblioteca/ })).toHaveTextContent('✓');
    fireEvent.click(screen.getByRole('button', { name: 'Compartir' }));
    expect(action).toHaveBeenCalledWith('share', video);
  });

  it('moves through carousel and pagination', () => {
    const select = jest.fn();
    render(
      <VideoCarousel
        videos={[video, { ...video, id: 'v2', title: 'Dos' }, { ...video, id: 'v3', title: 'Tres' }]}
        onSelect={select}
      />
    );
    expect(screen.getByText(/Destacado 1 de 3/)).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Siguiente vídeo destacado' }));
    expect(screen.getByText(/Destacado 2 de 3/)).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Mostrar destacado 3' }));
    expect(screen.getByText(/Destacado 3 de 3/)).toBeInTheDocument();
  });
});
