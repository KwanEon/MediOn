import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import {
  CheckCircle2,
  ChevronDown,
  CircleHelp,
  Clock3,
  LogIn,
  MessageCircleMore,
  RefreshCw,
  Send,
  Trash2,
  TriangleAlert
} from 'lucide-react';
import { Link } from 'react-router-dom';
import { createInquiry, deleteMyInquiry, fetchMyInquiries } from '../api/content';
import ConfirmationModal from '../components/ConfirmationModal';
import { useAuthStore } from '../store/useAuthStore';
import type {
  Inquiry,
  InquiryCategory,
  InquiryStatus
} from '../types/content';

const CATEGORY_LABELS: Record<InquiryCategory, string> = {
  GENERAL: '서비스 이용',
  ACCOUNT: '계정',
  DATA: '의료데이터',
  ERROR: '오류 신고',
  OTHER: '기타'
};

const STATUS_LABELS: Record<InquiryStatus, string> = {
  RECEIVED: '접수',
  REVIEWING: '확인 중',
  ANSWERED: '답변 완료',
  CLOSED: '종료'
};

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat('ko-KR', {
    dateStyle: 'medium',
    timeStyle: 'short'
  }).format(new Date(value));
}

function InquiryPage() {
  const user = useAuthStore((state) => state.user);
  const authInitialized = useAuthStore((state) => state.initialized);
  const [category, setCategory] = useState<InquiryCategory>('GENERAL');
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [inquiries, setInquiries] = useState<Inquiry[]>([]);
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<Inquiry | null>(null);
  const [deleting, setDeleting] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  useEffect(() => {
    if (!user) {
      setInquiries([]);
      return;
    }
    setLoading(true);
    fetchMyInquiries()
      .then(setInquiries)
      .catch((loadError: unknown) => {
        setError(loadError instanceof Error ? loadError.message : '문의 내역을 불러오지 못했습니다.');
      })
      .finally(() => setLoading(false));
  }, [user]);

  async function submitInquiry(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitting(true);
    setError('');
    setSuccess('');
    try {
      const created = await createInquiry({ category, title, content });
      setInquiries((current) => [created, ...current]);
      setCategory('GENERAL');
      setTitle('');
      setContent('');
      setSuccess('문의가 접수되었습니다. 확인 후 처리하겠습니다.');
    } catch (submitError: unknown) {
      setError(submitError instanceof Error ? submitError.message : '문의를 접수하지 못했습니다.');
    } finally {
      setSubmitting(false);
    }
  }

  async function confirmDelete() {
    if (!deleteTarget) return;
    setDeleting(true);
    setError('');
    setSuccess('');
    try {
      await deleteMyInquiry(deleteTarget.id);
      setInquiries((current) => current.filter((inquiry) => inquiry.id !== deleteTarget.id));
      setSuccess('문의를 삭제했습니다.');
      setDeleteTarget(null);
    } catch (deleteError: unknown) {
      setError(deleteError instanceof Error ? deleteError.message : '문의를 삭제하지 못했습니다.');
    } finally {
      setDeleting(false);
    }
  }

  if (!authInitialized) {
    return (
      <main className="main-content inquiry-page">
        <div className="content-loading"><RefreshCw className="spin" size={20} /> 계정을 확인하고 있습니다.</div>
      </main>
    );
  }

  return (
    <main className="main-content inquiry-page">
      <header className="content-hero inquiry-hero">
        <div>
          <p className="content-eyebrow">고객 지원</p>
          <h1>문의하기</h1>
          <p className="content-summary">
            서비스 이용 중 궁금한 점이나 데이터 오류를 알려주시면 확인하겠습니다.
          </p>
        </div>
        <div className="content-hero-symbol inquiry" aria-hidden="true">
          <MessageCircleMore size={40} strokeWidth={1.8} />
        </div>
      </header>

      {!user ? (
        <section className="inquiry-login-card">
          <span><LogIn size={26} /></span>
          <div>
            <h2>로그인 후 문의할 수 있습니다.</h2>
            <p>작성자 확인과 문의 내역 제공을 위해 로그인이 필요합니다.</p>
          </div>
          <Link to="/login">로그인하기</Link>
        </section>
      ) : (
        <div className="inquiry-layout">
          <section className="inquiry-form-card">
            <div className="inquiry-section-heading">
              <span><CircleHelp size={18} /></span>
              <div><h2>새 문의</h2><p>{user.name}님의 문의로 접수됩니다.</p></div>
            </div>

            {error && <div className="inquiry-message is-error"><TriangleAlert size={17} />{error}</div>}
            {success && <div className="inquiry-message is-success"><CheckCircle2 size={17} />{success}</div>}

            <form onSubmit={submitInquiry}>
              <label>
                <span>문의 유형</span>
                <div className="inquiry-select">
                  <select
                    value={category}
                    onChange={(event) => setCategory(event.target.value as InquiryCategory)}
                  >
                    {Object.entries(CATEGORY_LABELS).map(([value, label]) => (
                      <option key={value} value={value}>{label}</option>
                    ))}
                  </select>
                  <ChevronDown size={17} />
                </div>
              </label>
              <label>
                <span>제목</span>
                <input
                  value={title}
                  onChange={(event) => setTitle(event.target.value)}
                  maxLength={150}
                  required
                  placeholder="문의 제목을 입력해 주세요."
                />
              </label>
              <label>
                <span>문의 내용</span>
                <textarea
                  value={content}
                  onChange={(event) => setContent(event.target.value)}
                  maxLength={10000}
                  required
                  rows={9}
                  placeholder="확인이 필요한 내용을 자세히 적어 주세요."
                />
                <small>{content.length.toLocaleString('ko-KR')} / 10,000자</small>
              </label>
              <button type="submit" disabled={submitting}>
                {submitting ? <RefreshCw className="spin" size={17} /> : <Send size={17} />}
                {submitting ? '접수 중' : '문의 접수'}
              </button>
            </form>
          </section>

          <section className="inquiry-history-card">
            <div className="inquiry-section-heading">
              <span><Clock3 size={18} /></span>
              <div><h2>내 문의 내역</h2><p>최근 문의부터 표시됩니다.</p></div>
            </div>
            {loading ? (
              <div className="content-loading"><RefreshCw className="spin" size={19} /> 문의를 불러오고 있습니다.</div>
            ) : inquiries.length === 0 ? (
              <div className="content-empty">아직 등록한 문의가 없습니다.</div>
            ) : (
              <div className="inquiry-history-list">
                {inquiries.map((inquiry) => (
                  <details key={inquiry.id}>
                    <summary>
                      <div>
                        <span>{CATEGORY_LABELS[inquiry.category]}</span>
                        <strong>{inquiry.title}</strong>
                        <small>{formatDateTime(inquiry.createdAt)}</small>
                      </div>
                      <em className={`is-${inquiry.status.toLowerCase()}`}>
                        {STATUS_LABELS[inquiry.status]}
                      </em>
                      <ChevronDown size={18} />
                    </summary>
                    <div className="inquiry-history-detail">
                      <p>{inquiry.content}</p>
                      <button type="button" onClick={() => setDeleteTarget(inquiry)}>
                        <Trash2 size={15} /> 문의 삭제
                      </button>
                    </div>
                  </details>
                ))}
              </div>
            )}
          </section>
        </div>
      )}
      <ConfirmationModal
        open={deleteTarget !== null}
        title="문의 삭제"
        message={`‘${deleteTarget?.title ?? ''}’ 문의를 삭제하시겠습니까? 삭제한 문의는 복구할 수 없습니다.`}
        confirmLabel="삭제"
        loading={deleting}
        onCancel={() => setDeleteTarget(null)}
        onConfirm={() => void confirmDelete()}
      />
    </main>
  );
}

export default InquiryPage;
