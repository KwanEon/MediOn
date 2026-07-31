import { useCallback, useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import {
  Activity,
  Building2,
  CalendarDays,
  ChevronLeft,
  ChevronRight,
  CircleCheck,
  Clock3,
  Database,
  Heart,
  Hospital,
  LayoutDashboard,
  Mail,
  MapPin,
  Megaphone,
  MessageCircleMore,
  Phone,
  Pill,
  Play,
  RefreshCw,
  Search,
  Server,
  ShieldCheck,
  Siren,
  TriangleAlert,
  UserPlus,
  UserCog,
  Users
} from 'lucide-react';
import { Navigate } from 'react-router-dom';
import {
  fetchDeveloperDashboard,
  fetchDeveloperUsers,
  triggerDeveloperSync
} from '../api/developer';
import { useAuthStore } from '../store/useAuthStore';
import type {
  DeveloperDashboard,
  DeveloperUserPage,
  ExternalServiceStatus,
  SyncHistory
} from '../types/developer';
import DeveloperInquiriesPanel from '../components/DeveloperInquiriesPanel';
import DeveloperNoticesPanel from '../components/DeveloperNoticesPanel';

const USER_PAGE_SIZE = 20;

type DeveloperTab = 'dashboard' | 'users' | 'notices' | 'inquiries';

const DEVELOPER_TABS = [
  { key: 'dashboard', label: '대시보드', icon: LayoutDashboard },
  { key: 'users', label: '회원 관리', icon: UserCog },
  { key: 'notices', label: '공지사항', icon: Megaphone },
  { key: 'inquiries', label: '문의 관리', icon: MessageCircleMore }
] as const;

function formatNumber(value: number) {
  return new Intl.NumberFormat('ko-KR').format(value);
}

function formatDateTime(value: string | null) {
  if (!value) return '기록 없음';
  return new Intl.DateTimeFormat('ko-KR', {
    dateStyle: 'medium',
    timeStyle: 'short'
  }).format(new Date(value));
}

function formatUptime(seconds: number) {
  const days = Math.floor(seconds / 86400);
  const hours = Math.floor((seconds % 86400) / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  if (days > 0) return `${days}일 ${hours}시간`;
  if (hours > 0) return `${hours}시간 ${minutes}분`;
  return `${Math.max(0, minutes)}분`;
}

function externalStatusLabel(status: ExternalServiceStatus['status']) {
  if (status === 'READY') return '연결 준비됨';
  if (status === 'DISABLED') return '비활성';
  return '설정 필요';
}

function syncTargetLabel(targetType: string) {
  const labels: Record<string, string> = {
    HOSPITAL: '병원',
    HOSPITAL_BASE: '병원 기본 정보',
    HOSPITAL_DEPARTMENT: '진료과목',
    PHARMACY: '약국'
  };
  return labels[targetType] ?? targetType;
}

function syncSourceLabel(sourceName: string) {
  if (sourceName.includes('PHARMACY')) return '국립중앙의료원 약국 데이터';
  if (sourceName.includes('NATIONAL_MEDICAL_CENTER')) return '국립중앙의료원 의료기관 데이터';
  return sourceName;
}

function DeveloperDashboardPage() {
  const user = useAuthStore((state) => state.user);
  const authInitialized = useAuthStore((state) => state.initialized);
  const [dashboard, setDashboard] = useState<DeveloperDashboard | null>(null);
  const [usersPage, setUsersPage] = useState<DeveloperUserPage | null>(null);
  const [loading, setLoading] = useState(true);
  const [usersLoading, setUsersLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [searchInput, setSearchInput] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [page, setPage] = useState(0);
  const [syncingTarget, setSyncingTarget] =
    useState<'hospitals' | 'pharmacies' | null>(null);
  const [activeTab, setActiveTab] = useState<DeveloperTab>('dashboard');

  const loadDashboard = useCallback(async () => {
    setDashboard(await fetchDeveloperDashboard());
  }, []);

  const loadUsers = useCallback(async (query: string, nextPage: number) => {
    setUsersLoading(true);
    try {
      setUsersPage(await fetchDeveloperUsers(query, nextPage, USER_PAGE_SIZE));
    } finally {
      setUsersLoading(false);
    }
  }, []);

  useEffect(() => {
    if (!authInitialized || user?.role !== 'DEVELOPER') return;
    setLoading(true);
    setError('');
    Promise.all([loadDashboard(), loadUsers(searchQuery, page)])
      .catch((loadError: unknown) => {
        setError(loadError instanceof Error ? loadError.message : '대시보드를 불러오지 못했습니다.');
      })
      .finally(() => setLoading(false));
  }, [authInitialized, loadDashboard, loadUsers, page, searchQuery, user?.role]);

  useEffect(() => {
    if (!dashboard?.syncState.hospitalSyncRunning
        && !dashboard?.syncState.pharmacySyncRunning) return;
    const timer = window.setInterval(() => {
      void loadDashboard().catch(() => undefined);
    }, 5000);
    return () => window.clearInterval(timer);
  }, [
    dashboard?.syncState.hospitalSyncRunning,
    dashboard?.syncState.pharmacySyncRunning,
    loadDashboard
  ]);

  if (!authInitialized) {
    return (
      <main className="main-content developer-page">
        <DashboardLoading label="계정을 확인하고 있습니다." />
      </main>
    );
  }
  if (!user) return <Navigate to="/login" replace />;
  if (user.role !== 'DEVELOPER') return <Navigate to="/" replace />;

  async function refreshAll() {
    setRefreshing(true);
    setError('');
    try {
      await Promise.all([loadDashboard(), loadUsers(searchQuery, page)]);
      setNotice('최신 운영 정보를 불러왔습니다.');
    } catch (refreshError: unknown) {
      setError(refreshError instanceof Error ? refreshError.message : '새로고침하지 못했습니다.');
    } finally {
      setRefreshing(false);
    }
  }

  async function triggerSync(target: 'hospitals' | 'pharmacies') {
    setSyncingTarget(target);
    setError('');
    setNotice('');
    try {
      const result = await triggerDeveloperSync(target);
      setNotice(result.message);
      await loadDashboard();
    } catch (syncError: unknown) {
      setError(syncError instanceof Error ? syncError.message : '동기화를 시작하지 못했습니다.');
      await loadDashboard().catch(() => undefined);
    } finally {
      setSyncingTarget(null);
    }
  }

  function submitSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setPage(0);
    setSearchQuery(searchInput.trim());
  }

  const metrics = dashboard?.metrics;
  const syncState = dashboard?.syncState;
  const userPageNumber = usersPage?.page.number ?? 0;
  const userTotalPages = usersPage?.page.totalPages ?? 0;

  return (
    <main className="main-content developer-page">
      <section className="developer-hero">
        <div>
          <span className="developer-eyebrow"><ShieldCheck size={15} /> Developer console</span>
          <h1>MediOn 운영 대시보드</h1>
          <p>의료기관 데이터와 외부 연동, 회원 현황을 한곳에서 확인하고 관리합니다.</p>
        </div>
        <div className="developer-hero-actions">
          <div className="developer-live-status">
            <span />
            <div>
              <strong>서비스 정상</strong>
              <small>버전 {dashboard?.applicationVersion ?? '확인 중'}</small>
            </div>
          </div>
          <button type="button" onClick={() => void refreshAll()} disabled={refreshing || loading}>
            <RefreshCw className={refreshing ? 'spin' : undefined} size={17} /> 최신 정보
          </button>
        </div>
      </section>

      <nav className="developer-tabs" aria-label="개발자 대시보드 메뉴">
        {DEVELOPER_TABS.map((tab) => {
          const Icon = tab.icon;
          return (
            <button
              key={tab.key}
              type="button"
              className={activeTab === tab.key ? 'is-active' : undefined}
              onClick={() => setActiveTab(tab.key)}
              aria-current={activeTab === tab.key ? 'page' : undefined}
            >
              <Icon size={18} />
              <span>{tab.label}</span>
            </button>
          );
        })}
      </nav>

      {error && <div className="developer-alert is-error"><TriangleAlert size={18} />{error}</div>}
      {notice && <div className="developer-alert is-success"><CircleCheck size={18} />{notice}</div>}

      {loading && !dashboard ? <DashboardLoading label="운영 정보를 불러오고 있습니다." /> : (
        <>
          {activeTab === 'dashboard' && (
            <>
              <section className="developer-overview-grid" aria-label="핵심 운영 지표">
            <MetricCard
              className="is-blue"
              icon={<Users size={20} />}
              label="전체 회원"
              value={metrics?.totalUsers ?? 0}
              detail={<><UserPlus size={14} /> 최근 7일 +{formatNumber(metrics?.newUsersLast7Days ?? 0)}</>}
            />
            <MetricCard
              className="is-green"
              icon={<Building2 size={20} />}
              label="활성 의료기관"
              value={metrics?.activeInstitutions ?? 0}
              detail={<><Database size={14} /> 최신 {formatDateTime(metrics?.latestInstitutionSync ?? null)}</>}
            />
            <MetricCard
              className="is-violet"
              icon={<Activity size={20} />}
              label="응급실 운영 기관"
              value={metrics?.emergencyRooms ?? 0}
              detail={<><Siren size={14} /> 실시간 병상 연동 대상</>}
            />
            <MetricCard
              className={(metrics?.staleInstitutions ?? 0) > 0 ? 'is-amber' : 'is-slate'}
              icon={<Clock3 size={20} />}
              label="48시간 이상 미갱신"
              value={metrics?.staleInstitutions ?? 0}
              detail={<><Server size={14} /> 서버 가동 {formatUptime(dashboard?.uptimeSeconds ?? 0)}</>}
            />
              </section>

              <section className="developer-split-grid">
            <article className="developer-panel">
              <PanelHeading eyebrow="Data inventory" title="의료기관 현황" note="활성 데이터 기준" />
              <div className="developer-institution-stats">
                <InstitutionStat icon={<Hospital size={19} />} tone="hospital" label="병원" value={metrics?.hospitals ?? 0} />
                <InstitutionStat icon={<Pill size={19} />} tone="pharmacy" label="약국" value={metrics?.pharmacies ?? 0} />
                <InstitutionStat icon={<Siren size={19} />} tone="emergency" label="응급실" value={metrics?.emergencyRooms ?? 0} />
                <InstitutionStat icon={<Database size={19} />} tone="inactive" label="비활성" value={metrics?.inactiveInstitutions ?? 0} />
              </div>
            </article>

            <article className="developer-panel">
              <PanelHeading eyebrow="External APIs" title="외부 서비스 연동" note="비밀키 값은 표시하지 않습니다" />
              <div className="developer-service-list">
                {dashboard?.externalServices.map((service) => (
                  <div key={service.key}>
                    <span className={`developer-service-dot is-${service.status.toLowerCase()}`} />
                    <p><strong>{service.name}</strong><small>{service.description}</small></p>
                    <em>{externalStatusLabel(service.status)}</em>
                  </div>
                ))}
              </div>
            </article>
              </section>

              <section className="developer-panel developer-sync-panel">
            <PanelHeading
              eyebrow="Data operations"
              title="공공데이터 동기화"
              note={syncState?.publicDataEnabled ? '공공데이터 연동 활성' : '공공데이터 연동 비활성'}
            />
            <div className="developer-sync-actions">
              <SyncAction
                tone="hospital"
                icon={<Hospital size={21} />}
                title="병원·진료과목 데이터"
                description="기관 기본 정보와 전체 진료과목 관계를 갱신합니다."
                running={Boolean(syncState?.hospitalSyncRunning)}
                disabled={!syncState?.publicDataEnabled || Boolean(syncingTarget)}
                onClick={() => void triggerSync('hospitals')}
              />
              <SyncAction
                tone="pharmacy"
                icon={<Pill size={21} />}
                title="약국 데이터"
                description="전국 약국 정보와 운영 일정을 갱신합니다."
                running={Boolean(syncState?.pharmacySyncRunning)}
                disabled={!syncState?.publicDataEnabled || Boolean(syncingTarget)}
                onClick={() => void triggerSync('pharmacies')}
              />
            </div>
              </section>

              <section className="developer-panel developer-history-panel">
            <PanelHeading
              eyebrow="Recent activity"
              title="최근 동기화 기록"
              note={`최근 ${dashboard?.recentSyncs.length ?? 0}건`}
            />
            <SyncHistoryList items={dashboard?.recentSyncs ?? []} />
              </section>
            </>
          )}

          {activeTab === 'users' && (
            <section className="developer-panel developer-users-panel">
            <div className="developer-users-heading">
              <div>
                <span>Member directory</span>
                <h2>사용자 정보</h2>
                <p>
                  개발자 {formatNumber(metrics?.developerUsers ?? 0)}명 포함,
                  총 {formatNumber(usersPage?.page.totalElements ?? 0)}명
                </p>
              </div>
              <form onSubmit={submitSearch}>
                <Search size={17} />
                <input
                  value={searchInput}
                  onChange={(event) => setSearchInput(event.target.value)}
                  placeholder="이름, 아이디, 이메일, 전화번호 검색"
                  aria-label="사용자 검색"
                />
                <button type="submit">검색</button>
              </form>
            </div>
            <div className={usersLoading ? 'developer-user-table is-loading' : 'developer-user-table'}>
              <div className="developer-user-table-header">
                <span>사용자</span><span>연락처</span><span>검색 기준 주소</span><span>활동</span>
              </div>
              {usersPage?.items.length ? usersPage.items.map((member) => (
                <article key={member.id} className="developer-user-row">
                  <div className="developer-user-identity">
                    <span>{member.name.slice(0, 1)}</span>
                    <p><strong>{member.name}</strong><small>@{member.username} · #{member.id}</small></p>
                    <em className={member.role === 'DEVELOPER' ? 'is-developer' : undefined}>
                      {member.role === 'DEVELOPER' ? '개발자' : '일반'}
                    </em>
                  </div>
                  <div className="developer-user-contact">
                    <span><Mail size={14} />{member.email}</span>
                    <span><Phone size={14} />{member.phoneNumber}</span>
                  </div>
                  <div className="developer-user-address">
                    <span><MapPin size={14} />{member.address}</span>
                  </div>
                  <div className="developer-user-activity">
                    <span><Heart size={14} />즐겨찾기 {formatNumber(member.favoriteCount)}</span>
                    <span><CalendarDays size={14} />{formatDateTime(member.createdAt)} 가입</span>
                    <span><Clock3 size={14} />{formatDateTime(member.updatedAt)} 수정</span>
                  </div>
                </article>
              )) : (
                <div className="developer-empty">
                  {usersLoading ? '사용자를 불러오고 있습니다.' : '검색된 사용자가 없습니다.'}
                </div>
              )}
            </div>
            <div className="developer-pagination">
              <span>
                {formatNumber(usersPage?.page.totalElements ?? 0)}명 중{' '}
                {userTotalPages === 0 ? 0 : userPageNumber + 1}/{userTotalPages}페이지
              </span>
              <div>
                <button
                  type="button"
                  aria-label="이전 페이지"
                  disabled={usersLoading || userPageNumber <= 0}
                  onClick={() => setPage((current) => Math.max(0, current - 1))}
                ><ChevronLeft size={18} /></button>
                <button
                  type="button"
                  aria-label="다음 페이지"
                  disabled={usersLoading || userPageNumber + 1 >= userTotalPages}
                  onClick={() => setPage((current) => current + 1)}
                ><ChevronRight size={18} /></button>
              </div>
            </div>
            </section>
          )}

          {activeTab === 'notices' && <DeveloperNoticesPanel />}
          {activeTab === 'inquiries' && <DeveloperInquiriesPanel />}
        </>
      )}
    </main>
  );
}

function DashboardLoading({ label }: { label: string }) {
  return (
    <div className="developer-loading"><RefreshCw className="spin" size={22} />{label}</div>
  );
}

function MetricCard({
  className,
  icon,
  label,
  value,
  detail
}: {
  className: string;
  icon: React.ReactNode;
  label: string;
  value: number;
  detail: React.ReactNode;
}) {
  return (
    <article className={`developer-overview-card ${className}`}>
      <span>{icon}</span>
      <div><small>{label}</small><strong>{formatNumber(value)}</strong></div>
      <p>{detail}</p>
    </article>
  );
}

function PanelHeading({ eyebrow, title, note }: { eyebrow: string; title: string; note: string }) {
  return (
    <div className="developer-panel-heading">
      <div><span>{eyebrow}</span><h2>{title}</h2></div>
      <small>{note}</small>
    </div>
  );
}

function InstitutionStat({
  icon,
  tone,
  label,
  value
}: {
  icon: React.ReactNode;
  tone: string;
  label: string;
  value: number;
}) {
  return (
    <div>
      <span className={`is-${tone}`}>{icon}</span>
      <p>{label}<strong>{formatNumber(value)}</strong></p>
    </div>
  );
}

function SyncAction({
  icon,
  tone,
  title,
  description,
  running,
  disabled,
  onClick
}: {
  icon: React.ReactNode;
  tone: string;
  title: string;
  description: string;
  running: boolean;
  disabled: boolean;
  onClick: () => void;
}) {
  return (
    <div>
      <span className={`is-${tone}`}>{icon}</span>
      <p><strong>{title}</strong><small>{description}</small></p>
      <button type="button" onClick={onClick} disabled={disabled || running}>
        {running ? <RefreshCw className="spin" size={16} /> : <Play size={16} />}
        {running ? '진행 중' : '지금 동기화'}
      </button>
    </div>
  );
}

function SyncHistoryList({ items }: { items: SyncHistory[] }) {
  if (items.length === 0) {
    return <div className="developer-empty">아직 동기화 기록이 없습니다.</div>;
  }
  return (
    <div className="developer-history-list">
      {items.map((history) => (
        <article key={history.id}>
          <span className={history.status === 'SUCCESS' ? 'is-success' : 'is-failed'}>
            {history.status === 'SUCCESS'
              ? <CircleCheck size={17} />
              : <TriangleAlert size={17} />}
          </span>
          <div>
            <strong>{syncTargetLabel(history.targetType)}</strong>
            <small>{syncSourceLabel(history.sourceName)}</small>
            {history.message && <p>{history.message}</p>}
          </div>
          <time dateTime={history.syncedAt}>{formatDateTime(history.syncedAt)}</time>
        </article>
      ))}
    </div>
  );
}

export default DeveloperDashboardPage;
