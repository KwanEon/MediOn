export type NoticeCategory = 'IMPORTANT' | 'UPDATE' | 'DATA' | 'GUIDE';

export interface Notice {
  id: number;
  category: NoticeCategory;
  title: string;
  content: string;
  pinned: boolean;
  publishedAt: string;
  updatedAt: string;
}

export interface NoticeInput {
  category: NoticeCategory;
  title: string;
  content: string;
  pinned: boolean;
}

export type InquiryCategory = 'GENERAL' | 'ACCOUNT' | 'DATA' | 'ERROR' | 'OTHER';
export type InquiryStatus = 'RECEIVED' | 'REVIEWING' | 'ANSWERED' | 'CLOSED';

export interface Inquiry {
  id: number;
  category: InquiryCategory;
  title: string;
  content: string;
  status: InquiryStatus;
  createdAt: string;
  updatedAt: string;
}

export interface InquiryInput {
  category: InquiryCategory;
  title: string;
  content: string;
}

export interface DeveloperInquiry extends Inquiry {
  userId: number;
  username: string;
  userName: string;
  email: string;
  phoneNumber: string;
}

export interface DeveloperInquiryPage {
  items: DeveloperInquiry[];
  page: {
    number: number;
    size: number;
    totalElements: number;
    totalPages: number;
  };
}
