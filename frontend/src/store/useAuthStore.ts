import { create } from 'zustand';
import {
  fetchCurrentUser,
  login as loginRequest,
  logout as logoutRequest,
  register as registerRequest,
  updateProfile as updateProfileRequest
} from '../api/auth';
import type {
  AuthUser,
  LoginRequest,
  RegisterRequest,
  UpdateProfileRequest
} from '../types/auth';

interface AuthState {
  user: AuthUser | null;
  initialized: boolean;
  loading: boolean;
  error: string;
}

interface AuthActions {
  loadCurrentUser: () => Promise<AuthUser | null>;
  login: (request: LoginRequest) => Promise<AuthUser>;
  register: (request: RegisterRequest) => Promise<AuthUser>;
  updateProfile: (request: UpdateProfileRequest) => Promise<AuthUser>;
  logout: () => Promise<void>;
  clearError: () => void;
}

type AuthStore = AuthState & AuthActions;

let currentUserRequest: Promise<AuthUser | null> | null = null;

export const useAuthStore = create<AuthStore>((set, get) => ({
  user: null,
  initialized: false,
  loading: false,
  error: '',
  loadCurrentUser: async () => {
    if (get().initialized) {
      return get().user;
    }
    if (currentUserRequest) {
      return currentUserRequest;
    }

    set({ loading: true, error: '' });
    const request = fetchCurrentUser()
      .then((user) => {
        set({ user, initialized: true });
        return user;
      })
      .catch((error: unknown) => {
        const message = error instanceof Error ? error.message : '로그인 상태를 확인하지 못했습니다.';
        set({ user: null, initialized: true, error: message });
        return null;
      })
      .finally(() => {
        currentUserRequest = null;
        set({ loading: false });
      });
    currentUserRequest = request;
    return request;
  },
  login: async (request) => {
    set({ loading: true, error: '' });
    try {
      const user = await loginRequest(request);
      set({ user, initialized: true });
      return user;
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : '로그인하지 못했습니다.';
      set({ error: message });
      throw error;
    } finally {
      set({ loading: false });
    }
  },
  register: async (request) => {
    set({ loading: true, error: '' });
    try {
      return await registerRequest(request);
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : '회원가입을 완료하지 못했습니다.';
      set({ error: message });
      throw error;
    } finally {
      set({ loading: false });
    }
  },
  updateProfile: async (request) => {
    set({ loading: true, error: '' });
    try {
      const user = await updateProfileRequest(request);
      set({ user });
      return user;
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : '회원정보를 변경하지 못했습니다.';
      set({ error: message });
      throw error;
    } finally {
      set({ loading: false });
    }
  },
  logout: async () => {
    set({ loading: true, error: '' });
    try {
      await logoutRequest();
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : '로그아웃 요청을 완료하지 못했습니다.';
      set({ error: message });
    } finally {
      set({ user: null, initialized: true, loading: false });
    }
  },
  clearError: () => set({ error: '' })
}));
