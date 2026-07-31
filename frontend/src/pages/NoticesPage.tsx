import { useEffect, useState } from 'react';
import {
  BellRing,
  ChevronDown,
  CircleAlert,
  Database,
  Megaphone,
  RefreshCw,
  Sparkles
} from 'lucide-react';
import { fetchNotices } from '../api/content';
import type { NoticeCategory, Notice as NoticeItem } from '../types/content';

const CATEGORY_META: Record<NoticeCategory, {
  label: string;
  icon: typeof BellRing;
}> = {
  IMPORTANT: { label: '중요', icon: CircleAlert },
  UPDATE: { label: '업데이트', icon: Sparkles },
  DATA: { label: '데이터', icon: Database },
  GUIDE: { label: '안내', icon: Megaphone }
};

function formatNoticeDate(value: string) {
  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  }).format(new Date(value));
}

function NoticesPage() {
  const [notices, setNotices] = useState<NoticeItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    fetchNotices()
      .then(setNotices)
      .catch((loadError: unknown) => {
        setError(loadError instanceof Error ? loadError.message : '공지사항을 불러오지 못했습니다.');
      })
      .finally(() => setLoading(false));
  }, []);

  return (
    <main className="main-content resource-page">
      <header className="content-hero">
        <div>
          <p className="content-eyebrow">메디온 소식</p>
          <h1>공지사항</h1>
          <p className="content-summary">
            서비스 운영과 의료데이터 갱신에 관한 중요한 소식을 알려드립니다.
          </p>
        </div>
        <div className="content-hero-symbol notice" aria-hidden="true">
          <BellRing size={40} strokeWidth={1.8} />
        </div>
      </header>

      <section className="notice-board" aria-labelledby="notice-list-title">
        <div className="notice-board-head">
          <h2 id="notice-list-title">전체 공지</h2>
          <span>총 {notices.length}건</span>
        </div>

        {loading ? (
          <div className="content-loading"><RefreshCw className="spin" size={20} /> 공지를 불러오고 있습니다.</div>
        ) : error ? (
          <div className="content-error"><CircleAlert size={19} /> {error}</div>
        ) : notices.length === 0 ? (
          <div className="content-empty">등록된 공지사항이 없습니다.</div>
        ) : (
          <div className="notice-list">
            {notices.map((notice, index) => {
              const meta = CATEGORY_META[notice.category];
              const Icon = meta.icon;
              return (
                <details className="notice-item" key={notice.id} open={index === 0}>
                  <summary>
                    <div className={`notice-type ${notice.category === 'IMPORTANT' ? 'important' : ''}`}>
                      <Icon size={18} aria-hidden="true" />
                      <span>{meta.label}</span>
                    </div>
                    <div className="notice-title">
                      {notice.pinned ? <strong>중요</strong> : null}
                      <h3>{notice.title}</h3>
                    </div>
                    <time dateTime={notice.publishedAt}>{formatNoticeDate(notice.publishedAt)}</time>
                    <ChevronDown className="notice-chevron" size={19} aria-hidden="true" />
                  </summary>
                  <div className="notice-body">
                    {notice.content.split(/\n{2,}/).map((paragraph) => (
                      <p key={paragraph}>{paragraph}</p>
                    ))}
                  </div>
                </details>
              );
            })}
          </div>
        )}
      </section>

      <aside className="notice-help">
        <BellRing size={20} aria-hidden="true" />
        <div>
          <strong>중요 공지를 먼저 확인해 주세요.</strong>
          <p>데이터 제공 지연이나 서비스 점검처럼 이용에 영향을 주는 내용은 이곳에 안내합니다.</p>
        </div>
      </aside>
    </main>
  );
}

export default NoticesPage;
