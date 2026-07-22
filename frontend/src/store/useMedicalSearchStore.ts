import { create } from 'zustand';
import {
  addFavorite as addFavoriteRequest,
  fetchFavorites,
  removeFavorite as removeFavoriteRequest
} from '../api/favorites';
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

type LocationMode = 'INITIAL' | 'ACCOUNT' | 'CURRENT' | 'ADDRESS';

interface MedicalSearchState {
  location: Coordinates;
  locationLabel: string;
  locationMode: LocationMode;
  response: NearbyInstitutionResponse | null;
  selectedCategory: CategoryId;
  selectedHospitalDepartment: HospitalDepartmentId;
  radiusMeters: number;
  operatingSchedule: OperatingScheduleFilter;
  openNowOnly: boolean;
  resultSize: number;
  keyword: string;
  submittedKeyword: string;
  favoritesOnly: boolean;
  favorites: InstitutionId[];
  favoriteUserId: number | null;
  loading: boolean;
  locating: boolean;
  locationReady: boolean;
  locationAttempted: boolean;
  accountUserId: number | null;
  error: string;
  selectedId: InstitutionId | null;
  mobileMenuOpen: boolean;
}

interface MedicalSearchActions {
  setSelectedCategory: (selectedCategory: CategoryId) => void;
  setSelectedHospitalDepartment: (selectedHospitalDepartment: HospitalDepartmentId) => void;
  setRadiusMeters: (radiusMeters: number) => void;
  setOperatingSchedule: (operatingSchedule: OperatingScheduleFilter) => void;
  setOpenNowOnly: (openNowOnly: boolean) => void;
  setResultSize: (resultSize: number) => void;
  setKeyword: (keyword: string) => void;
  submitSearch: () => void;
  clearSearch: () => void;
  setFavoritesOnly: (favoritesOnly: boolean) => void;
  loadFavorites: (userId: number) => Promise<void>;
  clearFavorites: () => void;
  toggleFavorite: (id: InstitutionId) => Promise<void>;
  setSelectedId: (selectedId: InstitutionId | null) => void;
  toggleMobileMenu: () => void;
  closeMobileMenu: () => void;
  loadNearbyInstitutions: () => Promise<void>;
  requestCurrentLocation: () => void;
  setAddressLocation: (
    userId: number | null,
    location: Coordinates,
    address: string
  ) => void;
  setAccountLocation: (
    userId: number,
    location: Coordinates,
    address: string,
    force?: boolean
  ) => void;
}

export type MedicalSearchStore = MedicalSearchState & MedicalSearchActions;

let activeSearchKey: string | null = null;
let activeSearchSequence = 0;
let favoriteLoadSequence = 0;
const pendingFavoriteMutations = new Set<string>();

const initialState: MedicalSearchState = {
  location: INITIAL_LOCATION,
  locationLabel: '현재 위치 확인 중',
  locationMode: 'INITIAL',
  response: null,
  selectedCategory: 'ALL',
  selectedHospitalDepartment: 'ALL',
  radiusMeters: 3000,
  operatingSchedule: 'ALL',
  openNowOnly: true,
  resultSize: 100,
  keyword: '',
  submittedKeyword: '',
  favoritesOnly: false,
  favorites: [],
  favoriteUserId: null,
  loading: false,
  locating: false,
  locationReady: false,
  locationAttempted: false,
  accountUserId: null,
  error: '',
  selectedId: null,
  mobileMenuOpen: false
};

export const useMedicalSearchStore = create<MedicalSearchStore>((set, get) => ({
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
      setOpenNowOnly: (openNowOnly) => set({ openNowOnly, selectedId: null }),
      setResultSize: (resultSize) => set({ resultSize, selectedId: null }),
      setKeyword: (keyword) => set({ keyword }),
      submitSearch: () => set((state) => ({ submittedKeyword: state.keyword.trim(), selectedId: null })),
      clearSearch: () => set({ keyword: '', submittedKeyword: '', selectedId: null }),
      setFavoritesOnly: (favoritesOnly) => set({ favoritesOnly, selectedId: null }),
      loadFavorites: async (userId) => {
        const loadSequence = ++favoriteLoadSequence;
        set({ favorites: [], favoriteUserId: userId, favoritesOnly: false });
        try {
          const favorites = await fetchFavorites();
          if (loadSequence === favoriteLoadSequence && get().favoriteUserId === userId) {
            set({ favorites });
          }
        } catch (error: unknown) {
          if (loadSequence === favoriteLoadSequence && get().favoriteUserId === userId) {
            set({
              favorites: [],
              error: error instanceof Error
                ? error.message
                : '즐겨찾기를 불러오지 못했습니다.'
            });
          }
        }
      },
      clearFavorites: () => {
        favoriteLoadSequence += 1;
        set({ favorites: [], favoriteUserId: null, favoritesOnly: false });
      },
      toggleFavorite: async (id) => {
        const { favoriteUserId, favorites } = get();
        if (favoriteUserId === null) {
          return;
        }

        const mutationKey = `${favoriteUserId}:${id}`;
        if (pendingFavoriteMutations.has(mutationKey)) {
          return;
        }

        const wasFavorite = favorites.includes(id);
        pendingFavoriteMutations.add(mutationKey);
        set((state) => ({
          favorites: wasFavorite
            ? state.favorites.filter((favoriteId) => favoriteId !== id)
            : state.favorites.includes(id) ? state.favorites : [...state.favorites, id]
        }));

        try {
          if (wasFavorite) {
            await removeFavoriteRequest(id);
          } else {
            await addFavoriteRequest(id);
          }
        } catch (error: unknown) {
          if (get().favoriteUserId === favoriteUserId) {
            set((state) => ({
              favorites: wasFavorite
                ? state.favorites.includes(id) ? state.favorites : [...state.favorites, id]
                : state.favorites.filter((favoriteId) => favoriteId !== id),
              error: error instanceof Error
                ? error.message
                : '즐겨찾기를 변경하지 못했습니다.'
            }));
          }
        } finally {
          pendingFavoriteMutations.delete(mutationKey);
        }
      },
      setSelectedId: (selectedId) => set({ selectedId }),
      toggleMobileMenu: () => set((state) => ({ mobileMenuOpen: !state.mobileMenuOpen })),
      closeMobileMenu: () => set({ mobileMenuOpen: false }),
      setAddressLocation: (userId, location, address) => {
        activeSearchSequence += 1;
        activeSearchKey = null;
        set({
          location,
          locationLabel: `주소 검색 · ${address}`,
          locationMode: 'ADDRESS',
          locationReady: true,
          locationAttempted: true,
          accountUserId: userId,
          locating: false,
          response: null,
          error: '',
          selectedId: null
        });
      },
      setAccountLocation: (userId, location, address, force = false) => {
        const locationLabel = `내 주소 · ${address}`;
        const current = get();
        if (!force
          && current.accountUserId === userId
          && current.locationMode === 'ADDRESS'
          && current.locationReady) {
          return;
        }
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
          locationMode: 'ACCOUNT',
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
          operatingSchedule,
          openNowOnly,
          resultSize
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
          operatingSchedule,
          openNowOnly,
          resultSize
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
            openNowOnly,
            size: resultSize
          });
          if (searchSequence !== activeSearchSequence) {
            return;
          }
          set({ response });
        } catch (error: unknown) {
          if (searchSequence !== activeSearchSequence) {
            return;
          }
          set({
            response: null,
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
          locationMode: hadReadyLocation ? current.locationMode : 'INITIAL',
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
            locationMode: 'CURRENT',
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
}));
