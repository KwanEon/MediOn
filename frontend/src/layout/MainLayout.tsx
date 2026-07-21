import { Outlet } from 'react-router-dom';
import Footer from './Footer';
import Header from './Header';

function MainLayout() {
  return (
    <div className="site-shell">
      <Header />
      <Outlet />
      <Footer />
    </div>
  );
}

export default MainLayout;
