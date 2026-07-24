import { useEffect, useState } from 'react';
import {
  Check,
  LogOut,
  Mail,
  MapPin,
  Pencil,
  Phone,
  RefreshCw,
  Search,
  UserRound,
  X
} from 'lucide-react';
import { Navigate, useNavigate } from 'react-router-dom';
import { searchAddresses } from '../api/auth';
import ConfirmationModal from '../components/ConfirmationModal';
import { useAuthStore } from '../store/useAuthStore';
import { useMedicalSearchStore } from '../store/useMedicalSearchStore';
import type { AddressSearchResult } from '../types/auth';

function ProfilePage() {
  const navigate = useNavigate();
  const user = useAuthStore((state) => state.user);
  const initialized = useAuthStore((state) => state.initialized);
  const loading = useAuthStore((state) => state.loading);
  const error = useAuthStore((state) => state.error);
  const logout = useAuthStore((state) => state.logout);
  const updateAddress = useAuthStore((state) => state.updateAddress);
  const setFavoritesOnly = useMedicalSearchStore((state) => state.setFavoritesOnly);
  const setAccountLocation = useMedicalSearchStore((state) => state.setAccountLocation);
  const [editingAddress, setEditingAddress] = useState(false);
  const [addressQuery, setAddressQuery] = useState('');
  const [addressResults, setAddressResults] = useState<AddressSearchResult[]>([]);
  const [addressSearching, setAddressSearching] = useState(false);
  const [addressSearchError, setAddressSearchError] = useState('');
  const [selectedAddress, setSelectedAddress] = useState('');
  const [logoutModalOpen, setLogoutModalOpen] = useState(false);

  useEffect(() => {
    if (!editingAddress && user) {
      setAddressQuery(user.address);
    }
  }, [editingAddress, user]);

  if (initialized && !user) {
    return <Navigate to="/login" replace />;
  }
  if (!user) {
    return <main className="main-content profile-page"><p>로그인 정보를 확인하고 있습니다.</p></main>;
  }

  function beginAddressEdit() {
    setAddressQuery(user.address);
    setSelectedAddress('');
    setAddressResults([]);
    setAddressSearchError('');
    setEditingAddress(true);
  }

  function cancelAddressEdit() {
    setAddressQuery(user.address);
    setSelectedAddress('');
    setAddressResults([]);
    setAddressSearchError('');
    setEditingAddress(false);
  }

  function changeAddressQuery(value: string) {
    setAddressQuery(value);
    setSelectedAddress('');
    setAddressResults([]);
    setAddressSearchError('');
  }

  async function handleAddressSearch() {
    const query = addressQuery.trim();
    if (query.length < 2) {
      setAddressSearchError('도로명이나 지번 주소를 두 글자 이상 입력해 주세요.');
      setAddressResults([]);
      return;
    }

    setAddressSearching(true);
    setAddressSearchError('');
    setSelectedAddress('');
    try {
      const results = await searchAddresses(query);
      setAddressResults(results);
      if (results.length === 0) {
        setAddressSearchError('검색 결과가 없습니다. 도로명과 건물번호를 확인해 주세요.');
      }
    } catch (searchError: unknown) {
      setAddressResults([]);
      setAddressSearchError(
        searchError instanceof Error ? searchError.message : '주소를 검색하지 못했습니다.'
      );
    } finally {
      setAddressSearching(false);
    }
  }

  function selectAddress(result: AddressSearchResult) {
    setAddressQuery(result.address);
    setSelectedAddress(result.address);
    setAddressResults([]);
    setAddressSearchError('');
  }

  async function saveAddress() {
    if (!selectedAddress || selectedAddress !== addressQuery) {
      setAddressSearchError('주소를 검색한 뒤 목록에서 정확한 주소를 선택해 주세요.');
      return;
    }

    try {
      const updatedUser = await updateAddress(selectedAddress);
      setAccountLocation(
        updatedUser.id,
        { lat: updatedUser.latitude, lng: updatedUser.longitude },
        updatedUser.address,
        true
      );
      setEditingAddress(false);
      setSelectedAddress('');
      setAddressResults([]);
    } catch {
      // The authentication store exposes the user-facing error message.
    }
  }

  async function handleLogout() {
    await logout();
    setFavoritesOnly(false);
    setLogoutModalOpen(false);
    navigate('/', { replace: true });
  }

  const addressSelected = selectedAddress.length > 0 && selectedAddress === addressQuery;

  return (
    <main className="main-content profile-page">
      <section className="profile-card">
        <div className="profile-avatar"><UserRound size={32} /></div>
        <div className="profile-heading">
          <p>{user.username}</p>
          <h1>{user.name}님</h1>
        </div>
        <dl className="profile-details">
          <div><dt><Mail size={17} /> 이메일</dt><dd>{user.email}</dd></div>
          <div><dt><Phone size={17} /> 전화번호</dt><dd>{user.phoneNumber}</dd></div>
          <div>
            <dt><MapPin size={17} /> 검색 기준 주소</dt>
            <dd className="profile-address-value">
              <span>{user.address}</span>
              <button type="button" onClick={beginAddressEdit} disabled={editingAddress || loading}>
                <Pencil size={14} /> 수정
              </button>
            </dd>
          </div>
        </dl>

        {editingAddress && (
          <div className="profile-address-editor">
            <label htmlFor="profile-address">변경할 주소</label>
            <div className="address-search-row">
              <div className={addressSelected ? 'auth-input is-selected' : 'auth-input'}>
                <MapPin size={18} />
                <input
                  id="profile-address"
                  value={addressQuery}
                  onChange={(event) => changeAddressQuery(event.target.value)}
                  onKeyDown={(event) => {
                    if (event.key === 'Enter') {
                      event.preventDefault();
                      void handleAddressSearch();
                    }
                  }}
                  autoComplete="street-address"
                  maxLength={255}
                  placeholder="도로명 또는 지번 주소를 입력해 주세요"
                />
                {addressSelected && <Check className="address-selected-icon" size={18} />}
              </div>
              <button
                className="address-search-button"
                type="button"
                disabled={addressSearching || loading}
                onClick={() => void handleAddressSearch()}
              >
                {addressSearching ? <RefreshCw className="spin" size={17} /> : <Search size={17} />}
                {addressSearching ? '검색 중' : '주소 검색'}
              </button>
            </div>
            {addressSearchError && (
              <small className="address-search-error" role="alert">{addressSearchError}</small>
            )}
            {addressResults.length > 0 && (
              <div className="address-search-results" role="listbox" aria-label="주소 검색 결과">
                {addressResults.map((result) => (
                  <button
                    key={`${result.latitude}-${result.longitude}-${result.address}`}
                    type="button"
                    role="option"
                    aria-selected={false}
                    onClick={() => selectAddress(result)}
                  >
                    <MapPin size={17} />
                    <span>
                      <strong>{result.roadAddress ?? result.address}</strong>
                      {result.jibunAddress && result.jibunAddress !== result.roadAddress && (
                        <small>지번 {result.jibunAddress}</small>
                      )}
                    </span>
                  </button>
                ))}
              </div>
            )}
            {error && <small className="address-search-error" role="alert">{error}</small>}
            <div className="profile-address-actions">
              <button type="button" onClick={cancelAddressEdit} disabled={loading}>
                <X size={16} /> 취소
              </button>
              <button
                className="is-primary"
                type="button"
                onClick={() => void saveAddress()}
                disabled={loading || !addressSelected}
              >
                <Check size={16} /> {loading ? '저장 중' : '주소 저장'}
              </button>
            </div>
          </div>
        )}

        <button
          className="profile-logout-button"
          type="button"
          onClick={() => setLogoutModalOpen(true)}
          disabled={loading}
        >
          <LogOut size={18} /> 로그아웃
        </button>
      </section>
      <ConfirmationModal
        open={logoutModalOpen}
        title="로그아웃"
        message="로그아웃 하시겠습니까?"
        confirmLabel="로그아웃"
        loading={loading}
        onCancel={() => setLogoutModalOpen(false)}
        onConfirm={() => void handleLogout()}
      />
    </main>
  );
}

export default ProfilePage;
