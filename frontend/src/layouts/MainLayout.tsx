import { AppBar, Box, Button, Container, Stack, Toolbar, Typography } from '@mui/material';
import { Link as RouterLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import dogLogo from '../assets/dog-logo.svg';

export function MainLayout() {
  const navigate = useNavigate();
  const { user, logout } = useAuth();

  const handleLogout = () => {
    logout();
    navigate('/login', { replace: true });
  };

  return (
    <Box sx={{ minHeight: '100vh', bgcolor: 'background.default' }}>
      <AppBar position="static" color="primary">
        <Toolbar>
          <Stack
            direction="row"
            spacing={1}
            component={RouterLink}
            to="/"
            sx={{ flexGrow: 1, alignItems: 'center', color: 'inherit', textDecoration: 'none' }}
          >
            <Box
              component="img"
              src={dogLogo}
              alt="Pet Record"
              sx={{
                width: 32,
                height: 32,
                imageRendering: 'pixelated',
                display: 'block',
              }}
            />
            <Typography variant="h6" component="span">
              Pet Record
            </Typography>
          </Stack>
          <Stack direction="row" spacing={2} sx={{ alignItems: 'center' }}>
            {user && (
              <Typography variant="body2" sx={{ display: { xs: 'none', sm: 'inline' } }}>
                {user.username}
              </Typography>
            )}
            <Button color="inherit" onClick={handleLogout}>
              登出
            </Button>
          </Stack>
        </Toolbar>
      </AppBar>
      <Container maxWidth="md" sx={{ py: 4 }}>
        <Outlet />
      </Container>
    </Box>
  );
}
