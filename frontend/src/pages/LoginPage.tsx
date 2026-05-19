import { zodResolver } from '@hookform/resolvers/zod';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Link as MuiLink,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { useMutation } from '@tanstack/react-query';
import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { Link as RouterLink, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { z } from 'zod';
import { authApi } from '../api/authApi';
import { useAuth } from '../hooks/useAuth';

const schema = z.object({
  email: z.string().min(1, '請輸入 Email').email('Email 格式錯誤'),
  password: z.string().min(1, '請輸入密碼'),
});

type FormValues = z.infer<typeof schema>;

export function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { isAuthenticated, acceptAuthResponse } = useAuth();
  const from = (location.state as { from?: string } | null)?.from ?? '/';

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { email: '', password: '' },
  });

  const mutation = useMutation({
    mutationFn: authApi.login,
    onSuccess: (auth) => {
      acceptAuthResponse(auth);
      navigate(from, { replace: true });
    },
  });

  useEffect(() => {
    // no-op; rendering Navigate below handles redirect when already logged in
  }, []);

  if (isAuthenticated) {
    return <Navigate to={from} replace />;
  }

  const onSubmit = (values: FormValues) => mutation.mutate(values);

  const apiError = mutation.error as { response?: { data?: { message?: string } } } | undefined;
  const errorMessage = apiError?.response?.data?.message ?? (mutation.isError ? '登入失敗' : null);

  return (
    <Box
      sx={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        p: 2,
      }}
    >
      <Card sx={{ width: '100%', maxWidth: 400 }}>
        <CardContent>
          <Typography variant="h5" gutterBottom>
            登入
          </Typography>
          <Box component="form" onSubmit={handleSubmit(onSubmit)} noValidate>
            <Stack spacing={2}>
              {errorMessage && <Alert severity="error">{errorMessage}</Alert>}
              <TextField
                label="Email"
                type="email"
                autoComplete="email"
                fullWidth
                {...register('email')}
                error={!!errors.email}
                helperText={errors.email?.message}
              />
              <TextField
                label="密碼"
                type="password"
                autoComplete="current-password"
                fullWidth
                {...register('password')}
                error={!!errors.password}
                helperText={errors.password?.message}
              />
              <Button type="submit" variant="contained" disabled={mutation.isPending}>
                {mutation.isPending ? '登入中…' : '登入'}
              </Button>
              <Typography variant="body2" align="center">
                還沒有帳號？
                <MuiLink component={RouterLink} to="/register" sx={{ ml: 0.5 }}>
                  立即註冊
                </MuiLink>
              </Typography>
            </Stack>
          </Box>
        </CardContent>
      </Card>
    </Box>
  );
}
