import { Box, Typography } from '@mui/material';

interface EmptyStateProps {
  title?: string;
  description?: string;
}

export function EmptyState({ title = '尚無資料', description }: EmptyStateProps) {
  return (
    <Box
      sx={{
        py: 6,
        textAlign: 'center',
        color: 'text.secondary',
      }}
    >
      <Typography variant="h6" gutterBottom>
        {title}
      </Typography>
      {description && <Typography variant="body2">{description}</Typography>}
    </Box>
  );
}
