import { useEffect, useState } from 'react';
import {
  ChevronDown,
  ChevronLeft,
  ChevronRight,
  Mail,
  MessageCircleMore,
  Phone,
  RefreshCw,
  Trash2,
  TriangleAlert,
  UserRound
} from 'lucide-react';
import { deleteDeveloperInquiry, fetchDeveloperInquiries } from '../api/developer';
import ConfirmationModal from './ConfirmationModal';
import type {
  DeveloperInquiry,
  DeveloperInquiryPage,
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

function DeveloperInquiriesPanel() {
  const [inquiries, setInquiries] = useState<DeveloperInquiryPage | null>(null);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [deleteTarget, setDeleteTarget] = useState<DeveloperInquiry | null>(null);
  const [deleting, setDeleting] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  useEffect(() => {
    void loadInquiries(page);
  }, [page]);

  async function loadInquiries(requestedPage: number) {
    setLoading(true);
    setError('');
    try {
      setInquiries(await fetchDeveloperInquiries(requestedPage));
    } catch (loadError: unknown) {
      setError(loadError instanceof Error ? loadError.message : '문의 목록을 불러오지 못했습니다.');
    } finally {
      setLoading(false);
    }
  }

  async function confirmDelete() {
    if (!deleteTarget) return;
    setDeleting(true);
    setError('');
    setSuccess('');
    try {
      await deleteDeveloperInquiry(deleteTarget.id);
      setDeleteTarget(null);
      setSuccess('문의를 삭제했습니다.');
      if ((inquiries?.items.length ?? 0) === 1 && page > 0) {
        setPage((current) => current - 1);
      } else {
        await loadInquiries(page);
      }
    } catch (deleteError: unknown) {
      setError(deleteError instanceof Error ? deleteError.message : '문의를 삭제하지 못했습니다.');
    } finally {
      setDeleting(false);
    }
  }

  const currentPage = inquiries?.page.number ?? 0;
  const totalPages = inquiries?.page.totalPages ?? 0;

  return (
    <section className="developer-panel developer-content-panel">
      <div className="developer-management-heading">
        <div>
          <span>Inquiry management</span>
          <h2>문의 관리</h2>
          <p>사용자가 웹과 앱에서 등록한 문의를 확인합니다.</p>
        </div>
        <div className="developer-total-badge">
          <MessageCircleMore size={16} /> 총 {(inquiries?.page.totalElements ?? 0).toLocaleString('ko-KR')}건
        </div>
      </div>

      {error && <div className="developer-alert is-error"><TriangleAlert size={17} />{error}</div>}
      {success && <div className="developer-alert is-success">{success}</div>}

      {loading ? (
        <div className="developer-empty"><RefreshCw className="spin" size={19} /> 문의를 불러오고 있습니다.</div>
      ) : !inquiries?.items.length ? (
        <div className="developer-empty">접수된 문의가 없습니다.</div>
      ) : (
        <div className="developer-inquiry-list">
          {inquiries.items.map((inquiry) => (
            <details key={inquiry.id}>
              <summary>
                <div className="developer-inquiry-user">
                  <span><UserRound size={17} /></span>
                  <p><strong>{inquiry.userName}</strong><small>@{inquiry.username} · #{inquiry.userId}</small></p>
                </div>
                <div className="developer-inquiry-title">
                  <span>{CATEGORY_LABELS[inquiry.category]}</span>
                  <strong>{inquiry.title}</strong>
                  <small>{formatDateTime(inquiry.createdAt)}</small>
                </div>
                <em className={`is-${inquiry.status.toLowerCase()}`}>
                  {STATUS_LABELS[inquiry.status]}
                </em>
                <ChevronDown size={18} />
              </summary>
              <div className="developer-inquiry-detail">
                <p>{inquiry.content}</p>
                <div>
                  <span><Mail size={14} />{inquiry.email}</span>
                  <span><Phone size={14} />{inquiry.phoneNumber}</span>
                  <button type="button" onClick={() => setDeleteTarget(inquiry)}>
                    <Trash2 size={14} /> 문의 삭제
                  </button>
                </div>
              </div>
            </details>
          ))}
        </div>
      )}

      <div className="developer-pagination">
        <span>{totalPages === 0 ? 0 : currentPage + 1}/{totalPages}페이지</span>
        <div>
          <button
            type="button"
            aria-label="이전 페이지"
            disabled={loading || currentPage <= 0}
            onClick={() => setPage((current) => Math.max(0, current - 1))}
          ><ChevronLeft size={18} /></button>
          <button
            type="button"
            aria-label="다음 페이지"
            disabled={loading || currentPage + 1 >= totalPages}
            onClick={() => setPage((current) => current + 1)}
          ><ChevronRight size={18} /></button>
        </div>
      </div>
      <ConfirmationModal
        open={deleteTarget !== null}
        title="문의 삭제"
        message={`‘${deleteTarget?.title ?? ''}’ 문의를 삭제하시겠습니까? 삭제한 문의는 복구할 수 없습니다.`}
        confirmLabel="삭제"
        loading={deleting}
        onCancel={() => setDeleteTarget(null)}
        onConfirm={() => void confirmDelete()}
      />
    </section>
  );
}

export default DeveloperInquiriesPanel;
