export type UserRole = 'USER' | 'ADMIN';

export interface UserSummary {
  id: number;
  username: string;
  email: string;
  role: UserRole;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: 'Bearer';
  expiresInSeconds: number;
  user: UserSummary;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
}
