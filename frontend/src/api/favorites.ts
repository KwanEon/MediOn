interface ApiErrorResponse {
  message?: string;
}

export async function fetchFavorites(): Promise<number[]> {
  const response = await fetch('/api/v1/favorites', { credentials: 'include' });
  return parseResponse<number[]>(response);
}

export async function addFavorite(institutionId: number): Promise<void> {
  const response = await fetch(`/api/v1/favorites/${institutionId}`, {
    method: 'PUT',
    credentials: 'include'
  });
  await parseResponse(response);
}

export async function removeFavorite(institutionId: number): Promise<void> {
  const response = await fetch(`/api/v1/favorites/${institutionId}`, {
    method: 'DELETE',
    credentials: 'include'
  });
  await parseResponse(response);
}

async function parseResponse<T = void>(response: Response): Promise<T> {
  if (!response.ok) {
    const error = await response.json().catch(() => null) as ApiErrorResponse | null;
    throw new Error(error?.message ?? '즐겨찾기 요청을 처리하지 못했습니다.');
  }
  if (response.status === 204) {
    return undefined as T;
  }
  return response.json() as Promise<T>;
}
