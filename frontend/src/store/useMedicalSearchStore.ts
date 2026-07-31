import { create } from 'zustand';
import {
  addFavorite as addFavoriteRequest,
  fetchFavorites,
  removeFavorite as removeFavoriteRequest
} from '../api/favorites';
import {
  fetchEmergencyBedAvailability,
  fetchNearbyInstitutions
} from '../api/institutions';
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
  pageNumber: number;
  keyword: string;
  submittedKeyword: string;
  favoritesOnly: boolean;
  favorites: InstitutionId[];
  favoriteUserId: number | null;
  loading: boolean;
  locating: boolean;
  locationReady: boolean;
  locationAttempted: boolean;
  searchRevision: number;
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
  setPageNumber: (pageNumber: number) => void;
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
let activeLocationRequestSequence = 0;
let activeSearchController: AbortController | null = null;
let activeEmergencyBedController: AbortController | null = null;
let favoriteLoadSequence = 0;
const pendingFavoriteMutations = new Set<string>();

function cancelActiveInstitutionRequests() {
  activeSearchSequence += 1;
  activeSearchKey = null;
  activeSearchController?.abort();
  activeEmergencyBedController?.abort();
  activeSearchController = null;
  activeEmergencyBedController = null;
}

function isAbortError(error: unknown) {
  return error instanceof DOMException && error.name === 'AbortError';
}

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
  pageNumber: 0,
  keyword: '',
  submittedKeyword: '',
  favoritesOnly: false,
  favorites: [],
  favoriteUserId: null,
  loading: false,
  locating: false,
  locationReady: false,
  locationAttempted: false,
  searchRevision: 0,
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
        pageNumber: 0,
        selectedId: null
      })),
      setSelectedHospitalDepartment: (selectedHospitalDepartment) => set({
        selectedHospitalDepartment,
        selectedCategory: 'HOSPITAL',
        pageNumber: 0,
        selectedId: null
      }),
      setRadiusMeters: (radiusMeters) => set({ radiusMeters, pageNumber: 0, selectedId: null }),
      setOperatingSchedule: (operatingSchedule) => set({
        operatingSchedule,
        pageNumber: 0,
        selectedId: null
      }),
      setOpenNowOnly: (openNowOnly) => set({ openNowOnly, pageNumber: 0, selectedId: null }),
      setPageNumber: (pageNumber) => set({ pageNumber, selectedId: null }),
      setKeyword: (keyword) => set({ keyword }),
      submitSearch: () => set((state) => ({
        submittedKeyword: state.keyword.trim(),
        pageNumber: 0,
        selectedId: null
      })),
      clearSearch: () => set({
        keyword: '',
        submittedKeyword: '',
        pageNumber: 0,
        selectedId: null
      }),
      setFavoritesOnly: (favoritesOnly) => set({
        favoritesOnly,
        pageNumber: 0,
        selectedId: null
      }),
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
        activeLocationRequestSequence += 1;
        cancelActiveInstitutionRequests();
        set((state) => ({
          location,
          locationLabel: `주소 검색 · ${address}`,
          locationMode: 'ADDRESS',
          locationReady: true,
          locationAttempted: true,
          accountUserId: userId,
          locating: false,
          loading: true,
          response: null,
          error: '',
          pageNumber: 0,
          selectedId: null,
          searchRevision: state.searchRevision + 1
        }));
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
        const isCurrentAccountLocation = current.accountUserId === userId
          && current.locationReady
          && current.location.lat === location.lat
          && current.location.lng === location.lng
          && current.locationLabel === locationLabel;
        if (isCurrentAccountLocation && !force) {
          return;
        }
        activeLocationRequestSequence += 1;
        cancelActiveInstitutionRequests();
        set((state) => ({
          location,
          locationLabel,
          locationMode: 'ACCOUNT',
          locationReady: true,
          locationAttempted: false,
          accountUserId: userId,
          locating: false,
          loading: true,
          response: null,
          error: '',
          pageNumber: 0,
          selectedId: null,
          searchRevision: state.searchRevision + 1
        }));
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
          submittedKeyword,
          pageNumber
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
          submittedKeyword,
          pageNumber
        ].join(':');
        if (get().loading && activeSearchKey === searchKey) {
          return;
        }

        activeSearchController?.abort();
        activeEmergencyBedController?.abort();
        const searchController = new AbortController();
        const searchSequence = ++activeSearchSequence;
        activeSearchKey = searchKey;
        activeSearchController = searchController;
        activeEmergencyBedController = null;
        set({ loading: true, error: '' });

        try {
          const response = await fetchNearbyInstitutions({
            ...location,
            radiusMeters,
            keyword: submittedKeyword,
            types,
            hospitalDepartment: selectedHospitalDepartment === 'ALL'
              ? undefined
              : selectedHospitalDepartment,
            operatingSchedule,
            openNowOnly,
            page: pageNumber,
            size: 30
          }, searchController.signal);
          if (searchSequence !== activeSearchSequence) {
            return;
          }
          const responseWithAvailabilityState: NearbyInstitutionResponse = {
            ...response,
            items: response.items.map((institution) => ({
              ...institution,
              emergencyBedAvailabilityLoading: institution.type === 'EMERGENCY_ROOM'
            }))
          };
          set({ response: responseWithAvailabilityState });

          const emergencyInstitutionIds = responseWithAvailabilityState.items
            .filter((institution) => institution.type === 'EMERGENCY_ROOM')
            .map((institution) => institution.id);
          if (emergencyInstitutionIds.length > 0) {
            const emergencyBedController = new AbortController();
            activeEmergencyBedController = emergencyBedController;
            void fetchEmergencyBedAvailability(
              emergencyInstitutionIds,
              emergencyBedController.signal
            ).then(({ availableBeds }) => {
              if (searchSequence !== activeSearchSequence
                || activeEmergencyBedController !== emergencyBedController) {
                return;
              }
              set((state) => {
                if (state.response !== responseWithAvailabilityState) {
                  return {};
                }
                return {
                  response: {
                    ...responseWithAvailabilityState,
                    items: responseWithAvailabilityState.items.map((institution) => {
                      if (institution.type !== 'EMERGENCY_ROOM') {
                        return institution;
                      }
                      const availableEmergencyBeds = availableBeds[String(institution.id)];
                      return {
                        ...institution,
                        emergencyBedAvailabilityLoading: false,
                        availableEmergencyBeds: typeof availableEmergencyBeds === 'number'
                          ? availableEmergencyBeds
                          : null
                      };
                    })
                  }
                };
              });
            }).catch((error: unknown) => {
              if (isAbortError(error)
                || searchSequence !== activeSearchSequence
                || activeEmergencyBedController !== emergencyBedController) {
                return;
              }
              set((state) => {
                if (state.response !== responseWithAvailabilityState) {
                  return {};
                }
                return {
                  response: {
                    ...responseWithAvailabilityState,
                    items: responseWithAvailabilityState.items.map((institution) => (
                      institution.type === 'EMERGENCY_ROOM'
                        ? {
                            ...institution,
                            emergencyBedAvailabilityLoading: false,
                            availableEmergencyBeds: null
                          }
                        : institution
                    ))
                  }
                };
              });
            }).finally(() => {
              if (activeEmergencyBedController === emergencyBedController) {
                activeEmergencyBedController = null;
              }
            });
          }
        } catch (error: unknown) {
          if (searchSequence !== activeSearchSequence || isAbortError(error)) {
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
          if (activeSearchController === searchController) {
            activeSearchController = null;
          }
          if (searchSequence === activeSearchSequence) {
            activeSearchKey = null;
            set({ loading: false });
          }
        }
      },
      requestCurrentLocation: () => {
        const locationRequestSequence = ++activeLocationRequestSequence;
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
          (position) => {
            if (locationRequestSequence !== activeLocationRequestSequence) {
              return;
            }
            cancelActiveInstitutionRequests();
            set((state) => ({
              location: {
                lat: position.coords.latitude,
                lng: position.coords.longitude
              },
              locationLabel: '현재 위치',
              locationMode: 'CURRENT',
              locating: false,
              loading: true,
              locationReady: true,
              accountUserId: null,
              response: null,
              error: '',
              pageNumber: 0,
              selectedId: null,
              searchRevision: state.searchRevision + 1
            }));
          },
          () => {
            if (locationRequestSequence !== activeLocationRequestSequence) {
              return;
            }
            set({
              error: '위치 권한을 허용한 뒤 내 위치로 찾기 버튼을 다시 눌러 주세요.',
              locationLabel: hadReadyLocation ? get().locationLabel : '현재 위치 권한 필요',
              locating: false,
              selectedId: null
            });
          },
          { enableHighAccuracy: true, timeout: 8000, maximumAge: 60000 }
        );
  }
}));
