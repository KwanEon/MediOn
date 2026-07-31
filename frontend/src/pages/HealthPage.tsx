import {
  ArrowRight,
  Building2,
  CalendarClock,
  ClipboardCheck,
  ExternalLink,
  HeartPulse,
  Hospital,
  Pill,
  Stethoscope
} from 'lucide-react';
import { Link } from 'react-router-dom';

const HEALTH_GUIDES = [
  {
    icon: Stethoscope,
    category: '진료과 찾기',
    title: '증상에 맞는 진료과 고르기',
    description: '어느 진료과를 선택해야 할지 막막할 때 참고할 수 있는 기본 안내입니다.',
    items: [
      '기침·콧물·인후통은 내과 또는 이비인후과',
      '뼈·관절·근육의 통증은 정형외과',
      '피부 발진이나 가려움은 피부과',
      '영유아와 청소년의 증상은 소아청소년과'
    ],
    link: {
      label: '전체 보기',
      to: '/health/departments'
    },
    tone: 'blue'
  },
  {
    icon: Building2,
    category: '의료기관 구분',
    title: '의원·병원·종합병원의 차이',
    description: '가벼운 증상부터 입원이 필요한 경우까지 의료기관의 역할을 구분해 보세요.',
    items: [
      '의원은 외래 진료를 중심으로 운영됩니다.',
      '병원은 30개 이상의 병상을 갖춘 입원 진료 기관입니다.',
      '종합병원은 100개 이상의 병상과 여러 진료과를 갖춥니다.'
    ],
    source: {
      label: '건강보험심사평가원 제도 안내',
      href: 'https://www.hira.or.kr/dummy.do?WT.ac=&cmsurl=%2Fcms%2Fpolicy%2F02%2F01%2F1341852_27024.html&pgmid=HIRAA020006000000'
    },
    tone: 'green'
  },
  {
    icon: CalendarClock,
    category: '야간·휴일 진료',
    title: '운영시간을 한 번 더 확인하세요',
    description: '공휴일과 임시 휴진 등으로 실제 운영시간이 달라질 수 있습니다.',
    items: [
      '검색 결과에서 오늘의 운영시간을 확인하세요.',
      '접수 마감은 진료 종료보다 빠를 수 있습니다.',
      '출발 전 의료기관에 전화하면 헛걸음을 줄일 수 있습니다.'
    ],
    tone: 'amber'
  },
  {
    icon: ClipboardCheck,
    category: '방문 준비',
    title: '진료 전에 준비하면 좋은 것',
    description: '짧은 진료 시간에도 필요한 내용을 빠짐없이 전달할 수 있도록 준비해 보세요.',
    items: [
      '신분증과 복용 중인 약의 이름',
      '약물·음식 알레르기 여부',
      '증상이 시작된 시점과 달라진 과정',
      '이전에 받은 검사 결과나 처방전'
    ],
    tone: 'violet'
  },
  {
    icon: Pill,
    category: '약국 이용',
    title: '약국 방문 전 확인 사항',
    description: '조제 가능 여부와 운영시간은 약국마다 다를 수 있습니다.',
    items: [
      '처방전의 사용기간을 먼저 확인하세요.',
      '복용 중인 약과 알레르기를 약사에게 알려주세요.',
      '야간·휴일에는 방문 전에 운영 여부를 확인하세요.'
    ],
    tone: 'mint'
  },
  {
    icon: Hospital,
    category: '응급의료',
    title: '응급실이 필요한지 판단하기 어렵다면',
    description: '증상을 임의로 판단해 이동하기보다 전문적인 안내를 먼저 받는 것이 안전합니다.',
    items: [
      '의식 저하, 호흡 곤란, 심한 가슴 통증 등 위급한 상황에는 119에 도움을 요청하세요.',
      '가까운 응급실과 병·의원은 중앙응급의료센터 안내에서도 확인할 수 있습니다.'
    ],
    source: {
      label: '중앙응급의료센터 응급의료 안내',
      href: 'https://www.e-gen.or.kr/egen/notice_view.do?brdclscd=02&brdctsno=12899&currentPageNum=7024&searchDatayear=&searchKeyword=&searchTarget=ALL&upperfixyn=Y'
    },
    tone: 'red'
  }
] as const;

function HealthPage() {
  return (
    <main className="main-content resource-page">
      <header className="content-hero">
        <div>
          <p className="content-eyebrow">건강 정보</p>
          <h1>병원을 찾기 전,<br />알아두면 좋은 정보</h1>
          <p className="content-summary">
            의료기관을 더 알맞게 선택하고 안전하게 방문할 수 있도록 꼭 필요한 내용만 정리했습니다.
          </p>
        </div>
        <div className="content-hero-symbol health" aria-hidden="true">
          <HeartPulse size={42} strokeWidth={1.8} />
        </div>
      </header>

      <section className="emergency-callout" aria-labelledby="emergency-title">
        <div className="emergency-callout-icon" aria-hidden="true">
          <HeartPulse size={24} />
        </div>
        <div>
          <p>위급한 상황인가요?</p>
          <h2 id="emergency-title">의식 저하, 호흡 곤란 등 긴급한 증상은 즉시 119에 연락하세요.</h2>
          <span>이 페이지의 내용은 일반적인 정보이며 의료진의 진단을 대신하지 않습니다.</span>
        </div>
        <Link to="/emergency">
          주변 응급실 찾기
          <ArrowRight size={17} />
        </Link>
      </section>

      <section className="content-section" aria-labelledby="health-guide-title">
        <div className="section-heading">
          <div>
            <p>메디온 건강 가이드</p>
            <h2 id="health-guide-title">상황별로 확인해 보세요</h2>
          </div>
          <span>최근 검토 2026.07.28</span>
        </div>

        <div className="health-guide-grid">
          {HEALTH_GUIDES.map((guide) => {
            const Icon = guide.icon;

            return (
              <article className="health-guide-card" key={guide.title}>
                <div className={`guide-icon ${guide.tone}`} aria-hidden="true">
                  <Icon size={23} />
                </div>
                <p>{guide.category}</p>
                <h3>{guide.title}</h3>
                <span>{guide.description}</span>
                <ul>
                  {guide.items.map((item) => <li key={item}>{item}</li>)}
                </ul>
                {'link' in guide && guide.link ? (
                  <Link to={guide.link.to}>
                    {guide.link.label}
                    <ArrowRight size={14} />
                  </Link>
                ) : null}
                {'source' in guide && guide.source ? (
                  <a href={guide.source.href} target="_blank" rel="noreferrer">
                    {guide.source.label}
                    <ExternalLink size={14} />
                  </a>
                ) : null}
              </article>
            );
          })}
        </div>
      </section>

      <aside className="information-footnote">
        <strong>정보 이용 안내</strong>
        <p>
          의료기관의 진료과목과 운영시간은 현장 사정에 따라 달라질 수 있습니다.
          방문 전 상세정보와 전화 문의를 통해 다시 확인해 주세요.
        </p>
      </aside>
    </main>
  );
}

export default HealthPage;
