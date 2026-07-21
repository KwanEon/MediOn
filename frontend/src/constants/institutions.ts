import type {
  CategoryId,
  CategoryOption,
  Coordinates,
  HospitalDepartmentId,
  HospitalDepartmentOption,
  InstitutionType,
  InstitutionTypeMeta
} from '../types/institution';

export const INITIAL_LOCATION: Coordinates = { lat: 0, lng: 0 };

export const CATEGORY_OPTIONS: readonly CategoryOption[] = [
  { id: 'ALL', label: '전체', types: ['HOSPITAL', 'PHARMACY', 'EMERGENCY_ROOM'] },
  { id: 'HOSPITAL', label: '병원', types: ['HOSPITAL'] },
  { id: 'PHARMACY', label: '약국', types: ['PHARMACY'] },
  { id: 'EMERGENCY_ROOM', label: '응급실', types: ['EMERGENCY_ROOM'] }
];

export const TYPE_META: Record<InstitutionType, InstitutionTypeMeta> = {
  HOSPITAL: { label: '병원' },
  PHARMACY: { label: '약국' },
  EMERGENCY_ROOM: { label: '응급실' }
};

export const HOSPITAL_DEPARTMENT_OPTIONS: readonly HospitalDepartmentOption[] = [
  { value: 'ALL', label: '전체' },
  { value: 'INTERNAL_MEDICINE', label: '내과' },
  { value: 'PEDIATRICS', label: '소아과' },
  { value: 'DERMATOLOGY', label: '피부과' },
  { value: 'OTOLARYNGOLOGY', label: '이비인후과' },
  { value: 'ORTHOPEDICS', label: '정형외과' },
  { value: 'SURGERY', label: '외과' },
  { value: 'FAMILY_MEDICINE', label: '가정의학과' },
  { value: 'NEUROSURGERY', label: '신경외과' },
  { value: 'ANESTHESIOLOGY_PAIN_MEDICINE', label: '마취통증과' },
  { value: 'PLASTIC_SURGERY', label: '성형외과' },
  { value: 'OBSTETRICS_GYNECOLOGY', label: '산부인과' },
  { value: 'OPHTHALMOLOGY', label: '안과' },
  { value: 'MENTAL_HEALTH_MEDICINE', label: '정신건강의학과' },
  { value: 'UROLOGY', label: '비뇨의학과' },
  { value: 'NEUROLOGY', label: '신경과' },
  { value: 'REHABILITATION_MEDICINE', label: '재활의학과' },
  { value: 'CARDIOTHORACIC_SURGERY', label: '흉부외과' },
  { value: 'RADIOLOGY', label: '영상의학과' },
  { value: 'DENTISTRY', label: '치과' },
  { value: 'KOREAN_CLINIC', label: '한의원' }
];

export function getHospitalDepartmentLabel(departmentId: HospitalDepartmentId): string | null {
  if (departmentId === 'ALL') {
    return null;
  }
  return HOSPITAL_DEPARTMENT_OPTIONS.find((option) => option.value === departmentId)?.label ?? null;
}

export function getCategory(categoryId: CategoryId): CategoryOption {
  return CATEGORY_OPTIONS.find((category) => category.id === categoryId) ?? CATEGORY_OPTIONS[0];
}
