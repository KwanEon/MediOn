import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import {
  AtSign,
  Check,
  CheckCircle2,
  LogOut,
  Mail,
  MapPin,
  Pencil,
  Phone,
  RefreshCw,
  Save,
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
  const storeError = useAuthStore((state) => state.error);
  const clearError = useAuthStore((state) => state.clearError);
  const logout = useAuthStore((state) => state.logout);
  const updateProfile = useAuthStore((state) => state.updateProfile);
  const setFavoritesOnly = useMedicalSearchStore((state) => state.setFavoritesOnly);
  const setAccountLocation = useMedicalSearchStore((state) => state.setAccountLocation);
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [addressQuery, setAddressQuery] = useState('');
  const [selectedAddress, setSelectedAddress] = useState('');
  const [addressResults, setAddressResults] = useState<AddressSearchResult[]>([]);
  const [addressSearching, setAddressSearching] = useState(false);
  const [addressSearchError, setAddressSearchError] = useState('');
  const [success, setSuccess] = useState('');
  const [editing, setEditing] = useState(false);
  const [logoutModalOpen, setLogoutModalOpen] = useState(false);

  useEffect(() => {
    if (!user) return;
    setName(user.name);
    setEmail(user.email);
    setAddressQuery(user.address);
    setSelectedAddress(user.address);
    setAddressResults([]);
    setAddressSearchError('');
  }, [user]);

  if (initialized && !user) {
    return <Navigate to="/login" replace />;
  }
  if (!user) {
    return <main className="main-content profile-page"><p>로그인 정보를 확인하고 있습니다.</p></main>;
  }

  function resetProfileEditor() {
    if (!user) return;
    setName(user.name);
    setEmail(user.email);
    setAddressQuery(user.address);
    setSelectedAddress(user.address);
    setAddressResults([]);
    setAddressSearchError('');
  }

  function startEditing() {
    resetProfileEditor();
    setSuccess('');
    clearError();
    setEditing(true);
  }

  function cancelEditing() {
    resetProfileEditor();
    setSuccess('');
    clearError();
    setEditing(false);
  }

  function changeAddressQuery(value: string) {
    setAddressQuery(value);
    setSelectedAddress('');
    setAddressResults([]);
    setAddressSearchError('');
    setSuccess('');
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

  async function saveProfile(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!selectedAddress || selectedAddress !== addressQuery) {
      setAddressSearchError('주소를 검색한 뒤 목록에서 정확한 주소를 선택해 주세요.');
      return;
    }

    setSuccess('');
    try {
      const updatedUser = await updateProfile({
        name: name.trim(),
        email: email.trim(),
        address: selectedAddress
      });
      setAccountLocation(
        updatedUser.id,
        { lat: updatedUser.latitude, lng: updatedUser.longitude },
        updatedUser.address,
        true
      );
      setAddressResults([]);
      setAddressSearchError('');
      setSuccess('회원정보를 저장했습니다.');
      setEditing(false);
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
          <p>@{user.username}</p>
          <h1>회원 정보 관리</h1>
        </div>

        <dl className="profile-details">
          <div><dt><AtSign size={17} /> 아이디</dt><dd>{user.username}</dd></div>
          <div><dt><UserRound size={17} /> 이름</dt><dd>{user.name}</dd></div>
          <div><dt><Mail size={17} /> 이메일</dt><dd>{user.email}</dd></div>
          <div><dt><Phone size={17} /> 전화번호</dt><dd>{user.phoneNumber}</dd></div>
          <div><dt><MapPin size={17} /> 주소</dt><dd>{user.address}</dd></div>
        </dl>

        {success && !editing && (
          <div className="profile-save-success"><CheckCircle2 size={17} />{success}</div>
        )}

        {!editing && (
          <button
            className="profile-edit-button"
            type="button"
            onClick={startEditing}
            disabled={loading}
          >
            <Pencil size={17} /> 정보 수정
          </button>
        )}

        {editing && (
          <form className="profile-information-form" onSubmit={(event) => void saveProfile(event)}>
            <div className="profile-form-grid">
              <label>
                <span><UserRound size={15} /> 이름</span>
                <input
                  value={name}
                  onChange={(event) => {
                    setName(event.target.value);
                    setSuccess('');
                  }}
                  maxLength={50}
                  required
                  autoComplete="name"
                />
              </label>
              <label>
                <span><Mail size={15} /> 이메일</span>
                <input
                  type="email"
                  value={email}
                  onChange={(event) => {
                    setEmail(event.target.value);
                    setSuccess('');
                  }}
                  maxLength={150}
                  required
                  autoComplete="email"
                />
              </label>
            </div>

            <label>
              <span><MapPin size={15} /> 주소</span>
              <div className="address-search-row">
                <div className={addressSelected ? 'auth-input is-selected' : 'auth-input'}>
                  <MapPin size={18} />
                  <input
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
                    required
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
            </label>

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
            {storeError && <small className="address-search-error" role="alert">{storeError}</small>}

            <div className="profile-form-actions">
              <button
                className="profile-cancel-button"
                type="button"
                onClick={cancelEditing}
                disabled={loading || addressSearching}
              >
                <X size={17} /> 취소
              </button>
              <button
                className="profile-save-button"
                type="submit"
                disabled={loading || !addressSelected}
              >
                {loading ? <RefreshCw className="spin" size={17} /> : <Save size={17} />}
                {loading ? '저장 중' : '회원정보 저장'}
              </button>
            </div>
          </form>
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
