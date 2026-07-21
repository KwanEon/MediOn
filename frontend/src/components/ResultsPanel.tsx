import { ListChecks, Navigation, RefreshCw, Search } from 'lucide-react';
import InstitutionCard from './InstitutionCard';
import { useMedicalSearchStore } from '../store/useMedicalSearchStore';
import type { InstitutionType, NearbyInstitution } from '../types/institution';

interface ResultsPanelProps {
  institutions: NearbyInstitution[];
  visibleInstitutions: NearbyInstitution[];
}

function ResultsPanel({ institutions, visibleInstitutions }: ResultsPanelProps) {
  const radiusMeters = useMedicalSearchStore((state) => state.radiusMeters);
  const favorites = useMedicalSearchStore((state) => state.favorites);
  const selectedId = useMedicalSearchStore((state) => state.selectedId);
  const loading = useMedicalSearchStore((state) => state.loading);
  const toggleFavorite = useMedicalSearchStore((state) => state.toggleFavorite);
  const setSelectedId = useMedicalSearchStore((state) => state.setSelectedId);

  const counts: Record<InstitutionType, number> = {
    HOSPITAL: institutions.filter((item) => item.type === 'HOSPITAL').length,
    PHARMACY: institutions.filter((item) => item.type === 'PHARMACY').length,
    EMERGENCY_ROOM: institutions.filter((item) => item.type === 'EMERGENCY_ROOM').length
  };

  return (
    <section className="results-panel" aria-labelledby="result-title">
      <div className="result-summary">
        <div className="summary-location-icon"><Navigation size={23} /></div>
        <div>
          <p id="result-title">내 주변 {radiusMeters / 1000}km 내 의료기관</p>
          <div className="summary-counts">
            <span>병원 <b className="hospital-text">{counts.HOSPITAL}</b></span>
            <span>약국 <b className="pharmacy-text">{counts.PHARMACY}</b></span>
            <span>응급실 <b className="emergency-text">{counts.EMERGENCY_ROOM}</b></span>
          </div>
          <div className="data-attribution">
            병원: <a
              href="https://www.data.go.kr/data/15000736/openapi.do"
              target="_blank"
              rel="noreferrer"
            >공공데이터포털</a>
            {' · '}약국: <a
              href="https://www.data.go.kr/data/15000576/openapi.do"
              target="_blank"
              rel="noreferrer"
            >공공데이터포털</a>
            {' · '}응급실: <a
              href="https://www.data.go.kr/data/15000563/openapi.do"
              target="_blank"
              rel="noreferrer"
            >국립중앙의료원</a>
            {' · '}지도: <a
              href="https://www.openstreetmap.org/copyright"
              target="_blank"
              rel="noreferrer"
            >© OpenStreetMap contributors</a>
          </div>
        </div>
        <div className="open-summary">
          <ListChecks size={22} />
          <span>검색 결과<strong>{visibleInstitutions.length}곳 확인</strong></span>
        </div>
      </div>

      <div className="result-list" aria-busy={loading}>
        {loading && (
          <div className="loading-state">
            <RefreshCw className="spin" size={22} />
            주변 의료기관을 확인하고 있습니다.
          </div>
        )}

        {!loading && visibleInstitutions.map((institution) => (
          <InstitutionCard
            key={institution.id}
            institution={institution}
            favorite={favorites.includes(institution.id)}
            selected={selectedId === institution.id}
            onFavorite={() => toggleFavorite(institution.id)}
            onSelect={() => setSelectedId(institution.id)}
          />
        ))}

        {!loading && visibleInstitutions.length === 0 && (
          <div className="empty-state">
            <Search size={26} />
            <strong>조건에 맞는 의료기관이 없습니다.</strong>
            <span>검색어나 기관 유형을 변경해 보세요.</span>
          </div>
        )}
      </div>
    </section>
  );
}

export default ResultsPanel;
