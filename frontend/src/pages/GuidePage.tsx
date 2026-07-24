import {
  ArrowRight,
  BellRing,
  ChevronDown,
  CircleHelp,
  Clock3,
  Database,
  Filter,
  HeartPulse,
  Info,
  LocateFixed,
  MapPinned,
  Search,
  ShieldCheck,
  Star,
  UserRound
} from 'lucide-react';
import { Link } from 'react-router-dom';

const GUIDE_STEPS = [
  {
    number: '01',
    icon: MapPinned,
    title: '검색 위치 정하기',
    description: '현재 위치를 허용하거나 원하는 주소를 직접 검색하세요.'
  },
  {
    number: '02',
    icon: Filter,
    title: '조건 선택하기',
    description: '기관 종류, 진료과, 운영 상태, 검색 반경을 선택하세요.'
  },
  {
    number: '03',
    icon: Search,
    title: '결과 비교하기',
    description: '거리와 운영시간을 목록과 지도에서 함께 확인하세요.'
  },
  {
    number: '04',
    icon: BellRing,
    title: '방문 전 확인하기',
    description: '상세정보를 살펴보고 해당 기관에 전화로 문의하세요.'
  }
] as const;

const FEATURE_GUIDES = [
  {
    icon: LocateFixed,
    title: '현재 위치·주소 검색',
    description: '위치 권한을 허용하면 현재 위치를 기준으로 검색합니다. 권한을 사용할 수 없을 때는 주소 검색으로 기준 위치를 바꿀 수 있습니다.'
  },
  {
    icon: Clock3,
    title: '진료 중 표시',
    description: '등록된 운영 일정과 현재 시간을 비교한 안내입니다. 접수 마감, 임시 휴진, 공휴일 일정은 실제 상황과 다를 수 있습니다.'
  },
  {
    icon: HeartPulse,
    title: '응급실 정보',
    description: '응급실을 선택하면 가까운 응급의료기관과 제공 가능한 병상 정보를 확인할 수 있습니다. 위급한 상황에는 119의 안내를 우선해 주세요.'
  },
  {
    icon: Star,
    title: '즐겨찾기',
    description: '로그인한 사용자는 자주 방문하는 병원과 약국을 저장할 수 있습니다. 즐겨찾기만 모아 보는 필터도 제공합니다.'
  },
  {
    icon: UserRound,
    title: '내 주소 저장',
    description: '내 정보에서 자주 검색하는 주소를 등록하면 다음 방문부터 해당 위치를 편리하게 사용할 수 있습니다.'
  },
  {
    icon: Database,
    title: '공공데이터 기준',
    description: '메디온은 공공 의료데이터를 기반으로 정보를 제공합니다. 데이터 갱신 시점과 현장 상황 사이에 차이가 생길 수 있습니다.'
  }
] as const;

const FAQS = [
  {
    question: '현재 위치가 정확하지 않게 표시돼요.',
    answer: '건물 내부나 브라우저의 위치 권한 설정에 따라 오차가 발생할 수 있습니다. 위치 권한을 다시 허용하거나 주소 검색으로 기준 위치를 직접 지정해 주세요.'
  },
  {
    question: '진료 중으로 표시되는데 문이 닫혀 있어요.',
    answer: '진료 중 표시는 등록된 운영 일정에 따른 안내입니다. 임시 휴진, 공휴일, 접수 조기 마감은 즉시 반영되지 않을 수 있으므로 방문 전 전화 확인을 권장합니다.'
  },
  {
    question: '검색 결과가 너무 적거나 나오지 않아요.',
    answer: '검색 반경을 넓히고 진료과나 운영 상태 필터를 일부 해제해 보세요. 검색 위치가 올바른지도 함께 확인해 주세요.'
  },
  {
    question: '응급실 병상이 있다고 표시되면 바로 이용할 수 있나요?',
    answer: '병상 정보는 갱신되는 동안 실제 수용 상황과 달라질 수 있습니다. 환자의 상태와 의료진·장비 상황도 함께 고려되므로 위급할 때는 119의 안내를 받아 주세요.'
  },
  {
    question: '즐겨찾기는 어떻게 사용할 수 있나요?',
    answer: '로그인 후 검색 결과의 별 아이콘을 누르면 저장됩니다. 검색 화면에서 즐겨찾기 필터를 켜면 저장한 기관만 모아 볼 수 있습니다.'
  },
  {
    question: '내 위치 정보가 계속 저장되나요?',
    answer: '현재 위치는 주변 의료기관을 검색하는 데 사용됩니다. 계정에 주소를 직접 등록하는 경우를 제외하고, 브라우저의 위치 권한은 기기와 브라우저 설정에서 관리할 수 있습니다.'
  }
] as const;

function GuidePage() {
  return (
    <main className="main-content resource-page">
      <header className="content-hero guide-hero">
        <div>
          <p className="content-eyebrow">서비스 안내</p>
          <h1>메디온 이용 안내</h1>
          <p className="content-summary">
            내 주변 의료기관을 찾는 순간부터 방문 전 확인까지, 필요한 기능을 순서대로 안내합니다.
          </p>
          <Link className="hero-primary-link" to="/">
            의료기관 찾아보기
            <ArrowRight size={17} />
          </Link>
        </div>
        <div className="content-hero-symbol guide" aria-hidden="true">
          <CircleHelp size={42} strokeWidth={1.8} />
        </div>
      </header>

      <section className="content-section" aria-labelledby="quick-guide-title">
        <div className="section-heading">
          <div>
            <p>빠른 시작</p>
            <h2 id="quick-guide-title">네 단계로 찾아보세요</h2>
          </div>
        </div>
        <ol className="guide-step-list">
          {GUIDE_STEPS.map((step) => {
            const Icon = step.icon;
            return (
              <li key={step.number}>
                <span className="guide-step-number">{step.number}</span>
                <div className="guide-step-icon" aria-hidden="true"><Icon size={23} /></div>
                <h3>{step.title}</h3>
                <p>{step.description}</p>
              </li>
            );
          })}
        </ol>
      </section>

      <section className="content-section" aria-labelledby="feature-guide-title">
        <div className="section-heading">
          <div>
            <p>기능별 안내</p>
            <h2 id="feature-guide-title">표시와 기능을 알아보세요</h2>
          </div>
        </div>
        <div className="feature-guide-grid">
          {FEATURE_GUIDES.map((guide) => {
            const Icon = guide.icon;
            return (
              <article key={guide.title}>
                <div className="feature-guide-icon" aria-hidden="true"><Icon size={22} /></div>
                <div>
                  <h3>{guide.title}</h3>
                  <p>{guide.description}</p>
                </div>
              </article>
            );
          })}
        </div>
      </section>

      <aside className="data-caution">
        <ShieldCheck size={25} aria-hidden="true" />
        <div>
          <strong>방문 전 마지막으로 확인해 주세요</strong>
          <p>운영시간과 응급실 병상 정보는 실제 현장과 차이가 있을 수 있습니다. 상세정보에서 전화번호를 확인해 방문 전에 문의하는 것이 가장 정확합니다.</p>
        </div>
      </aside>

      <section className="content-section faq-section" aria-labelledby="faq-title">
        <div className="section-heading">
          <div>
            <p>자주 묻는 질문</p>
            <h2 id="faq-title">도움이 더 필요하신가요?</h2>
          </div>
        </div>
        <div className="faq-list">
          {FAQS.map((faq) => (
            <details key={faq.question}>
              <summary>
                <span><Info size={18} aria-hidden="true" />{faq.question}</span>
                <ChevronDown size={19} aria-hidden="true" />
              </summary>
              <p>{faq.answer}</p>
            </details>
          ))}
        </div>
      </section>
    </main>
  );
}

export default GuidePage;
