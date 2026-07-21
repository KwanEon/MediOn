import type { NearbyInstitutionResponse, NearbyInstitutionSearchParams } from '../types/institution';

interface ApiErrorResponse {
  message?: string;
}

const MAX_SEARCH_RESULTS = 100;

export async function fetchAllNearbyInstitutions(
  searchParams: NearbyInstitutionSearchParams
): Promise<NearbyInstitutionResponse> {
  return fetchNearbyInstitutions({
    ...searchParams,
    page: 0,
    size: Math.min(searchParams.size ?? MAX_SEARCH_RESULTS, MAX_SEARCH_RESULTS)
  });
}

export async function fetchNearbyInstitutions({
  lat,
  lng,
  radiusMeters = 3000,
  types = ['HOSPITAL', 'PHARMACY'],
  hospitalDepartment,
  operatingSchedule = 'ALL',
  page = 0,
  size = 20
}: NearbyInstitutionSearchParams): Promise<NearbyInstitutionResponse> {
  const params = new URLSearchParams({
    lat: String(lat),
    lng: String(lng),
    radiusMeters: String(radiusMeters),
    types: types.join(','),
    operatingSchedule,
    page: String(page),
    size: String(size)
  });
  if (hospitalDepartment) {
    params.set('hospitalDepartment', hospitalDepartment);
  }

  const response = await fetch(`/api/v1/institutions/nearby?${params.toString()}`, {
    credentials: 'include'
  });

  if (!response.ok) {
    const error = await response.json().catch(() => null) as ApiErrorResponse | null;
    throw new Error(error?.message ?? '의료기관 정보를 불러오지 못했습니다.');
  }

  return response.json() as Promise<NearbyInstitutionResponse>;
}
