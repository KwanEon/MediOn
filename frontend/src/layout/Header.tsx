import { useState } from 'react';
import { LogIn, LogOut, Menu, ShieldCheck, UserRound, X } from 'lucide-react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import Brand from '../components/Brand';
import ConfirmationModal from '../components/ConfirmationModal';
import { useAuthStore } from '../store/useAuthStore';
import { useMedicalSearchStore } from '../store/useMedicalSearchStore';

interface NavigationItem {
  to: string;
  label: string;
  end?: boolean;
}

const NAV_ITEMS: readonly NavigationItem[] = [
  { to: '/', label: '홈', end: true },
  { to: '/health/departments', label: '진료과 찾기' },
  { to: '/health', label: '건강 정보', end: true },
  { to: '/notices', label: '공지사항' },
  { to: '/guide', label: '이용 안내' },
  { to: '/inquiry', label: '문의하기' }
];

function Header() {
  const navigate = useNavigate();
  const user = useAuthStore((state) => state.user);
  const authLoading = useAuthStore((state) => state.loading);
  const logout = useAuthStore((state) => state.logout);
  const mobileMenuOpen = useMedicalSearchStore((state) => state.mobileMenuOpen);
  const setFavoritesOnly = useMedicalSearchStore((state) => state.setFavoritesOnly);
  const toggleMobileMenu = useMedicalSearchStore((state) => state.toggleMobileMenu);
  const closeMobileMenu = useMedicalSearchStore((state) => state.closeMobileMenu);
  const [logoutModalOpen, setLogoutModalOpen] = useState(false);

  async function handleLogout() {
    await logout();
    setFavoritesOnly(false);
    closeMobileMenu();
    setLogoutModalOpen(false);
    navigate('/', { replace: true });
  }

  return (
    <header className="site-header">
      <div className="header-inner">
        <Brand />

        <nav className={mobileMenuOpen ? 'main-nav is-open' : 'main-nav'} aria-label="주요 메뉴">
          {NAV_ITEMS.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className={({ isActive }) => (isActive ? 'is-active' : undefined)}
              onClick={closeMobileMenu}
            >
              {item.label}
            </NavLink>
          ))}
        </nav>

        <div className="header-actions">
          {user ? (
            <>
              {user.role === 'DEVELOPER' && (
                <Link
                  className="developer-header-link"
                  to="/developer"
                  aria-label="개발자 대시보드"
                  title="개발자 대시보드"
                >
                  <ShieldCheck size={18} />
                  <span>개발자 대시보드</span>
                </Link>
              )}
              <Link className="account-link" to="/profile" aria-label="내 정보" title="내 정보">
                <UserRound size={19} />
                <span>{user.name}</span>
              </Link>
              <button
                className="header-logout-button"
                type="button"
                onClick={() => setLogoutModalOpen(true)}
                disabled={authLoading}
              >
                <LogOut size={18} />
                <span>로그아웃</span>
              </button>
            </>
          ) : (
            <Link className="header-login-button" to="/login">
              <LogIn size={18} />
              <span>로그인</span>
            </Link>
          )}
          <button
            className="icon-button mobile-menu-button"
            type="button"
            aria-label={mobileMenuOpen ? '메뉴 닫기' : '메뉴 열기'}
            onClick={toggleMobileMenu}
          >
            {mobileMenuOpen ? <X size={22} /> : <Menu size={22} />}
          </button>
        </div>
      </div>
      <ConfirmationModal
        open={logoutModalOpen}
        title="로그아웃"
        message="로그아웃 하시겠습니까?"
        confirmLabel="로그아웃"
        loading={authLoading}
        onCancel={() => setLogoutModalOpen(false)}
        onConfirm={() => void handleLogout()}
      />
    </header>
  );
}

export default Header;
