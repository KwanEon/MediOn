import type {
  DeveloperDashboard,
  DeveloperUserPage,
  SyncTriggerResult
} from '../types/developer';
import type {
  DeveloperInquiryPage,
  Notice,
  NoticeInput
} from '../types/content';

interface ApiErrorResponse {
  message?: string;
}

export async function fetchDeveloperDashboard(): Promise<DeveloperDashboard> {
  const response = await fetch('/api/v1/developer/dashboard', {
    credentials: 'include'
  });
  return parseResponse<DeveloperDashboard>(response);
}

export async function fetchDeveloperUsers(
  query: string,
  page: number,
  size = 20
): Promise<DeveloperUserPage> {
  const params = new URLSearchParams({
    query,
    page: String(page),
    size: String(size)
  });
  const response = await fetch(`/api/v1/developer/users?${params.toString()}`, {
    credentials: 'include'
  });
  return parseResponse<DeveloperUserPage>(response);
}

export async function triggerDeveloperSync(
  target: 'hospitals' | 'pharmacies'
): Promise<SyncTriggerResult> {
  const response = await fetch(`/api/v1/developer/sync/${target}`, {
    method: 'POST',
    credentials: 'include'
  });
  return parseResponse<SyncTriggerResult>(response);
}

export async function fetchDeveloperNotices(): Promise<Notice[]> {
  const response = await fetch('/api/v1/developer/notices', {
    credentials: 'include'
  });
  return parseResponse<Notice[]>(response);
}

export async function createDeveloperNotice(input: NoticeInput): Promise<Notice> {
  const response = await fetch('/api/v1/developer/notices', {
    method: 'POST',
    credentials: 'include',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input)
  });
  return parseResponse<Notice>(response);
}

export async function updateDeveloperNotice(
  noticeId: number,
  input: NoticeInput
): Promise<Notice> {
  const response = await fetch(`/api/v1/developer/notices/${noticeId}`, {
    method: 'PUT',
    credentials: 'include',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input)
  });
  return parseResponse<Notice>(response);
}

export async function deleteDeveloperNotice(noticeId: number): Promise<void> {
  const response = await fetch(`/api/v1/developer/notices/${noticeId}`, {
    method: 'DELETE',
    credentials: 'include'
  });
  if (!response.ok) {
    await parseResponse(response);
  }
}

export async function fetchDeveloperInquiries(
  page: number,
  size = 20
): Promise<DeveloperInquiryPage> {
  const params = new URLSearchParams({ page: String(page), size: String(size) });
  const response = await fetch(`/api/v1/developer/inquiries?${params.toString()}`, {
    credentials: 'include'
  });
  return parseResponse<DeveloperInquiryPage>(response);
}

export async function deleteDeveloperInquiry(inquiryId: number): Promise<void> {
  const response = await fetch(`/api/v1/developer/inquiries/${inquiryId}`, {
    method: 'DELETE',
    credentials: 'include'
  });
  if (!response.ok) {
    await parseResponse(response);
  }
}

async function parseResponse<T>(response: Response): Promise<T> {
  const body = await response.json().catch(() => null) as (T & ApiErrorResponse) | null;
  if (!response.ok) {
    throw new Error(body?.message ?? '개발자 기능 요청을 처리하지 못했습니다.');
  }
  if (body === null) {
    throw new Error('서버에서 비어 있는 응답을 받았습니다.');
  }
  return body;
}
