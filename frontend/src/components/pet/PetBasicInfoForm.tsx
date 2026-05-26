import { zodResolver } from '@hookform/resolvers/zod';
import { Alert, Box, Button, MenuItem, Stack, TextField, Typography } from '@mui/material';
import dayjs from 'dayjs';
import { Controller, useForm } from 'react-hook-form';
import { useNavigate } from 'react-router-dom';
import { z } from 'zod';
import { PetAvatar } from '../file/PetAvatar';
import { useAvatarUpload } from '../../hooks/useAvatarUpload';
import { useUpdatePet } from '../../hooks/usePets';
import {
  PET_GENDER_OPTIONS,
  PET_SPECIES_OPTIONS,
  type Pet,
  type PetSpecies,
  type UpdatePetRequest,
} from '../../types/pet';

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

function toForm(pet: Pet): FormValues {
  return {
    name: pet.name,
    species: pet.species,
    breed: pet.breed ?? '',
    gender: pet.gender ?? '',
    birthDate: pet.birthDate ?? '',
    color: pet.color ?? '',
    notes: pet.notes ?? '',
  };
}

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

export function PetBasicInfoForm({ pet }: { pet: Pet }) {
  const navigate = useNavigate();
  const updateMutation = useUpdatePet(pet.id);
  const avatarUpload = useAvatarUpload(pet.id);

  const onAvatarChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) avatarUpload.mutate(file);
    e.target.value = ''; // allow re-selecting the same file
  };

  const {
    control,
    register,
    reset,
    handleSubmit,
    formState: { errors, isDirty },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: toForm(pet),
  });

  const onSubmit = (values: FormValues) => {
    updateMutation.mutate(toPayload(values), { onSuccess: (updated) => reset(toForm(updated)) });
  };

  const apiError = updateMutation.error as { response?: { data?: { message?: string } } } | undefined;
  const errorMessage = apiError?.response?.data?.message;

  return (
    <Box component="form" onSubmit={handleSubmit(onSubmit)} noValidate>
      <Stack spacing={2}>
        <Stack direction="row" spacing={2} sx={{ alignItems: 'center' }}>
          <PetAvatar pet={pet} size={96} />
          <Box>
            <Button variant="outlined" component="label" disabled={avatarUpload.isPending}>
              {avatarUpload.isPending ? '上傳中…' : '更換頭像'}
              <input
                hidden
                type="file"
                accept="image/jpeg,image/png,image/webp"
                onChange={onAvatarChange}
              />
            </Button>
            {avatarUpload.isError && (
              <Typography variant="caption" color="error" sx={{ mt: 0.5, display: 'block' }}>
                頭像上傳失敗,請再試一次
              </Typography>
            )}
          </Box>
        </Stack>

        {updateMutation.isSuccess && !isDirty && <Alert severity="success">已儲存</Alert>}
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
          <Button onClick={() => navigate('/')} disabled={updateMutation.isPending}>
            返回
          </Button>
          <Button type="submit" variant="contained" disabled={updateMutation.isPending || !isDirty}>
            {updateMutation.isPending ? '儲存中…' : '儲存'}
          </Button>
        </Stack>
      </Stack>
    </Box>
  );
}
