import type { Inquiry, InquiryInput, Notice } from '../types/content';

interface ApiErrorResponse {
  message?: string;
  fieldErrors?: Array<{ message: string }>;
}

export async function fetchNotices(): Promise<Notice[]> {
  const response = await fetch('/api/v1/notices', { credentials: 'include' });
  return parseResponse<Notice[]>(response);
}

export async function fetchMyInquiries(): Promise<Inquiry[]> {
  const response = await fetch('/api/v1/inquiries/me', { credentials: 'include' });
  return parseResponse<Inquiry[]>(response);
}

export async function createInquiry(input: InquiryInput): Promise<Inquiry> {
  const response = await fetch('/api/v1/inquiries', {
    method: 'POST',
    credentials: 'include',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input)
  });
  return parseResponse<Inquiry>(response);
}

export async function deleteMyInquiry(inquiryId: number): Promise<void> {
  const response = await fetch(`/api/v1/inquiries/${inquiryId}`, {
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
    throw new Error(
      body?.fieldErrors?.[0]?.message
      ?? body?.message
      ?? '요청을 처리하지 못했습니다.'
    );
  }
  if (body === null) {
    throw new Error('서버에서 비어 있는 응답을 받았습니다.');
  }
  return body;
}
