import type {
  CategoryId,
  CategoryOption,
  Coordinates,
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
  { value: 'PEDIATRICS', label: '소아청소년과' },
  { value: 'NEUROLOGY', label: '신경과' },
  { value: 'MENTAL_HEALTH_MEDICINE', label: '정신건강의학과' },
  { value: 'DERMATOLOGY', label: '피부과' },
  { value: 'SURGERY', label: '외과' },
  { value: 'CARDIOTHORACIC_SURGERY', label: '흉부외과' },
  { value: 'ORTHOPEDICS', label: '정형외과' },
  { value: 'NEUROSURGERY', label: '신경외과' },
  { value: 'PLASTIC_SURGERY', label: '성형외과' },
  { value: 'OBSTETRICS_GYNECOLOGY', label: '산부인과' },
  { value: 'OPHTHALMOLOGY', label: '안과' },
  { value: 'OTOLARYNGOLOGY', label: '이비인후과' },
  { value: 'UROLOGY', label: '비뇨기과' },
  { value: 'TUBERCULOSIS', label: '결핵과' },
  { value: 'REHABILITATION_MEDICINE', label: '재활의학과' },
  { value: 'ANESTHESIOLOGY_PAIN_MEDICINE', label: '마취통증의학과' },
  { value: 'RADIOLOGY', label: '영상의학과' },
  { value: 'THERAPEUTIC_RADIOLOGY', label: '치료방사선과' },
  { value: 'CLINICAL_PATHOLOGY', label: '임상병리과' },
  { value: 'ANATOMICAL_PATHOLOGY', label: '해부병리과' },
  { value: 'FAMILY_MEDICINE', label: '가정의학과' },
  { value: 'NUCLEAR_MEDICINE', label: '핵의학과' },
  { value: 'EMERGENCY_MEDICINE', label: '응급의학과' },
  { value: 'OCCUPATIONAL_MEDICINE', label: '산업의학과' },
  { value: 'DENTISTRY', label: '치과' },
  { value: 'KOREAN_INTERNAL_MEDICINE', label: '한방내과' },
  { value: 'KOREAN_GYNECOLOGY', label: '한방부인과' },
  { value: 'PREVENTIVE_MEDICINE', label: '예방의학과' },
  { value: 'KOREAN_CLINIC', label: '한의원·한방병원' }
];

export function getCategory(categoryId: CategoryId): CategoryOption {
  return CATEGORY_OPTIONS.find((category) => category.id === categoryId) ?? CATEGORY_OPTIONS[0];
}
