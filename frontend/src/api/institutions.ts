import type {
  EmergencyBedAvailabilityResponse,
  InstitutionId,
  NearbyInstitutionResponse,
  NearbyInstitutionSearchParams
} from '../types/institution';

interface ApiErrorResponse {
  message?: string;
}

export async function fetchNearbyInstitutions({
  lat,
  lng,
  radiusMeters = 3000,
  keyword,
  types = ['HOSPITAL', 'PHARMACY', 'EMERGENCY_ROOM'],
  hospitalDepartment,
  operatingSchedule = 'ALL',
  openNowOnly = true,
  favoritesOnly = false,
  page = 0,
  size = 30
}: NearbyInstitutionSearchParams, signal?: AbortSignal): Promise<NearbyInstitutionResponse> {
  const params = new URLSearchParams({
    lat: String(lat),
    lng: String(lng),
    radiusMeters: String(radiusMeters),
    types: types.join(','),
    operatingSchedule,
    openNowOnly: String(openNowOnly),
    favoritesOnly: String(favoritesOnly),
    page: String(page),
    size: String(size)
  });
  if (hospitalDepartment) {
    params.set('hospitalDepartment', hospitalDepartment);
  }
  if (keyword?.trim()) {
    params.set('keyword', keyword.trim());
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
