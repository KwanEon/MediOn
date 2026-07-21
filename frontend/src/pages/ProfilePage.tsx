import { LogOut, Mail, MapPin, Phone, UserRound } from 'lucide-react';
import { Navigate, useNavigate } from 'react-router-dom';
import { useAuthStore } from '../store/useAuthStore';
import { useMedicalSearchStore } from '../store/useMedicalSearchStore';

function ProfilePage() {
  const navigate = useNavigate();
  const user = useAuthStore((state) => state.user);
  const initialized = useAuthStore((state) => state.initialized);
  const loading = useAuthStore((state) => state.loading);
  const logout = useAuthStore((state) => state.logout);
  const setFavoritesOnly = useMedicalSearchStore((state) => state.setFavoritesOnly);

  if (initialized && !user) {
    return <Navigate to="/login" replace />;
  }
  if (!user) {
    return <main className="main-content profile-page"><p>로그인 정보를 확인하고 있습니다.</p></main>;
  }

  async function handleLogout() {
    await logout();
    setFavoritesOnly(false);
    navigate('/', { replace: true });
  }

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
          <div><dt><MapPin size={17} /> 검색 기준 주소</dt><dd>{user.address}</dd></div>
        </dl>
        <button className="profile-logout-button" type="button" onClick={handleLogout} disabled={loading}>
          <LogOut size={18} /> 로그아웃
        </button>
      </section>
    </main>
  );
}

export default ProfilePage;
