import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import MainLayout from '../layout/MainLayout';
import HomePage from '../pages/HomePage';
import InfoPage from '../pages/InfoPage';
import LoginPage from '../pages/LoginPage';
import ProfilePage from '../pages/ProfilePage';
import RegisterPage from '../pages/RegisterPage';

function RoutesSetup() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="login" element={<LoginPage />} />
        <Route path="register" element={<RegisterPage />} />
        <Route element={<MainLayout />}>
          <Route index element={<HomePage initialCategory="ALL" />} />
          <Route path="institutions" element={<HomePage initialCategory="ALL" />} />
          <Route path="emergency" element={<HomePage initialCategory="EMERGENCY_ROOM" />} />
          <Route
            path="health"
            element={<InfoPage eyebrow="건강 정보" title="건강 정보" description="공공 의료데이터와 신뢰할 수 있는 건강 정보를 준비하고 있습니다." />}
          />
          <Route
            path="notices"
            element={<InfoPage eyebrow="메디온 소식" title="공지사항" description="서비스 운영 및 의료데이터 갱신 소식을 안내합니다." />}
          />
          <Route
            path="guide"
            element={<InfoPage eyebrow="서비스 안내" title="이용 안내" description="위치 기반 의료기관 검색과 즐겨찾기 이용 방법을 안내합니다." />}
          />
          <Route path="profile" element={<ProfilePage />} />
          <Route
            path="privacy"
            element={<InfoPage eyebrow="정책" title="개인정보처리방침" description="위치 정보와 개인정보 처리 기준을 안내합니다." />}
          />
          <Route
            path="terms"
            element={<InfoPage eyebrow="정책" title="이용약관" description="메디온 서비스 이용약관을 안내합니다." />}
          />
          <Route
            path="sitemap"
            element={<InfoPage eyebrow="전체 메뉴" title="사이트맵" description="메디온의 주요 서비스를 한곳에서 확인할 수 있습니다." />}
          />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}

export default RoutesSetup;
