import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import {
  CheckCircle2,
  ChevronDown,
  Megaphone,
  Pencil,
  Plus,
  RefreshCw,
  Save,
  Trash2,
  TriangleAlert,
  X
} from 'lucide-react';
import {
  createDeveloperNotice,
  deleteDeveloperNotice,
  fetchDeveloperNotices,
  updateDeveloperNotice
} from '../api/developer';
import type {
  Notice,
  NoticeCategory,
  NoticeInput
} from '../types/content';
import ConfirmationModal from './ConfirmationModal';

const CATEGORY_LABELS: Record<NoticeCategory, string> = {
  IMPORTANT: '중요',
  UPDATE: '업데이트',
  DATA: '데이터',
  GUIDE: '안내'
};

const EMPTY_FORM: NoticeInput = {
  category: 'GUIDE',
  title: '',
  content: '',
  pinned: false
};

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat('ko-KR', {
    dateStyle: 'medium',
    timeStyle: 'short'
  }).format(new Date(value));
}

function DeveloperNoticesPanel() {
  const [notices, setNotices] = useState<Notice[]>([]);
  const [form, setForm] = useState<NoticeInput>(EMPTY_FORM);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [formOpen, setFormOpen] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<Notice | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  useEffect(() => {
    void loadNotices();
  }, []);

  async function loadNotices() {
    setLoading(true);
    setError('');
    try {
      setNotices(await fetchDeveloperNotices());
    } catch (loadError: unknown) {
      setError(loadError instanceof Error ? loadError.message : '공지사항을 불러오지 못했습니다.');
    } finally {
      setLoading(false);
    }
  }

  function startCreate() {
    setEditingId(null);
    setForm(EMPTY_FORM);
    setFormOpen(true);
    setError('');
    setSuccess('');
  }

  function startEdit(notice: Notice) {
    setEditingId(notice.id);
    setForm({
      category: notice.category,
      title: notice.title,
      content: notice.content,
      pinned: notice.pinned
    });
    setFormOpen(true);
    setError('');
    setSuccess('');
  }

  function closeForm() {
    setFormOpen(false);
    setEditingId(null);
    setForm(EMPTY_FORM);
  }

  async function submitNotice(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaving(true);
    setError('');
    setSuccess('');
    try {
      if (editingId === null) {
        await createDeveloperNotice(form);
        setSuccess('공지사항을 등록했습니다.');
      } else {
        await updateDeveloperNotice(editingId, form);
        setSuccess('공지사항을 수정했습니다.');
      }
      closeForm();
      await loadNotices();
    } catch (saveError: unknown) {
      setError(saveError instanceof Error ? saveError.message : '공지사항을 저장하지 못했습니다.');
    } finally {
      setSaving(false);
    }
  }

  async function confirmDelete() {
    if (!deleteTarget) return;
    setSaving(true);
    setError('');
    try {
      await deleteDeveloperNotice(deleteTarget.id);
      setDeleteTarget(null);
      setSuccess('공지사항을 삭제했습니다.');
      await loadNotices();
    } catch (deleteError: unknown) {
      setError(deleteError instanceof Error ? deleteError.message : '공지사항을 삭제하지 못했습니다.');
    } finally {
      setSaving(false);
    }
  }

  return (
    <section className="developer-panel developer-content-panel">
      <div className="developer-management-heading">
        <div>
          <span>Notice management</span>
          <h2>공지사항</h2>
          <p>웹과 앱에 함께 노출되는 공지를 관리합니다.</p>
        </div>
        <button type="button" className="is-primary" onClick={startCreate}>
          <Plus size={17} /> 공지 등록
        </button>
      </div>

      {error && <div className="developer-alert is-error"><TriangleAlert size={17} />{error}</div>}
      {success && <div className="developer-alert is-success"><CheckCircle2 size={17} />{success}</div>}

      {formOpen && (
        <form className="developer-notice-form" onSubmit={submitNotice}>
          <div className="developer-form-heading">
            <div>
              <Megaphone size={19} />
              <strong>{editingId === null ? '새 공지 등록' : '공지 수정'}</strong>
            </div>
            <button type="button" onClick={closeForm} aria-label="입력 폼 닫기"><X size={18} /></button>
          </div>
          <div className="developer-form-grid">
            <label>
              <span>공지 유형</span>
              <div className="developer-select">
                <select
                  value={form.category}
                  onChange={(event) => setForm((current) => ({
                    ...current,
                    category: event.target.value as NoticeCategory
                  }))}
                >
                  {Object.entries(CATEGORY_LABELS).map(([value, label]) => (
                    <option key={value} value={value}>{label}</option>
                  ))}
                </select>
                <ChevronDown size={16} />
              </div>
            </label>
            <label className="developer-pin-field">
              <input
                type="checkbox"
                checked={form.pinned}
                onChange={(event) => setForm((current) => ({
                  ...current,
                  pinned: event.target.checked
                }))}
              />
              <span>중요 공지로 상단 고정</span>
            </label>
          </div>
          <label>
            <span>제목</span>
            <input
              value={form.title}
              onChange={(event) => setForm((current) => ({ ...current, title: event.target.value }))}
              maxLength={150}
              required
              placeholder="공지 제목을 입력해 주세요."
            />
          </label>
          <label>
            <span>내용</span>
            <textarea
              value={form.content}
              onChange={(event) => setForm((current) => ({ ...current, content: event.target.value }))}
              maxLength={10000}
              required
              rows={8}
              placeholder="공지 내용을 입력해 주세요. 빈 줄로 문단을 나눌 수 있습니다."
            />
          </label>
          <div className="developer-form-actions">
            <button type="button" onClick={closeForm}>취소</button>
            <button type="submit" className="is-primary" disabled={saving}>
              {saving ? <RefreshCw className="spin" size={16} /> : <Save size={16} />}
              {editingId === null ? '등록' : '저장'}
            </button>
          </div>
        </form>
      )}

      {loading ? (
        <div className="developer-empty"><RefreshCw className="spin" size={19} /> 공지를 불러오고 있습니다.</div>
      ) : notices.length === 0 ? (
        <div className="developer-empty">등록된 공지사항이 없습니다.</div>
      ) : (
        <div className="developer-notice-list">
          {notices.map((notice) => (
            <article key={notice.id}>
              <div className="developer-notice-type">
                <span>{CATEGORY_LABELS[notice.category]}</span>
                {notice.pinned && <em>상단 고정</em>}
              </div>
              <div>
                <strong>{notice.title}</strong>
                <p>{notice.content}</p>
                <small>게시 {formatDateTime(notice.publishedAt)} · 수정 {formatDateTime(notice.updatedAt)}</small>
              </div>
              <div className="developer-row-actions">
                <button type="button" onClick={() => startEdit(notice)}><Pencil size={15} /> 수정</button>
                <button type="button" className="is-danger" onClick={() => setDeleteTarget(notice)}>
                  <Trash2 size={15} /> 삭제
                </button>
              </div>
            </article>
          ))}
        </div>
      )}

      <ConfirmationModal
        open={deleteTarget !== null}
        title="공지사항 삭제"
        message={`‘${deleteTarget?.title ?? ''}’ 공지를 삭제하시겠습니까?`}
        confirmLabel="삭제"
        loading={saving}
        onCancel={() => setDeleteTarget(null)}
        onConfirm={() => void confirmDelete()}
      />
    </section>
  );
}

export default DeveloperNoticesPanel;
