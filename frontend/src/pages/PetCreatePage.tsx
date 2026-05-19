import { zodResolver } from '@hookform/resolvers/zod';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  MenuItem,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import dayjs from 'dayjs';
import { Controller, useForm } from 'react-hook-form';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { z } from 'zod';
import { ErrorState } from '../components/common/ErrorState';
import { useCreatePet } from '../hooks/usePets';
import {
  PET_GENDER_OPTIONS,
  PET_SPECIES_OPTIONS,
  type CreatePetRequest,
  type PetSpecies,
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

function resolveInitialSpecies(raw: string | null): PetSpecies {
  if (!raw) return 'OTHER';
  return (SPECIES_VALUES as string[]).includes(raw) ? (raw as PetSpecies) : 'OTHER';
}

function toPayload(values: FormValues): CreatePetRequest {
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

export function PetCreatePage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const initialSpecies = resolveInitialSpecies(searchParams.get('type'));

  const {
    control,
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      name: '',
      species: initialSpecies,
      breed: '',
      gender: '',
      birthDate: '',
      color: '',
      notes: '',
    },
  });

  const mutation = useCreatePet();

  const onSubmit = (values: FormValues) => {
    mutation.mutate(toPayload(values), {
      onSuccess: (pet) =>
        navigate(`/pets/${pet.id}`, { replace: true, state: { justCreated: true } }),
    });
  };

  const apiError = mutation.error as { response?: { data?: { message?: string } } } | undefined;
  const errorMessage = apiError?.response?.data?.message;

  return (
    <Card>
      <CardContent>
        <Typography variant="h5" gutterBottom>
          新增寵物
        </Typography>
        {mutation.isError && !errorMessage && (
          <ErrorState message="建立失敗，請稍後再試" onRetry={() => mutation.reset()} />
        )}
        <Box component="form" onSubmit={handleSubmit(onSubmit)} noValidate>
          <Stack spacing={2}>
            {errorMessage && <Alert severity="error">{errorMessage}</Alert>}
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
              <Button onClick={() => navigate(-1)} disabled={mutation.isPending}>
                取消
              </Button>
              <Button type="submit" variant="contained" disabled={mutation.isPending}>
                {mutation.isPending ? '建立中…' : '建立'}
              </Button>
            </Stack>
          </Stack>
        </Box>
      </CardContent>
    </Card>
  );
}
