import { useEffect, useRef, useState } from 'react';
import type { FormEvent } from 'react';
import { Check, MapPin, RefreshCw, Search, TrainFront, X } from 'lucide-react';
import { searchAddresses } from '../api/auth';
import type { AddressSearchResult } from '../types/auth';

interface AddressSearchModalProps {
  open: boolean;
  onClose: () => void;
  onSelect: (address: AddressSearchResult) => void;
}

function AddressSearchModal({ open, onClose, onSelect }: AddressSearchModalProps) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<AddressSearchResult[]>([]);
  const [selectedAddress, setSelectedAddress] = useState<AddressSearchResult | null>(null);
  const [searching, setSearching] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!open) {
      return;
    }

    setQuery('');
    setResults([]);
    setSelectedAddress(null);
    setError('');
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    inputRef.current?.focus();

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        onClose();
      }
    }

    document.addEventListener('keydown', handleKeyDown);
    return () => {
      document.body.style.overflow = previousOverflow;
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [onClose, open]);

  function changeQuery(value: string) {
    setQuery(value);
    setResults([]);
    setSelectedAddress(null);
    setError('');
  }

  async function handleSearch(event?: FormEvent<HTMLFormElement>) {
    event?.preventDefault();
    const normalizedQuery = query.trim();
    if (normalizedQuery.length < 2) {
      setError('도로명, 지번 주소 또는 역 이름을 두 글자 이상 입력해 주세요.');
      setResults([]);
      return;
    }

    setSearching(true);
    setError('');
    setSelectedAddress(null);
    try {
      const addressResults = await searchAddresses(normalizedQuery, true);
      setResults(addressResults);
      if (addressResults.length === 0) {
        setError('검색 결과가 없습니다. 주소나 역 이름을 확인해 주세요.');
      }
    } catch (searchError: unknown) {
      setResults([]);
      setError(searchError instanceof Error ? searchError.message : '주소를 검색하지 못했습니다.');
    } finally {
      setSearching(false);
    }
  }

  function selectResult(result: AddressSearchResult) {
    setQuery(result.address);
    setSelectedAddress(result);
    setResults([]);
    setError('');
  }

  if (!open) {
    return null;
  }

  return (
    <div
      className="modal-backdrop"
      role="presentation"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget && !searching) {
          onClose();
        }
      }}
    >
      <section
        className="address-location-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="address-location-modal-title"
      >
        <button
          className="modal-close-button"
          type="button"
          aria-label="닫기"
          disabled={searching}
          onClick={onClose}
        >
          <X size={19} />
        </button>
        <div className="modal-heading">
          <span><MapPin size={23} /></span>
          <div>
            <h2 id="address-location-modal-title">주소 또는 역으로 의료기관 찾기</h2>
            <p>검색 기준으로 사용할 주소나 역을 선택해 주세요.</p>
          </div>
        </div>

        <form className="address-location-form" onSubmit={(event) => void handleSearch(event)}>
          <div className="address-search-row">
            <div className={selectedAddress ? 'auth-input is-selected' : 'auth-input'}>
              <MapPin size={18} />
              <input
                ref={inputRef}
                value={query}
                onChange={(event) => changeQuery(event.target.value)}
                autoComplete="off"
                maxLength={255}
                placeholder="도로명·지번 주소 또는 역 이름을 입력해 주세요"
              />
              {selectedAddress && <Check className="address-selected-icon" size={18} />}
            </div>
            <button className="address-search-button" type="submit" disabled={searching}>
              {searching ? <RefreshCw className="spin" size={17} /> : <Search size={17} />}
              {searching ? '검색 중' : '위치 검색'}
            </button>
          </div>
          {error && <small className="address-search-error" role="alert">{error}</small>}
          {results.length > 0 && (
            <div className="address-search-results" role="listbox" aria-label="주소 검색 결과">
              {results.map((result) => {
                const stationResult = result.kind === 'STATION';
                return (
                  <button
                    key={`${result.latitude}-${result.longitude}-${result.address}`}
                    type="button"
                    role="option"
                    aria-selected={false}
                    onClick={() => selectResult(result)}
                  >
                    {stationResult ? <TrainFront size={17} /> : <MapPin size={17} />}
                    <span>
                      <strong>{result.roadAddress ?? result.address}</strong>
                      {result.jibunAddress && result.jibunAddress !== result.roadAddress && (
                        <small>
                          {stationResult ? result.jibunAddress : `지번 ${result.jibunAddress}`}
                        </small>
                      )}
                    </span>
                  </button>
                );
              })}
            </div>
          )}
        </form>

        <div className="modal-actions">
          <button type="button" disabled={searching} onClick={onClose}>취소</button>
          <button
            className="is-primary"
            type="button"
            disabled={!selectedAddress || searching}
            onClick={() => selectedAddress && onSelect(selectedAddress)}
          >
            이 위치로 찾기
          </button>
        </div>
      </section>
    </div>
  );
}

export default AddressSearchModal;
