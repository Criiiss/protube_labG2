import { fireEvent, render, screen } from '@testing-library/react';
import ProfileStats from '../ProfileStats.tsx';
import ProfileImagePicker from '../ProfileImagePicker.tsx';
import { ChannelVideoGrid } from '../../videos/ChannelVideoGrid.tsx';

describe('profile and channel components', () => {
  it('formats profile statistics', () => {
    render(<ProfileStats stats={{ subscriptionsCount: 1200, videosCount: 3 }} />);
    expect(screen.getByText('1,2 mil')).toBeInTheDocument();
    expect(screen.getByText('3')).toBeInTheDocument();
  });

  it('renders empty channel state and custom text', () => {
    render(<ChannelVideoGrid videos={[]} emptyTitle="Sin vídeos" emptyDescription="Nada todavía" />);
    expect(screen.getByText('Sin vídeos')).toBeInTheDocument();
    expect(screen.getByText('Nada todavía')).toBeInTheDocument();
  });

  it('renders channel videos and navigates on selection', () => {
    render(<ChannelVideoGrid videos={[{
      id: 'a/b', title: 'Video', channel: 'channel', views: '1', published: 'today',
      duration: '1:00', categoryId: 'all', thumbnail: '/thumb',
    }]} />);
    fireEvent.click(screen.getByRole('button', { name: 'Ver Video' }));
    expect(window.location.hash).toContain('/watch?video=a%2Fb');
  });

  it('validates image types and size', () => {
    const alert = jest.spyOn(window, 'alert').mockImplementation(() => {});
    const onChange = jest.fn();
    render(<ProfileImagePicker label="Foto" value={null} aspectRatio="avatar" onChange={onChange} />);
    const input = document.getElementById('profile-image-avatar') as HTMLInputElement;
    fireEvent.change(input, { target: { files: [new File(['x'], 'bad.gif', { type: 'image/gif' })] } });
    expect(alert).toHaveBeenCalledWith('Selecciona una imagen JPG, PNG o WebP.');
    fireEvent.change(input, { target: { files: [new File([new Uint8Array(6 * 1024 * 1024)], 'big.png', { type: 'image/png' })] } });
    expect(alert).toHaveBeenCalledWith('La imagen no puede superar los 5 MB.');
    alert.mockRestore();
  });

  it('renders existing image and invokes removal', () => {
    const remove = jest.fn();
    render(<ProfileImagePicker label="Banner" value="data:image/png;base64,x" aspectRatio="banner" onChange={jest.fn()} onRemove={remove} />);
    expect(screen.getByAltText('Previsualización de banner')).toHaveAttribute('src', 'data:image/png;base64,x');
    fireEvent.click(screen.getByRole('button', { name: 'Eliminar' }));
    expect(remove).toHaveBeenCalled();
  });
});
