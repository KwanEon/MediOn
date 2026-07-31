export interface AuthUser {
  id: number;
  username: string;
  name: string;
  email: string;
  phoneNumber: string;
  address: string;
  latitude: number;
  longitude: number;
  role: 'USER' | 'DEVELOPER';
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterRequest extends LoginRequest {
  name: string;
  email: string;
  phoneNumber: string;
  address: string;
}

export interface UpdateProfileRequest {
  name: string;
  email: string;
  address: string;
}

export interface AddressSearchResult {
  address: string;
  roadAddress: string | null;
  jibunAddress: string | null;
  latitude: number;
  longitude: number;
}
