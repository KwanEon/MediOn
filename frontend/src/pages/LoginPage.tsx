import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { LockKeyhole, LogIn, UserRound } from 'lucide-react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import Brand from '../components/Brand';
import { useAuthStore } from '../store/useAuthStore';
import { useMedicalSearchStore } from '../store/useMedicalSearchStore';

function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const user = useAuthStore((state) => state.user);
  const loading = useAuthStore((state) => state.loading);
  const error = useAuthStore((state) => state.error);
  const login = useAuthStore((state) => state.login);
  const clearError = useAuthStore((state) => state.clearError);
  const setAccountLocation = useMedicalSearchStore((state) => state.setAccountLocation);
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const registered = Boolean((location.state as { registered?: boolean } | null)?.registered);

  useEffect(() => {
    clearError();
  }, [clearError]);

  if (user) {
    return <Navigate to={user.role === 'DEVELOPER' ? '/developer' : '/'} replace />;
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    try {
      const authenticatedUser = await login({ username, password });
      setAccountLocation(
        authenticatedUser.id,
        { lat: authenticatedUser.latitude, lng: authenticatedUser.longitude },
        authenticatedUser.address
      );
      navigate(authenticatedUser.role === 'DEVELOPER' ? '/developer' : '/', { replace: true });
    } catch {
      // The authentication store exposes the user-facing error message.
    }
  }

  return (
    <main className="auth-page">
      <div className="auth-brand"><Brand showTagline={false} /></div>
      <section className="auth-card" aria-labelledby="login-title">
        <div className="auth-heading">
          <span><LogIn size={22} /></span>
          <div>
            <p>메디온 계정</p>
            <h1 id="login-title">로그인</h1>
          </div>
        </div>
        <p className="auth-description">등록한 주소를 기준으로 가까운 의료기관을 찾아보세요.</p>
        {registered && <div className="auth-success">회원가입이 완료되었습니다. 로그인해 주세요.</div>}
        {error && <div className="auth-error" role="alert">{error}</div>}

        <form className="auth-form" onSubmit={handleSubmit}>
          <label>
            <span>아이디</span>
            <div className="auth-input">
              <UserRound size={18} />
              <input
                value={username}
                onChange={(event) => setUsername(event.target.value)}
                autoComplete="username"
                required
                placeholder="아이디를 입력해 주세요"
              />
            </div>
          </label>
          <label>
            <span>비밀번호</span>
            <div className="auth-input">
              <LockKeyhole size={18} />
              <input
                type="password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                autoComplete="current-password"
                required
                placeholder="비밀번호를 입력해 주세요"
              />
            </div>
          </label>
          <button className="auth-primary-button" type="submit" disabled={loading}>
            {loading ? '로그인 중...' : '로그인'}
          </button>
          <Link className="auth-secondary-button" to="/register">회원가입</Link>
        </form>
      </section>
    </main>
  );
}

export default LoginPage;
