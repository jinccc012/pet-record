import { useCallback, useEffect, useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { authApi } from '../api/authApi';
import { ACCESS_TOKEN_KEY } from '../api/axiosInstance';
import type { AuthResponse, UserSummary } from '../types/auth';

function readToken(): string | null {
  return localStorage.getItem(ACCESS_TOKEN_KEY);
}

export function useAuth() {
  const queryClient = useQueryClient();
  const [token, setToken] = useState<string | null>(readToken);

  useEffect(() => {
    const onStorage = (e: StorageEvent) => {
      if (e.key === ACCESS_TOKEN_KEY) {
        setToken(e.newValue);
      }
    };
    window.addEventListener('storage', onStorage);
    return () => window.removeEventListener('storage', onStorage);
  }, []);

  const meQuery = useQuery<UserSummary>({
    queryKey: ['me'],
    queryFn: authApi.getMe,
    enabled: !!token,
    staleTime: 5 * 60 * 1000,
  });

  const acceptAuthResponse = useCallback(
    (auth: AuthResponse) => {
      localStorage.setItem(ACCESS_TOKEN_KEY, auth.accessToken);
      setToken(auth.accessToken);
      queryClient.setQueryData(['me'], auth.user);
    },
    [queryClient],
  );

  const logout = useCallback(() => {
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    setToken(null);
    queryClient.clear();
  }, [queryClient]);

  return {
    token,
    isAuthenticated: !!token,
    user: meQuery.data,
    isLoadingUser: meQuery.isLoading,
    acceptAuthResponse,
    logout,
  };
}
