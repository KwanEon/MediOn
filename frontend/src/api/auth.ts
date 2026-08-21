import type {
  AddressSearchResult,
  AuthUser,
  LoginRequest,
  RegisterRequest,
  UpdateProfileRequest
} from '../types/auth';

interface ApiErrorResponse {
  message?: string;
  fieldErrors?: Array<{ field: string; message: string }>;
}

export async function fetchCurrentUser(): Promise<AuthUser | null> {
  const response = await fetch('/api/v1/auth/me', { credentials: 'include' });
  if (response.status === 401) {
    return null;
  }
  return parseResponse<AuthUser>(response);
}

export async function login(request: LoginRequest): Promise<AuthUser> {
  return requestJson<AuthUser>('/api/v1/auth/login', request);
}

export async function register(request: RegisterRequest): Promise<AuthUser> {
  return requestJson<AuthUser>('/api/v1/auth/register', request);
}

export async function updateProfile(request: UpdateProfileRequest): Promise<AuthUser> {
  return requestJson<AuthUser>('/api/v1/auth/me', request, 'PATCH');
}

export async function logout(): Promise<void> {
  const response = await fetch('/api/v1/auth/logout', {
    method: 'POST',
    credentials: 'include'
  });
  if (!response.ok && response.status !== 401) {
    await parseResponse(response);
  }
}

export async function searchAddresses(
  query: string,
  includeStations = false
): Promise<AddressSearchResult[]> {
  const params = new URLSearchParams({
    query,
    includeStations: String(includeStations)
  });
  const response = await fetch(`/api/v1/auth/addresses?${params.toString()}`, {
    credentials: 'include'
  });
  return parseResponse<AddressSearchResult[]>(response);
}

async function requestJson<T>(url: string, body: unknown, method = 'POST'): Promise<T> {
  const response = await fetch(url, {
    method,
    credentials: 'include',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  });
  return parseResponse<T>(response);
}

async function parseResponse<T = void>(response: Response): Promise<T> {
  if (!response.ok) {
    const error = await response.json().catch(() => null) as ApiErrorResponse | null;
    throw new Error(error?.fieldErrors?.[0]?.message ?? error?.message ?? '요청을 처리하지 못했습니다.');
  }
  if (response.status === 204) {
    return undefined as T;
  }
  return response.json() as Promise<T>;
}
