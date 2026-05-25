import DeleteIcon from '@mui/icons-material/Delete';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Snackbar,
  Stack,
  Tab,
  Tabs,
  Typography,
} from '@mui/material';
import { useEffect, useState } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import { ConfirmDialog } from '../components/common/ConfirmDialog';
import { DailyRecordsTab } from '../components/daily/DailyRecordsTab';
import { ErrorState } from '../components/common/ErrorState';
import { Loading } from '../components/common/Loading';
import { PetBasicInfoForm } from '../components/pet/PetBasicInfoForm';
import { useDeletePet, usePet } from '../hooks/usePets';

export function PetDetailPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { petId } = useParams<{ petId: string }>();
  const id = petId ? Number(petId) : NaN;
  const validId = Number.isFinite(id);

  const petQuery = usePet(validId ? id : undefined);
  const deleteMutation = useDeletePet();
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [tab, setTab] = useState(0);
  const justCreated = (location.state as { justCreated?: boolean } | null)?.justCreated ?? false;
  const [createdSnackOpen, setCreatedSnackOpen] = useState(justCreated);

  useEffect(() => {
    if (justCreated) {
      navigate(location.pathname, { replace: true, state: null });
    }
  }, [justCreated, navigate, location.pathname]);

  if (!validId) {
    return <ErrorState message="無效的寵物 ID" />;
  }
  if (petQuery.isLoading) {
    return <Loading fullHeight />;
  }
  if (petQuery.isError || !petQuery.data) {
    const status = (petQuery.error as { response?: { status?: number } } | null)?.response?.status;
    const message = status === 404 ? '找不到這隻寵物' : '載入失敗';
    return <ErrorState message={message} onRetry={() => petQuery.refetch()} />;
  }

  const pet = petQuery.data;

  const onConfirmDelete = () => {
    deleteMutation.mutate(id, {
      onSuccess: () => {
        setConfirmOpen(false);
        navigate('/', { replace: true });
      },
    });
  };

  const deleteApiError = deleteMutation.error as
    | { response?: { data?: { message?: string } } }
    | undefined;
  const deleteErrorMessage = deleteApiError?.response?.data?.message;

  return (
    <Stack spacing={2}>
      <Card>
        <CardContent>
          <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'center' }}>
            <Typography variant="h5">{pet.name}</Typography>
            <Button
              color="error"
              variant="outlined"
              startIcon={<DeleteIcon />}
              onClick={() => setConfirmOpen(true)}
              disabled={deleteMutation.isPending}
            >
              刪除
            </Button>
          </Stack>

          {deleteErrorMessage && (
            <Alert severity="error" sx={{ mt: 2 }}>
              {deleteErrorMessage}
            </Alert>
          )}

          <Tabs value={tab} onChange={(_e, v) => setTab(v)} sx={{ mt: 1, mb: 2 }}>
            <Tab label="基本資料" />
            <Tab label="日常紀錄" />
          </Tabs>

          <Box hidden={tab !== 0}>{tab === 0 && <PetBasicInfoForm pet={pet} />}</Box>
          <Box hidden={tab !== 1}>{tab === 1 && <DailyRecordsTab petId={id} />}</Box>
        </CardContent>
      </Card>

      <ConfirmDialog
        open={confirmOpen}
        title="刪除寵物"
        message={`確定要刪除「${pet.name}」嗎？此動作不可復原。`}
        loading={deleteMutation.isPending}
        onCancel={() => setConfirmOpen(false)}
        onConfirm={onConfirmDelete}
      />

      <Snackbar
        open={createdSnackOpen}
        autoHideDuration={3000}
        onClose={() => setCreatedSnackOpen(false)}
        anchorOrigin={{ vertical: 'top', horizontal: 'center' }}
      >
        <Alert
          severity="success"
          variant="filled"
          onClose={() => setCreatedSnackOpen(false)}
          sx={{ width: '100%' }}
        >
          已成功建立
        </Alert>
      </Snackbar>
    </Stack>
  );
}
