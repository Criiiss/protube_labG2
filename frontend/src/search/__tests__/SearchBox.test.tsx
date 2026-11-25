import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import fetchMock from 'jest-fetch-mock';
import { SearchBox } from '../SearchBox';
import { readHistory, remember } from '../history';

beforeEach(() => {
  localStorage.clear();
  fetchMock.resetMocks();
});

function setup(value?: string) {
  const onSearch = jest.fn();
  render(<SearchBox onSearch={onSearch} value={value} />);
  return { onSearch, input: screen.getByRole('combobox', { name: 'Buscar vídeos y canales' }) };
}

describe('SearchBox', () => {
  it('searches with Enter and remembers the search', () => {
    const { onSearch, input } = setup();
    fireEvent.change(input, { target: { value: '  paella ' } });
    fireEvent.submit(input);
    expect(onSearch).toHaveBeenCalledWith('paella');
    expect(readHistory()).toEqual(['paella']);
  });

  it('allows an empty search without remembering it', () => {
    const { onSearch, input } = setup();
    fireEvent.click(screen.getByRole('button', { name: 'Buscar' }));
    expect(onSearch).toHaveBeenCalledWith('');
    expect(readHistory()).toEqual([]);
    expect(input).toHaveValue('');
  });

  it('shows suggestions while typing and picks one with the keyboard', async () => {
    fetchMock.mockResponseOnce(JSON.stringify(['Receta de paella', 'Paella de marisco']));
    const { onSearch, input } = setup();
    fireEvent.focus(input);
    fireEvent.change(input, { target: { value: 'pae' } });
    expect(await screen.findByRole('option', { name: 'Paella de marisco' })).toBeInTheDocument();
    expect(fetchMock.mock.calls[0][0]).toBe('other/search/suggest?q=pae');
    fireEvent.keyDown(input, { key: 'ArrowDown' });
    fireEvent.keyDown(input, { key: 'ArrowDown' });
    fireEvent.keyDown(input, { key: 'ArrowDown' });
    fireEvent.keyDown(input, { key: 'ArrowUp' });
    expect(screen.getByRole('option', { name: 'Paella de marisco' })).toHaveAttribute('aria-selected', 'true');
    fireEvent.keyDown(input, { key: 'ArrowUp' });
    fireEvent.submit(input);
    expect(onSearch).toHaveBeenCalledWith('Receta de paella');
  });

  it('closes suggestions with Escape and on blur, and hides them if the backend fails', async () => {
    fetchMock.mockResponseOnce(JSON.stringify(['Jazz']));
    const { input } = setup();
    fireEvent.focus(input);
    fireEvent.change(input, { target: { value: 'ja' } });
    await screen.findByRole('option', { name: 'Jazz' });
    fireEvent.keyDown(input, { key: 'Escape' });
    expect(screen.queryByRole('listbox')).not.toBeInTheDocument();
    fireEvent.focus(input);
    expect(screen.getByRole('listbox')).toBeInTheDocument();
    fireEvent.blur(input);
    expect(screen.queryByRole('listbox')).not.toBeInTheDocument();

    fetchMock.mockRejectOnce(new Error('down'));
    fireEvent.focus(input);
    fireEvent.change(input, { target: { value: 'jaz' } });
    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(2));
    await waitFor(() => expect(screen.queryByRole('listbox')).not.toBeInTheDocument());
  });

  it('clears the text with the clear button', () => {
    const { input } = setup('jazz');
    expect(input).toHaveValue('jazz');
    fireEvent.click(screen.getByRole('button', { name: 'Borrar texto' }));
    expect(input).toHaveValue('');
    expect(screen.queryByRole('button', { name: 'Borrar texto' })).not.toBeInTheDocument();
  });

  it('shows recent searches, searches one, forgets one and clears them', () => {
    remember('paella');
    remember('jazz');
    const { onSearch, input } = setup();
    fireEvent.focus(input);
    expect(screen.getByText('Búsquedas recientes')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Quitar «jazz» del historial' }));
    expect(screen.queryByRole('option', { name: /jazz/ })).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole('option', { name: /paella/ }));
    expect(onSearch).toHaveBeenCalledWith('paella');

    fireEvent.change(input, { target: { value: '' } });
    fireEvent.focus(input);
    fireEvent.click(screen.getByRole('button', { name: 'Borrar historial' }));
    expect(readHistory()).toEqual([]);
    expect(screen.queryByRole('listbox')).not.toBeInTheDocument();
  });
});
