import { api, ApiError } from '../api.ts';

describe('api', () => {
  beforeEach(() => {
    fetchMock.resetMocks();
  });

  it('performs GET requests and returns JSON', async () => {
    fetchMock.mockResponseOnce(JSON.stringify({ ok: true }));

    await expect(api('/test')).resolves.toEqual({ ok: true });
    expect(fetchMock).toHaveBeenCalledWith(
      'other/test',
      expect.objectContaining({
        method: 'GET',
        credentials: 'include',
        headers: { 'Content-Type': 'application/json', 'X-Protube-Request': '1' },
      })
    );
  });

  it('serializes bodies and supports explicit methods', async () => {
    fetchMock.mockResponseOnce(JSON.stringify({ id: 1 }));
    await api('/test', { name: 'one' });
    expect(fetchMock).toHaveBeenLastCalledWith(
      'other/test',
      expect.objectContaining({ method: 'POST', body: JSON.stringify({ name: 'one' }) })
    );

    fetchMock.mockResponseOnce(JSON.stringify({ ok: true }));
    await api('/test', undefined, 'DELETE');
    expect(fetchMock).toHaveBeenLastCalledWith('other/test', expect.objectContaining({ method: 'DELETE' }));
  });

  it('normalizes network errors', async () => {
    fetchMock.mockRejectOnce(new Error('offline'));
    await expect(api('/test')).rejects.toMatchObject({
      message: 'No se puede conectar con el servidor. Comprueba que el backend esté en marcha.',
      status: 0,
    });
  });

  it('exposes server messages and fields', async () => {
    fetchMock.mockResponseOnce(JSON.stringify({ message: 'Invalid', fields: { email: 'bad' } }), { status: 422 });
    await expect(api('/test')).rejects.toEqual(expect.objectContaining({
      message: 'Invalid',
      status: 422,
      fields: { email: 'bad' },
    }));

    fetchMock.mockResponseOnce('not-json', { status: 500 });
    await expect(api('/test')).rejects.toEqual(expect.objectContaining({
      message: 'No se pudo completar la solicitud.',
      status: 500,
      fields: {},
    }));
  });

  it('constructs ApiError with default fields', () => {
    const error = new ApiError('x', 400);
    expect(error).toBeInstanceOf(Error);
    expect(error.fields).toEqual({});
  });
});
