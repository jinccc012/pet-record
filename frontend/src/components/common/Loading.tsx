import { Box, CircularProgress } from '@mui/material';

interface LoadingProps {
  fullHeight?: boolean;
}

export function Loading({ fullHeight = false }: LoadingProps) {
  return (
    <Box
      sx={{
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        py: 6,
        minHeight: fullHeight ? '40vh' : undefined,
      }}
    >
      <CircularProgress />
    </Box>
  );
}
