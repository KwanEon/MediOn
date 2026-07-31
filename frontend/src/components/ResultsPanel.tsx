import { useEffect, useState, type FormEvent } from "react";
import {
  ChevronLeft,
  ChevronRight,
  ListChecks,
  Navigation,
  RefreshCw,
  Search,
} from "lucide-react";
import InstitutionCard from "./InstitutionCard";
import { useMedicalSearchStore } from "../store/useMedicalSearchStore";
import type { NearbyInstitution } from "../types/institution";

interface ResultsPanelProps {
  visibleInstitutions: NearbyInstitution[];
}

function ResultsPanel({ visibleInstitutions }: ResultsPanelProps) {
  const favorites = useMedicalSearchStore((state) => state.favorites);
  const selectedId = useMedicalSearchStore((state) => state.selectedId);
  const loading = useMedicalSearchStore((state) => state.loading);
  const response = useMedicalSearchStore((state) => state.response);
  const toggleFavorite = useMedicalSearchStore((state) => state.toggleFavorite);
  const setSelectedId = useMedicalSearchStore((state) => state.setSelectedId);
  const setPageNumber = useMedicalSearchStore((state) => state.setPageNumber);

  const displayedInstitutions = loading ? [] : visibleInstitutions;
  const totalElements = response?.page.totalElements ?? 0;
  const currentPage = response?.page.number ?? 0;
  const totalPages = response?.page.totalPages ?? 0;
  const responseRequestedAt = response?.requestedAt;
  const counts = response?.typeCounts ?? {
    HOSPITAL: 0,
    PHARMACY: 0,
    EMERGENCY_ROOM: 0,
  };
  const [pageInput, setPageInput] = useState("1");

  useEffect(() => {
    setPageInput(String(currentPage + 1));
  }, [currentPage, responseRequestedAt]);

  const submitPage = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const requestedPage = Number(pageInput);
    if (!Number.isInteger(requestedPage) || totalPages < 1) {
      setPageInput(String(currentPage + 1));
      return;
    }

    const targetPage = Math.min(Math.max(requestedPage, 1), totalPages);
    setPageInput(String(targetPage));
    if (targetPage - 1 !== currentPage) {
      setPageNumber(targetPage - 1);
    }
  };

  return (
    <section className="results-panel" aria-labelledby="result-title">
      <div className="result-summary">
        <div className="summary-location-icon">
          <Navigation size={23} />
        </div>
        <div>
          <p id="result-title">의료기관 목록은 거리순으로 출력됩니다.</p>
          <div className="summary-counts">
            <span>
              병원 <b className="hospital-text">{counts.HOSPITAL}</b>
            </span>
            <span>
              약국 <b className="pharmacy-text">{counts.PHARMACY}</b>
            </span>
            <span>
              응급실 <b className="emergency-text">{counts.EMERGENCY_ROOM}</b>
            </span>
          </div>
          <div className="data-attribution">
            병원:{" "}
            <a
              href="https://www.data.go.kr/data/15000736/openapi.do"
              target="_blank"
              rel="noreferrer"
            >
              공공데이터포털
            </a>
            {" · "}약국:{" "}
            <a
              href="https://www.data.go.kr/data/15000576/openapi.do"
              target="_blank"
              rel="noreferrer"
            >
              공공데이터포털
            </a>
            {" · "}응급실:{" "}
            <a
              href="https://www.data.go.kr/data/15000563/openapi.do"
              target="_blank"
              rel="noreferrer"
            >
              국립중앙의료원
            </a>
            {" · "}지도:{" "}
            <a
              href="https://www.openstreetmap.org/copyright"
              target="_blank"
              rel="noreferrer"
            >
              © OpenStreetMap contributors
            </a>
          </div>
        </div>
        <div className="open-summary">
          <ListChecks size={22} />
          <span>
            검색 결과
            <strong>전체 {totalElements.toLocaleString("ko-KR")}곳</strong>
          </span>
        </div>
        <div className="distance-notice">
          <Navigation size={17} aria-hidden="true" />
          <span>
            표시된 거리는 직선거리 기준이며, 도보나 대중교통, 자가용 이용 시
            실제 이동 거리가 달라질 수 있습니다.
          </span>
        </div>
      </div>

      <div className="result-list" aria-busy={loading}>
        {loading && (
          <div className="loading-state">
            <RefreshCw className="spin" size={22} />
            주변 의료기관을 확인하고 있습니다.
          </div>
        )}

        {displayedInstitutions.map((institution) => (
          <InstitutionCard
            key={institution.id}
            institution={institution}
            favorite={favorites.includes(institution.id)}
            selected={selectedId === institution.id}
            onFavorite={() => toggleFavorite(institution.id)}
            onSelect={() => setSelectedId(institution.id)}
          />
        ))}

        {!loading && displayedInstitutions.length === 0 && (
          <div className="empty-state">
            <Search size={26} />
            <strong>조건에 맞는 의료기관이 없습니다.</strong>
            <span>검색어나 기관 유형을 변경해 보세요.</span>
          </div>
        )}
      </div>

      {totalPages > 1 && (
        <nav className="result-pagination" aria-label="검색 결과 페이지">
          <button
            type="button"
            disabled={loading || currentPage === 0}
            onClick={() => setPageNumber(currentPage - 1)}
          >
            <ChevronLeft size={18} />
            이전
          </button>
          <form className="page-jump" onSubmit={submitPage}>
            <input
              type="number"
              min={1}
              max={totalPages}
              value={pageInput}
              disabled={loading}
              aria-label="이동할 페이지"
              onChange={(event) => setPageInput(event.target.value)}
            />
            <span>/ {totalPages}</span>
            <button type="submit" disabled={loading || pageInput.length === 0}>
              이동
            </button>
          </form>
          <button
            type="button"
            disabled={loading || currentPage + 1 >= totalPages}
            onClick={() => setPageNumber(currentPage + 1)}
          >
            다음
            <ChevronRight size={18} />
          </button>
        </nav>
      )}
    </section>
  );
}

export default ResultsPanel;
