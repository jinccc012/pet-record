import { Alert, Box, Button, Stack } from '@mui/material';

interface ErrorStateProps {
  message?: string;
  onRetry?: () => void;
}

export function ErrorState({ message = '發生錯誤，請稍後再試', onRetry }: ErrorStateProps) {
  return (
    <Box sx={{ py: 4 }}>
      <Stack spacing={2} sx={{ alignItems: 'flex-start' }}>
        <Alert severity="error" sx={{ width: '100%' }}>
          {message}
        </Alert>
        {onRetry && (
          <Button variant="outlined" onClick={onRetry}>
            重試
          </Button>
        )}
      </Stack>
    </Box>
  );
}
