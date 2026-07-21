import { Link } from 'react-router-dom';
import Brand from '../components/Brand';

function Footer() {
  return (
    <footer className="site-footer">
      <div className="footer-inner">
        <Brand compact />
        <p>공공 의료데이터를 기반으로 주변 의료기관 정보를 제공합니다.</p>
        <nav aria-label="하단 메뉴">
          <Link to="/privacy">개인정보처리방침</Link>
          <Link to="/terms">이용약관</Link>
          <Link to="/sitemap">사이트맵</Link>
        </nav>
        <span>© 2026 메디온</span>
      </div>
    </footer>
  );
}

export default Footer;
