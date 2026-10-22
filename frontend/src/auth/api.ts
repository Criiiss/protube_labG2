import { getEnv } from '../utils/Env';

export interface SessionUser {
  id: number;
  username: string;
  displayName: string;
  avatarUrl: string | null;
}
export class ApiError extends Error {
  constructor(
    message: string,
    public status: number,
    public fields: Record<string, string> = {}
  ) {
    super(message);
  }
}
export async function api<T>(path: string, body?: unknown, method = body === undefined ? 'GET' : 'POST'): Promise<T> {
  let response: Response;
  try {
    response = await fetch(`${getEnv().API_BASE_URL}${path}`, {
      method,
      credentials: 'include',
      headers: { 'Content-Type': 'application/json', 'X-Protube-Request': '1' },
      ...(body !== undefined ? { body: JSON.stringify(body) } : {}),
    });
  } catch {
    throw new ApiError('No se puede conectar con el servidor. Comprueba que el backend esté en marcha.', 0);
  }
  const data = await response.json().catch(() => ({}));
  if (!response.ok)
    throw new ApiError(data.message || 'No se pudo completar la solicitud.', response.status, data.fields || {});
  return data as T;
}
