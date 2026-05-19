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
import { useForm } from 'react-hook-form';
import { Link as RouterLink, Navigate, useNavigate } from 'react-router-dom';
import { z } from 'zod';
import { authApi } from '../api/authApi';
import { useAuth } from '../hooks/useAuth';

const schema = z.object({
  username: z.string().min(1, '請輸入帳號').max(100, '帳號最多 100 字'),
  email: z.string().min(1, '請輸入 Email').email('Email 格式錯誤').max(255),
  password: z.string().min(8, '密碼至少 8 個字').max(255, '密碼最多 255 字'),
});

type FormValues = z.infer<typeof schema>;

export function RegisterPage() {
  const navigate = useNavigate();
  const { isAuthenticated, acceptAuthResponse } = useAuth();

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { username: '', email: '', password: '' },
  });

  const mutation = useMutation({
    mutationFn: authApi.register,
    onSuccess: (auth) => {
      acceptAuthResponse(auth);
      navigate('/', { replace: true });
    },
  });

  if (isAuthenticated) {
    return <Navigate to="/" replace />;
  }

  const onSubmit = (values: FormValues) => mutation.mutate(values);

  const apiError = mutation.error as { response?: { data?: { message?: string } } } | undefined;
  const errorMessage = apiError?.response?.data?.message ?? (mutation.isError ? '註冊失敗' : null);

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
      <Card sx={{ width: '100%', maxWidth: 420 }}>
        <CardContent>
          <Typography variant="h5" gutterBottom>
            註冊
          </Typography>
          <Box component="form" onSubmit={handleSubmit(onSubmit)} noValidate>
            <Stack spacing={2}>
              {errorMessage && <Alert severity="error">{errorMessage}</Alert>}
              <TextField
                label="帳號"
                autoComplete="username"
                fullWidth
                {...register('username')}
                error={!!errors.username}
                helperText={errors.username?.message}
              />
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
                autoComplete="new-password"
                fullWidth
                {...register('password')}
                error={!!errors.password}
                helperText={errors.password?.message ?? '至少 8 個字'}
              />
              <Button type="submit" variant="contained" disabled={mutation.isPending}>
                {mutation.isPending ? '註冊中…' : '註冊'}
              </Button>
              <Typography variant="body2" align="center">
                已有帳號？
                <MuiLink component={RouterLink} to="/login" sx={{ ml: 0.5 }}>
                  返回登入
                </MuiLink>
              </Typography>
            </Stack>
          </Box>
        </CardContent>
      </Card>
    </Box>
  );
}
