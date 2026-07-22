import { useEffect, useMemo } from 'react';
import FilterBar from '../components/FilterBar';
import MedicalMap from '../components/MedicalMap';
import ResultsPanel from '../components/ResultsPanel';
import SearchPanel from '../components/SearchPanel';
import StatusNotice from '../components/StatusNotice';
import { useAuthStore } from '../store/useAuthStore';
import { useMedicalSearchStore } from '../store/useMedicalSearchStore';
import type { CategoryId } from '../types/institution';

interface HomePageProps {
  initialCategory?: CategoryId;
}

function HomePage({ initialCategory = 'ALL' }: HomePageProps) {
  const location = useMedicalSearchStore((state) => state.location);
  const user = useAuthStore((state) => state.user);
  const authInitialized = useAuthStore((state) => state.initialized);
  const locationLabel = useMedicalSearchStore((state) => state.locationLabel);
  const response = useMedicalSearchStore((state) => state.response);
  const locating = useMedicalSearchStore((state) => state.locating);
  const locationReady = useMedicalSearchStore((state) => state.locationReady);
  const locationAttempted = useMedicalSearchStore((state) => state.locationAttempted);
  const selectedCategory = useMedicalSearchStore((state) => state.selectedCategory);
  const selectedHospitalDepartment = useMedicalSearchStore(
    (state) => state.selectedHospitalDepartment
  );
  const radiusMeters = useMedicalSearchStore((state) => state.radiusMeters);
  const operatingSchedule = useMedicalSearchStore((state) => state.operatingSchedule);
  const submittedKeyword = useMedicalSearchStore((state) => state.submittedKeyword);
  const favoritesOnly = useMedicalSearchStore((state) => state.favoritesOnly);
  const favorites = useMedicalSearchStore((state) => state.favorites);
  const selectedId = useMedicalSearchStore((state) => state.selectedId);
  const setSelectedCategory = useMedicalSearchStore((state) => state.setSelectedCategory);
  const setSelectedId = useMedicalSearchStore((state) => state.setSelectedId);
  const setFavoritesOnly = useMedicalSearchStore((state) => state.setFavoritesOnly);
  const setAccountLocation = useMedicalSearchStore((state) => state.setAccountLocation);
  const loadNearbyInstitutions = useMedicalSearchStore((state) => state.loadNearbyInstitutions);
  const requestCurrentLocation = useMedicalSearchStore((state) => state.requestCurrentLocation);

  const institutions = response?.items ?? [];
  const visibleInstitutions = useMemo(() => {
    const normalizedKeyword = submittedKeyword.trim().toLowerCase();
    const filteredInstitutions = institutions.filter((institution) => {
      const medicalDepartments = institution.medicalDepartments ?? [];
      const searchableText = `${institution.name} ${institution.roadAddress ?? ''} ${institution.institutionKind ?? ''} ${medicalDepartments.join(' ')}`.toLowerCase();
      const matchesKeyword = !normalizedKeyword || searchableText.includes(normalizedKeyword);
      const matchesFavorite = !favoritesOnly || favorites.includes(institution.id);
      return matchesKeyword && matchesFavorite;
    });

    return filteredInstitutions.sort(
      (first, second) => first.distanceMeters - second.distanceMeters
    );
  }, [
    favorites,
    favoritesOnly,
    institutions,
    submittedKeyword
  ]);

  useEffect(() => {
    setSelectedCategory(initialCategory);
  }, [initialCategory, setSelectedCategory]);

  useEffect(() => {
    if (!authInitialized || !user) {
      return;
    }
    setAccountLocation(
      user.id,
      { lat: user.latitude, lng: user.longitude },
      user.address
    );
  }, [authInitialized, setAccountLocation, user]);

  useEffect(() => {
    if (!authInitialized || user || locationAttempted) {
      return;
    }
    requestCurrentLocation();
  }, [authInitialized, locationAttempted, requestCurrentLocation, user]);

  useEffect(() => {
    if (!authInitialized || user || !favoritesOnly) {
      return;
    }
    setFavoritesOnly(false);
  }, [authInitialized, favoritesOnly, setFavoritesOnly, user]);

  useEffect(() => {
    if (!locationReady) {
      return;
    }
    void loadNearbyInstitutions();
  }, [
    loadNearbyInstitutions,
    location.lat,
    location.lng,
    locationReady,
    radiusMeters,
    selectedCategory,
    selectedHospitalDepartment,
    operatingSchedule
  ]);

  return (
    <main id="top" className="main-content">
      <SearchPanel />
      <FilterBar />
      <StatusNotice />
      <section id="finder" className="finder-layout">
        {locationReady ? (
          <MedicalMap
            location={location}
            institutions={visibleInstitutions}
            selectedId={selectedId}
            onSelect={setSelectedId}
          />
        ) : (
          <div className="map-panel map-location-pending" role="status" aria-live="polite">
            <strong>{locating ? '현재 위치를 확인하고 있습니다.' : '현재 위치 권한이 필요합니다.'}</strong>
            <span>{locating ? '위치를 확인하면 주변 의료기관을 불러옵니다.' : '내 위치로 찾기 버튼을 눌러 주세요.'}</span>
          </div>
        )}
        <ResultsPanel institutions={institutions} visibleInstitutions={visibleInstitutions} />
      </section>
    </main>
  );
}

export default HomePage;
