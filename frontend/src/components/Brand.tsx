import { Link } from 'react-router-dom';

interface BrandProps {
  compact?: boolean;
}

function Brand({ compact = false }: BrandProps) {
  if (compact) {
    return (
      <Link className="footer-brand" to="/" aria-label="메디온 홈">
        <span className="brand-mark small" aria-hidden="true"><span /></span>
        <strong>메디<span>온</span></strong>
      </Link>
    );
  }

  return (
    <Link className="brand" to="/" aria-label="메디온 홈">
      <span className="brand-mark" aria-hidden="true"><span /></span>
      <strong>메디<span>온</span></strong>
      <small>내 주변 의료기관 찾기</small>
    </Link>
  );
}

export default Brand;
