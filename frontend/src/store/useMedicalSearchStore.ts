import { create } from 'zustand';
import { createJSONStorage, persist } from 'zustand/middleware';
import { fetchAllNearbyInstitutions } from '../api/institutions';
import { getCategory, INITIAL_LOCATION } from '../constants/institutions';
import type {
  CategoryId,
  Coordinates,
  HospitalDepartmentId,
  InstitutionId,
  OperatingScheduleFilter,
  NearbyInstitutionResponse
} from '../types/institution';

interface MedicalSearchState {
  location: Coordinates;
  locationLabel: string;
  response: NearbyInstitutionResponse | null;
  selectedCategory: CategoryId;
  selectedHospitalDepartment: HospitalDepartmentId;
  radiusMeters: number;
  operatingSchedule: OperatingScheduleFilter;
  keyword: string;
  submittedKeyword: string;
  favoritesOnly: boolean;
  favorites: InstitutionId[];
  loading: boolean;
  locating: boolean;
  locationReady: boolean;
  locationAttempted: boolean;
  accountUserId: number | null;
  error: string;
  previewMode: boolean;
  selectedId: InstitutionId | null;
  mobileMenuOpen: boolean;
}

interface MedicalSearchActions {
  setSelectedCategory: (selectedCategory: CategoryId) => void;
  setSelectedHospitalDepartment: (selectedHospitalDepartment: HospitalDepartmentId) => void;
  setRadiusMeters: (radiusMeters: number) => void;
  setOperatingSchedule: (operatingSchedule: OperatingScheduleFilter) => void;
  setKeyword: (keyword: string) => void;
  submitSearch: () => void;
  clearSearch: () => void;
  setFavoritesOnly: (favoritesOnly: boolean) => void;
  toggleFavorite: (id: InstitutionId) => void;
  setSelectedId: (selectedId: InstitutionId | null) => void;
  toggleMobileMenu: () => void;
  closeMobileMenu: () => void;
  loadNearbyInstitutions: () => Promise<void>;
  requestCurrentLocation: () => void;
  setAccountLocation: (userId: number, location: Coordinates, address: string) => void;
}

export type MedicalSearchStore = MedicalSearchState & MedicalSearchActions;

type PersistedMedicalSearchState = Pick<MedicalSearchStore, 'favorites'>;

let activeSearchKey: string | null = null;
let activeSearchSequence = 0;

const initialState: MedicalSearchState = {
  location: INITIAL_LOCATION,
  locationLabel: '현재 위치 확인 중',
  response: null,
  selectedCategory: 'ALL',
  selectedHospitalDepartment: 'ALL',
  radiusMeters: 3000,
  operatingSchedule: 'ALL',
  keyword: '',
  submittedKeyword: '',
  favoritesOnly: false,
  favorites: [],
  loading: false,
  locating: false,
  locationReady: false,
  locationAttempted: false,
  accountUserId: null,
  error: '',
  previewMode: false,
  selectedId: null,
  mobileMenuOpen: false
};

export const useMedicalSearchStore = create<MedicalSearchStore>()(
  persist<MedicalSearchStore, [], [], PersistedMedicalSearchState>(
    (set, get) => ({
      ...initialState,
      setSelectedCategory: (selectedCategory) => set((state) => ({
        selectedCategory,
        selectedHospitalDepartment: selectedCategory === 'HOSPITAL'
          ? state.selectedHospitalDepartment
          : 'ALL',
        selectedId: null
      })),
      setSelectedHospitalDepartment: (selectedHospitalDepartment) => set({
        selectedHospitalDepartment,
        selectedCategory: 'HOSPITAL',
        selectedId: null
      }),
      setRadiusMeters: (radiusMeters) => set({ radiusMeters, selectedId: null }),
      setOperatingSchedule: (operatingSchedule) => set({ operatingSchedule, selectedId: null }),
      setKeyword: (keyword) => set({ keyword }),
      submitSearch: () => set((state) => ({ submittedKeyword: state.keyword.trim(), selectedId: null })),
      clearSearch: () => set({ keyword: '', submittedKeyword: '', selectedId: null }),
      setFavoritesOnly: (favoritesOnly) => set({ favoritesOnly, selectedId: null }),
      toggleFavorite: (id) => set((state) => ({
        favorites: state.favorites.includes(id)
          ? state.favorites.filter((favoriteId) => favoriteId !== id)
          : [...state.favorites, id]
      })),
      setSelectedId: (selectedId) => set({ selectedId }),
      toggleMobileMenu: () => set((state) => ({ mobileMenuOpen: !state.mobileMenuOpen })),
      closeMobileMenu: () => set({ mobileMenuOpen: false }),
      setAccountLocation: (userId, location, address) => {
        const locationLabel = `등록 주소 기준 · ${address}`;
        const current = get();
        if (current.accountUserId === userId
          && current.locationReady
          && current.location.lat === location.lat
          && current.location.lng === location.lng
          && current.locationLabel === locationLabel) {
          return;
        }
        activeSearchSequence += 1;
        activeSearchKey = null;
        set({
          location,
          locationLabel,
          locationReady: true,
          locationAttempted: false,
          accountUserId: userId,
          locating: false,
          response: null,
          error: '',
          selectedId: null
        });
      },
      loadNearbyInstitutions: async () => {
        const {
          location,
          locationReady,
          radiusMeters,
          selectedCategory,
          selectedHospitalDepartment,
          operatingSchedule
        } = get();
        if (!locationReady) {
          return;
        }
        const types = getCategory(selectedCategory).types;
        const searchKey = [
          location.lat,
          location.lng,
          radiusMeters,
          types.join(','),
          selectedHospitalDepartment,
          operatingSchedule
        ].join(':');
        if (get().loading && activeSearchKey === searchKey) {
          return;
        }

        const searchSequence = ++activeSearchSequence;
        activeSearchKey = searchKey;
        set({ loading: true, error: '' });

        try {
          const response = await fetchAllNearbyInstitutions({
            ...location,
            radiusMeters,
            types,
            hospitalDepartment: selectedHospitalDepartment === 'ALL'
              ? undefined
              : selectedHospitalDepartment,
            operatingSchedule,
            size: 100
          });
          if (searchSequence !== activeSearchSequence) {
            return;
          }
          set({ response, previewMode: false });
        } catch (error: unknown) {
          if (searchSequence !== activeSearchSequence) {
            return;
          }
          set({
            response: null,
            previewMode: false,
            selectedId: null,
            error: error instanceof Error
              ? error.message
              : '의료기관 정보를 불러오지 못했습니다.'
          });
        } finally {
          if (searchSequence === activeSearchSequence) {
            activeSearchKey = null;
            set({ loading: false });
          }
        }
      },
      requestCurrentLocation: () => {
        const current = get();
        const hadReadyLocation = current.locationReady && current.accountUserId === null;
        set({
          location: hadReadyLocation ? current.location : INITIAL_LOCATION,
          locationLabel: hadReadyLocation ? current.locationLabel : '현재 위치 확인 중',
          response: hadReadyLocation ? current.response : null,
          error: '',
          locating: true,
          locationReady: hadReadyLocation,
          locationAttempted: true,
          accountUserId: null,
          selectedId: null
        });

        if (!navigator.geolocation) {
          set({
            error: '이 브라우저에서는 현재 위치를 사용할 수 없습니다.',
            locationLabel: hadReadyLocation ? get().locationLabel : '현재 위치를 확인할 수 없음',
            locating: false
          });
          return;
        }

        navigator.geolocation.getCurrentPosition(
          (position) => set({
            location: {
              lat: position.coords.latitude,
              lng: position.coords.longitude
            },
            locationLabel: '현재 위치',
            locating: false,
            locationReady: true,
            accountUserId: null,
            selectedId: null
          }),
          () => set({
            error: '위치 권한을 허용한 뒤 내 위치로 찾기 버튼을 다시 눌러 주세요.',
            locationLabel: hadReadyLocation ? get().locationLabel : '현재 위치 권한 필요',
            locating: false,
            selectedId: null
          }),
          { enableHighAccuracy: true, timeout: 8000, maximumAge: 60000 }
        );
      }
    }),
    {
      name: 'medion-search-store',
      storage: createJSONStorage(() => localStorage),
      partialize: (state) => ({ favorites: state.favorites })
    }
  )
);
