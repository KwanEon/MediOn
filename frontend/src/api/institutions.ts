import type {
  EmergencyBedAvailabilityResponse,
  InstitutionId,
  NearbyInstitutionResponse,
  NearbyInstitutionSearchParams
} from '../types/institution';

interface ApiErrorResponse {
  message?: string;
}

const MAX_SEARCH_RESULTS = 500;

export async function fetchAllNearbyInstitutions(
  searchParams: NearbyInstitutionSearchParams,
  signal?: AbortSignal
): Promise<NearbyInstitutionResponse> {
  return fetchNearbyInstitutions({
    ...searchParams,
    page: 0,
    size: Math.min(searchParams.size ?? MAX_SEARCH_RESULTS, MAX_SEARCH_RESULTS)
  }, signal);
}

export async function fetchNearbyInstitutions({
  lat,
  lng,
  radiusMeters = 3000,
  types = ['HOSPITAL', 'PHARMACY', 'EMERGENCY_ROOM'],
  hospitalDepartment,
  operatingSchedule = 'ALL',
  openNowOnly = true,
  page = 0,
  size = 20
}: NearbyInstitutionSearchParams, signal?: AbortSignal): Promise<NearbyInstitutionResponse> {
  const params = new URLSearchParams({
    lat: String(lat),
    lng: String(lng),
    radiusMeters: String(radiusMeters),
    types: types.join(','),
    operatingSchedule,
    openNowOnly: String(openNowOnly),
    page: String(page),
    size: String(size)
  });
  if (hospitalDepartment) {
    params.set('hospitalDepartment', hospitalDepartment);
  }

  const response = await fetch(`/api/v1/institutions/nearby?${params.toString()}`, {
    credentials: 'include',
    signal
  });

  if (!response.ok) {
    const error = await response.json().catch(() => null) as ApiErrorResponse | null;
    throw new Error(error?.message ?? '의료기관 정보를 불러오지 못했습니다.');
  }

  return response.json() as Promise<NearbyInstitutionResponse>;
}

export async function fetchEmergencyBedAvailability(
  institutionIds: InstitutionId[],
  signal?: AbortSignal
): Promise<EmergencyBedAvailabilityResponse> {
  const response = await fetch('/api/v1/institutions/emergency-beds', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    credentials: 'include',
    signal,
    body: JSON.stringify({ institutionIds })
  });

  if (!response.ok) {
    const error = await response.json().catch(() => null) as ApiErrorResponse | null;
    throw new Error(error?.message ?? '응급실 가용 병상 정보를 불러오지 못했습니다.');
  }

  return response.json() as Promise<EmergencyBedAvailabilityResponse>;
}
