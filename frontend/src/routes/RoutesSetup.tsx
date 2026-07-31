import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import DepartmentGuidePage from '../pages/DepartmentGuidePage';
import DeveloperDashboardPage from '../pages/DeveloperDashboardPage';
import GuidePage from '../pages/GuidePage';
import HealthPage from '../pages/HealthPage';
import MainLayout from '../layout/MainLayout';
import HomePage from '../pages/HomePage';
import InfoPage from '../pages/InfoPage';
import InquiryPage from '../pages/InquiryPage';
import LoginPage from '../pages/LoginPage';
import NoticesPage from '../pages/NoticesPage';
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
          <Route path="health" element={<HealthPage />} />
          <Route path="health/departments" element={<DepartmentGuidePage />} />
          <Route path="notices" element={<NoticesPage />} />
          <Route path="guide" element={<GuidePage />} />
          <Route path="inquiry" element={<InquiryPage />} />
          <Route path="profile" element={<ProfilePage />} />
          <Route path="developer" element={<DeveloperDashboardPage />} />
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
