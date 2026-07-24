import type { Coordinates, InstitutionType, NearbyInstitution, NearbyInstitutionResponse } from '../types/institution';

type PreviewInstitutionTuple = readonly [
  InstitutionType,
  string,
  string,
  string,
  number,
  number,
  number,
  string,
  string
];

export function createPreviewResponse(
  items: NearbyInstitution[],
  radiusMeters: number
): NearbyInstitutionResponse {
  const syncedAt = new Date().toISOString();

  return {
    requestedAt: syncedAt,
    radiusMeters,
    lastSyncedAt: syncedAt,
    items,
    page: { number: 0, size: items.length, totalElements: items.length, totalPages: 1 }
  };
}

export function createPreviewInstitutions(center: Coordinates): NearbyInstitution[] {
  const sampleData: readonly PreviewInstitutionTuple[] = [
    ['HOSPITAL', '온누리365의원', '서울 중구 세종대로 110', '02-120-3650', 0.0032, 0.0024, 420, '09:00', '20:00'],
    ['PHARMACY', '시청푸른약국', '서울 중구 을지로 18', '02-777-2040', -0.0018, 0.0049, 610, '08:30', '22:00'],
    ['EMERGENCY_ROOM', '서울중앙 응급의료센터', '서울 종로구 새문안로 65', '02-2001-1000', 0.0061, -0.0038, 890, '00:00', '24:00'],
    ['HOSPITAL', '다온내과의원', '서울 중구 남대문로 42', '02-755-1010', -0.0049, -0.0026, 1100, '09:00', '18:00'],
    ['PHARMACY', '정다운온누리약국', '서울 종로구 종로 33', '02-735-8890', 0.0055, 0.0065, 1400, '09:00', '21:00'],
    ['HOSPITAL', '바른정형외과의원', '서울 서대문구 통일로 97', '02-312-7788', 0.0092, -0.0063, 1800, '09:00', '19:00']
  ];

  return sampleData.map((item, index) => {
    const [type, name, roadAddress, phoneNumber, latOffset, lngOffset, distanceMeters, todayOpenTime, todayCloseTime] = item;

    return {
      id: `preview-${index + 1}`,
      type,
      name,
      institutionKind: type === 'HOSPITAL'
        ? name.includes('의원') ? '의원' : '병원'
        : type === 'EMERGENCY_ROOM' ? '응급의료기관' : null,
      medicalDepartments: type === 'HOSPITAL' ? previewMedicalDepartments(name) : [],
      phoneNumber,
      roadAddress,
      latitude: center.lat + latOffset,
      longitude: center.lng + lngOffset,
      distanceMeters,
      open: true,
      operatingHoursKnown: true,
      todayOpenTime,
      todayCloseTime,
      availableEmergencyBeds: null,
      emergencyBedAvailabilityLoading: false,
      operatingSchedules: [],
      lastSyncedAt: new Date().toISOString()
    };
  });
}

function previewMedicalDepartments(name: string): string[] {
  if (name.includes('내과')) {
    return ['내과'];
  }
  if (name.includes('정형외과')) {
    return ['정형외과'];
  }
  return [];
}
