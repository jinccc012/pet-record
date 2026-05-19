import { axiosInstance } from './axiosInstance';
import type { AuthResponse, LoginRequest, RegisterRequest, UserSummary } from '../types/auth';

export const authApi = {
  async login(payload: LoginRequest): Promise<AuthResponse> {
    const { data } = await axiosInstance.post<AuthResponse>('/api/auth/login', payload);
    return data;
  },

  async register(payload: RegisterRequest): Promise<AuthResponse> {
    const { data } = await axiosInstance.post<AuthResponse>('/api/auth/register', payload);
    return data;
  },

  async getMe(): Promise<UserSummary> {
    const { data } = await axiosInstance.get<UserSummary>('/api/users/me');
    return data;
  },
};
