import { zodResolver } from '@hookform/resolvers/zod';
import DeleteIcon from '@mui/icons-material/Delete';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  MenuItem,
  Snackbar,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import dayjs from 'dayjs';
import { useEffect, useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import { z } from 'zod';
import { ConfirmDialog } from '../components/common/ConfirmDialog';
import { ErrorState } from '../components/common/ErrorState';
import { Loading } from '../components/common/Loading';
import { useDeletePet, usePet, useUpdatePet } from '../hooks/usePets';
import {
  PET_GENDER_OPTIONS,
  PET_SPECIES_OPTIONS,
  type PetSpecies,
  type UpdatePetRequest,
} from '../types/pet';

const SPECIES_VALUES = PET_SPECIES_OPTIONS.map((o) => o.value) as [PetSpecies, ...PetSpecies[]];

const schema = z.object({
  name: z.string().trim().min(1, '請輸入名字').max(100, '名字最多 100 字'),
  species: z.enum(SPECIES_VALUES),
  breed: z.string().trim().max(100, '品種最多 100 字').optional(),
  gender: z.enum(['MALE', 'FEMALE', 'UNKNOWN', '']).optional(),
  birthDate: z
    .string()
    .optional()
    .refine((v) => !v || !dayjs(v).isAfter(dayjs(), 'day'), '生日不可為未來日期'),
  color: z.string().trim().max(50, '毛色最多 50 字').optional(),
  notes: z.string().trim().max(2000, '備註最多 2000 字').optional(),
});

type FormValues = z.infer<typeof schema>;

function toPayload(values: FormValues): UpdatePetRequest {
  return {
    name: values.name,
    species: values.species,
    breed: values.breed ? values.breed : null,
    gender: values.gender ? (values.gender as 'MALE' | 'FEMALE' | 'UNKNOWN') : null,
    birthDate: values.birthDate ? values.birthDate : null,
    color: values.color ? values.color : null,
    notes: values.notes ? values.notes : null,
  };
}

export function PetDetailPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { petId } = useParams<{ petId: string }>();
  const id = petId ? Number(petId) : NaN;
  const validId = Number.isFinite(id);

  const petQuery = usePet(validId ? id : undefined);
  const updateMutation = useUpdatePet(id);
  const deleteMutation = useDeletePet();
  const [confirmOpen, setConfirmOpen] = useState(false);
  const justCreated = (location.state as { justCreated?: boolean } | null)?.justCreated ?? false;
  const [createdSnackOpen, setCreatedSnackOpen] = useState(justCreated);

  useEffect(() => {
    if (justCreated) {
      // Clear state so a refresh doesn't re-trigger the snackbar.
      navigate(location.pathname, { replace: true, state: null });
    }
  }, [justCreated, navigate, location.pathname]);

  const {
    control,
    register,
    reset,
    handleSubmit,
    formState: { errors, isDirty },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      name: '',
      species: 'OTHER',
      breed: '',
      gender: '',
      birthDate: '',
      color: '',
      notes: '',
    },
  });

  useEffect(() => {
    if (petQuery.data) {
      reset({
        name: petQuery.data.name,
        species: petQuery.data.species,
        breed: petQuery.data.breed ?? '',
        gender: petQuery.data.gender ?? '',
        birthDate: petQuery.data.birthDate ?? '',
        color: petQuery.data.color ?? '',
        notes: petQuery.data.notes ?? '',
      });
    }
  }, [petQuery.data, reset]);

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

  const onSubmit = (values: FormValues) => {
    updateMutation.mutate(toPayload(values), {
      onSuccess: (pet) => {
        reset({
          name: pet.name,
          species: pet.species,
          breed: pet.breed ?? '',
          gender: pet.gender ?? '',
          birthDate: pet.birthDate ?? '',
          color: pet.color ?? '',
          notes: pet.notes ?? '',
        });
      },
    });
  };

  const onConfirmDelete = () => {
    deleteMutation.mutate(id, {
      onSuccess: () => {
        setConfirmOpen(false);
        navigate('/', { replace: true });
      },
    });
  };

  const updateApiError = updateMutation.error as
    | { response?: { data?: { message?: string } } }
    | undefined;
  const updateErrorMessage = updateApiError?.response?.data?.message;

  const deleteApiError = deleteMutation.error as
    | { response?: { data?: { message?: string } } }
    | undefined;
  const deleteErrorMessage = deleteApiError?.response?.data?.message;

  return (
    <Stack spacing={2}>
      <Card>
        <CardContent>
          <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
            <Typography variant="h5">編輯寵物</Typography>
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

          {updateMutation.isSuccess && !isDirty && <Alert severity="success">已儲存</Alert>}

          <Box component="form" onSubmit={handleSubmit(onSubmit)} noValidate sx={{ mt: 1 }}>
            <Stack spacing={2}>
              {updateErrorMessage && <Alert severity="error">{updateErrorMessage}</Alert>}
              {deleteErrorMessage && <Alert severity="error">{deleteErrorMessage}</Alert>}
              <TextField
                label="名字"
                required
                fullWidth
                {...register('name')}
                error={!!errors.name}
                helperText={errors.name?.message}
              />
              <Controller
                name="species"
                control={control}
                render={({ field }) => (
                  <TextField
                    select
                    label="物種"
                    required
                    fullWidth
                    {...field}
                    error={!!errors.species}
                    helperText={errors.species?.message}
                  >
                    {PET_SPECIES_OPTIONS.map((o) => (
                      <MenuItem key={o.value} value={o.value}>
                        {o.label}
                      </MenuItem>
                    ))}
                  </TextField>
                )}
              />
              <TextField
                label="品種"
                fullWidth
                {...register('breed')}
                error={!!errors.breed}
                helperText={errors.breed?.message}
              />
              <Controller
                name="gender"
                control={control}
                render={({ field }) => (
                  <TextField select label="性別" fullWidth {...field}>
                    <MenuItem value="">未指定</MenuItem>
                    {PET_GENDER_OPTIONS.map((o) => (
                      <MenuItem key={o.value} value={o.value}>
                        {o.label}
                      </MenuItem>
                    ))}
                  </TextField>
                )}
              />
              <TextField
                label="生日"
                type="date"
                fullWidth
                slotProps={{ inputLabel: { shrink: true } }}
                {...register('birthDate')}
                error={!!errors.birthDate}
                helperText={errors.birthDate?.message}
              />
              <TextField
                label="毛色"
                fullWidth
                {...register('color')}
                error={!!errors.color}
                helperText={errors.color?.message}
              />
              <TextField
                label="備註"
                fullWidth
                multiline
                minRows={3}
                {...register('notes')}
                error={!!errors.notes}
                helperText={errors.notes?.message}
              />
              <Stack direction="row" spacing={2} sx={{ justifyContent: 'flex-end' }}>
                <Button onClick={() => navigate('/')} disabled={updateMutation.isPending}>
                  返回
                </Button>
                <Button
                  type="submit"
                  variant="contained"
                  disabled={updateMutation.isPending || !isDirty}
                >
                  {updateMutation.isPending ? '儲存中…' : '儲存'}
                </Button>
              </Stack>
            </Stack>
          </Box>
        </CardContent>
      </Card>

      <ConfirmDialog
        open={confirmOpen}
        title="刪除寵物"
        message={`確定要刪除「${petQuery.data.name}」嗎？此動作不可復原。`}
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
