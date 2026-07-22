import { useCallback, useState } from 'react';
import type { FormEvent } from 'react';
import { LocateFixed, MapPinned, RefreshCw, Search, X } from 'lucide-react';
import { useAuthStore } from '../store/useAuthStore';
import { useMedicalSearchStore } from '../store/useMedicalSearchStore';
import type { AddressSearchResult } from '../types/auth';
import AddressSearchModal from './AddressSearchModal';

function SearchPanel() {
  const userId = useAuthStore((state) => state.user?.id ?? null);
  const keyword = useMedicalSearchStore((state) => state.keyword);
  const locationLabel = useMedicalSearchStore((state) => state.locationLabel);
  const locating = useMedicalSearchStore((state) => state.locating);
  const setKeyword = useMedicalSearchStore((state) => state.setKeyword);
  const submitSearch = useMedicalSearchStore((state) => state.submitSearch);
  const clearSearch = useMedicalSearchStore((state) => state.clearSearch);
  const requestCurrentLocation = useMedicalSearchStore((state) => state.requestCurrentLocation);
  const setAddressLocation = useMedicalSearchStore((state) => state.setAddressLocation);
  const [addressModalOpen, setAddressModalOpen] = useState(false);

  const closeAddressModal = useCallback(() => setAddressModalOpen(false), []);
  const selectSearchAddress = useCallback((result: AddressSearchResult) => {
    setAddressLocation(
      userId,
      { lat: result.latitude, lng: result.longitude },
      result.address
    );
    setAddressModalOpen(false);
  }, [setAddressLocation, userId]);

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    submitSearch();
  }

  return (
    <section className="intro-row" aria-labelledby="page-title">
      <div className="intro-copy">
        <p>{locationLabel}</p>
        <h1 id="page-title">주변 의료기관 찾기</h1>
        <span>우리집 주변 의료기관들을 거리순으로 확인하세요.</span>
      </div>

      <form className="search-controls" role="search" onSubmit={handleSubmit}>
        <div className="search-box">
          <Search className="search-leading-icon" size={22} aria-hidden="true" />
          <input
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            placeholder="병원명, 약국명, 주소로 검색"
            aria-label="의료기관 검색어"
          />
          {keyword && (
            <button className="clear-search" type="button" aria-label="검색어 지우기" onClick={clearSearch}>
              <X size={18} />
            </button>
          )}
          <button className="search-submit" type="submit">
            <Search size={19} aria-hidden="true" />
            <span>검색</span>
          </button>
        </div>
        <button className="location-search-button" type="button" onClick={requestCurrentLocation} disabled={locating}>
          {locating ? <RefreshCw className="spin" size={19} /> : <LocateFixed size={19} />}
          {locating ? '위치 확인 중' : '내 위치로 찾기'}
        </button>
        <button
          className="address-location-search-button"
          type="button"
          onClick={() => setAddressModalOpen(true)}
        >
          <MapPinned size={19} />
          주소로 찾기
        </button>
      </form>
      <AddressSearchModal
        open={addressModalOpen}
        onClose={closeAddressModal}
        onSelect={selectSearchAddress}
      />
    </section>
  );
}

export default SearchPanel;
