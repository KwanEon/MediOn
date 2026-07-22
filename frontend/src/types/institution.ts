export type InstitutionType = 'HOSPITAL' | 'PHARMACY' | 'EMERGENCY_ROOM';

export type CategoryId = 'ALL' | InstitutionType;

export type OperatingScheduleFilter =
  | 'ALL'
  | 'NIGHT'
  | 'TWENTY_FOUR_HOURS'
  | 'SATURDAY'
  | 'SUNDAY'
  | 'HOLIDAY';

export type HospitalDepartmentId =
  | 'ALL'
  | 'INTERNAL_MEDICINE'
  | 'PEDIATRICS'
  | 'NEUROLOGY'
  | 'MENTAL_HEALTH_MEDICINE'
  | 'DERMATOLOGY'
  | 'SURGERY'
  | 'CARDIOTHORACIC_SURGERY'
  | 'ORTHOPEDICS'
  | 'NEUROSURGERY'
  | 'PLASTIC_SURGERY'
  | 'OBSTETRICS_GYNECOLOGY'
  | 'OPHTHALMOLOGY'
  | 'OTOLARYNGOLOGY'
  | 'UROLOGY'
  | 'TUBERCULOSIS'
  | 'REHABILITATION_MEDICINE'
  | 'ANESTHESIOLOGY_PAIN_MEDICINE'
  | 'RADIOLOGY'
  | 'THERAPEUTIC_RADIOLOGY'
  | 'CLINICAL_PATHOLOGY'
  | 'ANATOMICAL_PATHOLOGY'
  | 'FAMILY_MEDICINE'
  | 'NUCLEAR_MEDICINE'
  | 'EMERGENCY_MEDICINE'
  | 'OCCUPATIONAL_MEDICINE'
  | 'DENTISTRY'
  | 'KOREAN_INTERNAL_MEDICINE'
  | 'KOREAN_GYNECOLOGY'
  | 'PREVENTIVE_MEDICINE'
  | 'KOREAN_CLINIC';

export type InstitutionId = number | string;

export interface Coordinates {
  lat: number;
  lng: number;
}

export interface NearbyInstitution {
  id: InstitutionId;
  type: InstitutionType;
  name: string;
  institutionKind: string | null;
  medicalDepartments: string[];
  phoneNumber: string | null;
  roadAddress: string | null;
  latitude: number;
  longitude: number;
  distanceMeters: number;
  open: boolean;
  operatingHoursKnown: boolean;
  todayOpenTime: string | null;
  todayCloseTime: string | null;
  availableEmergencyBeds: number | null;
  operatingSchedules: OperatingScheduleFilter[];
  lastSyncedAt: string | null;
}

export interface PageResponse {
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface NearbyInstitutionResponse {
  requestedAt: string;
  radiusMeters: number;
  lastSyncedAt: string | null;
  items: NearbyInstitution[];
  page: PageResponse;
}

export interface NearbyInstitutionSearchParams extends Coordinates {
  radiusMeters?: number;
  types?: readonly InstitutionType[];
  hospitalDepartment?: Exclude<HospitalDepartmentId, 'ALL'>;
  operatingSchedule?: OperatingScheduleFilter;
  page?: number;
  size?: number;
}

export interface CategoryOption {
  id: CategoryId;
  label: string;
  types: readonly InstitutionType[];
}

export interface InstitutionTypeMeta {
  label: string;
}

export interface HospitalDepartmentOption {
  value: HospitalDepartmentId;
  label: string;
}
