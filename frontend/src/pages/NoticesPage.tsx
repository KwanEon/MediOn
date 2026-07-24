import { BellRing, ChevronDown, CircleAlert, Database, Megaphone, Sparkles } from 'lucide-react';

const NOTICES = [
  {
    category: '업데이트',
    title: '건강 정보와 이용 안내 콘텐츠를 새롭게 구성했습니다.',
    date: '2026.07.24',
    pinned: true,
    icon: Sparkles,
    content: [
      '건강 정보에서 의료기관 선택과 방문 준비에 필요한 내용을 확인할 수 있습니다.',
      '이용 안내에서 위치 검색, 필터, 즐겨찾기 사용법과 자주 묻는 질문을 확인할 수 있습니다.'
    ]
  },
  {
    category: '중요',
    title: '의료기관 운영시간은 방문 전에 다시 확인해 주세요.',
    date: '2026.07.24',
    pinned: true,
    icon: CircleAlert,
    content: [
      '메디온의 운영시간은 공공 의료데이터를 기반으로 제공됩니다.',
      '임시 휴진, 접수 조기 마감, 공휴일 운영 등 현장 상황이 즉시 반영되지 않을 수 있으므로 방문 전 해당 기관에 전화로 확인해 주세요.'
    ]
  },
  {
    category: '데이터',
    title: '응급실 병상 정보 이용 시 참고 사항을 안내합니다.',
    date: '2026.07.24',
    pinned: false,
    icon: Database,
    content: [
      '응급실 병상 정보는 관계 기관이 제공하는 데이터를 바탕으로 표시합니다.',
      '정보가 갱신되는 사이 실제 수용 가능 여부가 달라질 수 있으며, 위급한 상황에는 직접 이동하기보다 119의 안내를 받아 주세요.'
    ]
  },
  {
    category: '안내',
    title: '현재 위치를 사용할 수 없을 때는 주소 검색을 이용해 주세요.',
    date: '2026.07.24',
    pinned: false,
    icon: Megaphone,
    content: [
      '브라우저에서 위치 권한이 차단되었거나 현재 위치가 정확하지 않다면 검색창의 주소 검색 기능을 이용할 수 있습니다.',
      '로그인한 사용자는 내 정보에서 자주 사용하는 주소를 저장할 수 있습니다.'
    ]
  }
] as const;

function NoticesPage() {
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
          <span>총 {NOTICES.length}건</span>
        </div>

        <div className="notice-list">
          {NOTICES.map((notice, index) => {
            const Icon = notice.icon;

            return (
              <details className="notice-item" key={notice.title} open={index === 0}>
                <summary>
                  <div className={`notice-type ${notice.category === '중요' ? 'important' : ''}`}>
                    <Icon size={18} aria-hidden="true" />
                    <span>{notice.category}</span>
                  </div>
                  <div className="notice-title">
                    {notice.pinned ? <strong>중요</strong> : null}
                    <h3>{notice.title}</h3>
                  </div>
                  <time dateTime={notice.date.replaceAll('.', '-')}>{notice.date}</time>
                  <ChevronDown className="notice-chevron" size={19} aria-hidden="true" />
                </summary>
                <div className="notice-body">
                  {notice.content.map((paragraph) => <p key={paragraph}>{paragraph}</p>)}
                </div>
              </details>
            );
          })}
        </div>
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
