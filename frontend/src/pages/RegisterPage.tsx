import { useEffect, useState } from 'react';
import type { FormEvent, ReactNode } from 'react';
import { Check, Mail, MapPin, Phone, RefreshCw, Search, ShieldCheck, UserRound } from 'lucide-react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import Brand from '../components/Brand';
import { searchAddresses } from '../api/auth';
import { useAuthStore } from '../store/useAuthStore';
import type { AddressSearchResult, RegisterRequest } from '../types/auth';

const INITIAL_FORM: RegisterRequest = {
  username: '',
  password: '',
  name: '',
  email: '',
  phoneNumber: '',
  address: ''
};

function RegisterPage() {
  const navigate = useNavigate();
  const user = useAuthStore((state) => state.user);
  const loading = useAuthStore((state) => state.loading);
  const error = useAuthStore((state) => state.error);
  const register = useAuthStore((state) => state.register);
  const clearError = useAuthStore((state) => state.clearError);
  const [form, setForm] = useState(INITIAL_FORM);
  const [addressResults, setAddressResults] = useState<AddressSearchResult[]>([]);
  const [addressSearching, setAddressSearching] = useState(false);
  const [addressSearchError, setAddressSearchError] = useState('');
  const [selectedAddress, setSelectedAddress] = useState('');
  const addressSelected = selectedAddress.length > 0 && selectedAddress === form.address;

  useEffect(() => {
    clearError();
  }, [clearError]);

  if (user) {
    return <Navigate to="/" replace />;
  }

  function updateField(field: keyof RegisterRequest, value: string) {
    setForm((current) => ({ ...current, [field]: value }));
  }

  function updateAddress(value: string) {
    updateField('address', value);
    setSelectedAddress('');
    setAddressResults([]);
    setAddressSearchError('');
  }

  async function handleAddressSearch() {
    const query = form.address.trim();
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
    updateField('address', result.address);
    setSelectedAddress(result.address);
    setAddressResults([]);
    setAddressSearchError('');
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!selectedAddress || selectedAddress !== form.address) {
      setAddressSearchError('주소를 검색한 뒤 목록에서 정확한 주소를 선택해 주세요.');
      return;
    }
    try {
      await register(form);
      navigate('/login', { replace: true, state: { registered: true } });
    } catch {
      // The authentication store exposes the user-facing error message.
    }
  }

  return (
    <main className="auth-page register-page">
      <div className="auth-brand"><Brand showTagline={false} /></div>
      <section className="auth-card register-card" aria-labelledby="register-title">
        <div className="auth-heading">
          <span><ShieldCheck size={22} /></span>
          <div>
            <p>새 계정 만들기</p>
            <h1 id="register-title">회원가입</h1>
          </div>
        </div>
        <p className="auth-description">입력한 주소는 로그인 후 주변 의료기관 검색 기준으로 사용됩니다.</p>
        {error && <div className="auth-error" role="alert">{error}</div>}

        <form className="auth-form register-form" onSubmit={handleSubmit}>
          <AuthField label="아이디" icon={<UserRound size={18} />}>
            <input value={form.username} onChange={(event) => updateField('username', event.target.value)} autoComplete="username" minLength={4} maxLength={30} required placeholder="영문, 숫자, 밑줄 4~30자" />
          </AuthField>
          <AuthField label="비밀번호" icon={<ShieldCheck size={18} />}>
            <input type="password" value={form.password} onChange={(event) => updateField('password', event.target.value)} autoComplete="new-password" minLength={8} maxLength={72} required placeholder="8자 이상 입력해 주세요" />
          </AuthField>
          <AuthField label="이름" icon={<UserRound size={18} />}>
            <input value={form.name} onChange={(event) => updateField('name', event.target.value)} autoComplete="name" maxLength={50} required placeholder="이름을 입력해 주세요" />
          </AuthField>
          <AuthField label="이메일" icon={<Mail size={18} />}>
            <input type="email" value={form.email} onChange={(event) => updateField('email', event.target.value)} autoComplete="email" maxLength={150} required placeholder="example@email.com" />
          </AuthField>
          <AuthField label="전화번호" icon={<Phone size={18} />}>
            <input type="tel" value={form.phoneNumber} onChange={(event) => updateField('phoneNumber', event.target.value)} autoComplete="tel" maxLength={30} required placeholder="010-1234-5678" />
          </AuthField>
          <div className="auth-field is-wide address-search-field">
            <label htmlFor="register-address">주소</label>
            <div className="address-search-row">
              <div className={addressSelected ? 'auth-input is-selected' : 'auth-input'}>
                <MapPin size={18} />
                <input
                  id="register-address"
                  value={form.address}
                  onChange={(event) => updateAddress(event.target.value)}
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
                disabled={addressSearching}
                onClick={() => void handleAddressSearch()}
              >
                {addressSearching ? <RefreshCw className="spin" size={17} /> : <Search size={17} />}
                {addressSearching ? '검색 중' : '주소 검색'}
              </button>
            </div>
            {addressSearchError && <small className="address-search-error" role="alert">{addressSearchError}</small>}
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
          </div>
          <button className="auth-primary-button register-submit" type="submit" disabled={loading}>
            {loading ? '주소 확인 및 가입 중...' : '회원가입 완료'}
          </button>
          <Link className="auth-text-link" to="/login">이미 계정이 있나요? 로그인</Link>
        </form>
      </section>
    </main>
  );
}

interface AuthFieldProps {
  label: string;
  icon: ReactNode;
  children: ReactNode;
  wide?: boolean;
}

function AuthField({ label, icon, children, wide = false }: AuthFieldProps) {
  return (
    <label className={wide ? 'auth-field is-wide' : 'auth-field'}>
      <span>{label}</span>
      <div className="auth-input">
        {icon}
        {children}
      </div>
    </label>
  );
}

export default RegisterPage;
