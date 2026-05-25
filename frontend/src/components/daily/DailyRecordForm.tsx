import { zodResolver } from '@hookform/resolvers/zod';
import AddIcon from '@mui/icons-material/Add';
import DeleteOutlineIcon from '@mui/icons-material/Delete';
import {
  Alert,
  Box,
  Button,
  Checkbox,
  Divider,
  FormControlLabel,
  IconButton,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import dayjs from 'dayjs';
import { Controller, useFieldArray, useForm } from 'react-hook-form';
import { z } from 'zod';
import { useCreateDailyRecord, useUpdateDailyRecord } from '../../hooks/useDailyRecords';
import type {
  CreateDailyRecordRequest,
  DailyRecord,
  UpdateDailyRecordRequest,
} from '../../types/dailyRecord';

const schema = z.object({
  recordDate: z
    .string()
    .min(1, '請選日期')
    .refine((v) => !dayjs(v).isAfter(dayjs(), 'day'), '日期不可為未來'),
  weightKg: z.string().optional(),
  waterMl: z.string().optional(),
  dailyNote: z.string().max(1000, '備註最多 1000 字').optional(),
  feedings: z.array(
    z.object({
      feedingTime: z.string().min(1, '請選時間'),
      foodGram: z.string().optional(),
      conditionText: z.string().max(500).optional(),
    }),
  ),
  stools: z.array(
    z.object({
      stoolTime: z.string().min(1, '請選時間'),
      conditionText: z.string().max(500).optional(),
      abnormal: z.boolean(),
    }),
  ),
});

type FormValues = z.infer<typeof schema>;

const toTimeInput = (t: string) => (t ? t.slice(0, 5) : '');
const numOrNull = (s: string | undefined) => (s && s.trim() !== '' ? Number(s) : null);

function toDefaults(existing: DailyRecord | null): FormValues {
  if (!existing) {
    return {
      recordDate: dayjs().format('YYYY-MM-DD'),
      weightKg: '',
      waterMl: '',
      dailyNote: '',
      feedings: [],
      stools: [],
    };
  }
  return {
    recordDate: existing.recordDate,
    weightKg: existing.weightKg != null ? String(existing.weightKg) : '',
    waterMl: existing.waterMl != null ? String(existing.waterMl) : '',
    dailyNote: existing.dailyNote ?? '',
    feedings: existing.feedings.map((f) => ({
      feedingTime: toTimeInput(f.feedingTime),
      foodGram: f.foodGram != null ? String(f.foodGram) : '',
      conditionText: f.conditionText ?? '',
    })),
    stools: existing.stools.map((s) => ({
      stoolTime: toTimeInput(s.stoolTime),
      conditionText: s.conditionText ?? '',
      abnormal: s.abnormal,
    })),
  };
}

interface DailyRecordFormProps {
  petId: number;
  existing: DailyRecord | null;
  onDone: () => void;
}

export function DailyRecordForm({ petId, existing, onDone }: DailyRecordFormProps) {
  const isEdit = !!existing;
  const createMutation = useCreateDailyRecord(petId);
  const updateMutation = useUpdateDailyRecord(petId);

  const {
    control,
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: toDefaults(existing),
  });

  const feedings = useFieldArray({ control, name: 'feedings' });
  const stools = useFieldArray({ control, name: 'stools' });

  const onSubmit = (values: FormValues) => {
    const feedingsPayload = values.feedings.map((f) => ({
      feedingTime: f.feedingTime,
      foodGram: numOrNull(f.foodGram),
      conditionText: f.conditionText || null,
    }));
    const stoolsPayload = values.stools.map((s) => ({
      stoolTime: s.stoolTime,
      conditionText: s.conditionText || null,
      abnormal: s.abnormal,
    }));

    if (isEdit) {
      const payload: UpdateDailyRecordRequest = {
        weightKg: numOrNull(values.weightKg),
        waterMl: numOrNull(values.waterMl),
        dailyNote: values.dailyNote || null,
        feedings: feedingsPayload,
        stools: stoolsPayload,
      };
      updateMutation.mutate({ id: existing.id, payload }, { onSuccess: onDone });
    } else {
      const payload: CreateDailyRecordRequest = {
        recordDate: values.recordDate,
        weightKg: numOrNull(values.weightKg),
        waterMl: numOrNull(values.waterMl),
        dailyNote: values.dailyNote || null,
        feedings: feedingsPayload,
        stools: stoolsPayload,
      };
      createMutation.mutate(payload, { onSuccess: onDone });
    }
  };

  const pending = createMutation.isPending || updateMutation.isPending;
  const apiError = (createMutation.error || updateMutation.error) as
    | { response?: { data?: { message?: string } } }
    | undefined;
  const errorMessage = apiError?.response?.data?.message;

  return (
    <Box component="form" onSubmit={handleSubmit(onSubmit)} noValidate>
      <Stack spacing={2}>
        {errorMessage && <Alert severity="error">{errorMessage}</Alert>}

        <TextField
          label="日期"
          type="date"
          fullWidth
          disabled={isEdit}
          slotProps={{ inputLabel: { shrink: true } }}
          {...register('recordDate')}
          error={!!errors.recordDate}
          helperText={errors.recordDate?.message ?? (isEdit ? '日期建立後不可修改' : undefined)}
        />
        <Stack direction="row" spacing={2}>
          <TextField
            label="體重 (kg)"
            type="number"
            fullWidth
            slotProps={{ htmlInput: { step: '0.01', min: '0' } }}
            {...register('weightKg')}
          />
          <TextField
            label="喝水量 (ml)"
            type="number"
            fullWidth
            slotProps={{ htmlInput: { step: '1', min: '0' } }}
            {...register('waterMl')}
          />
        </Stack>
        <TextField
          label="當日備註"
          fullWidth
          multiline
          minRows={2}
          {...register('dailyNote')}
          error={!!errors.dailyNote}
          helperText={errors.dailyNote?.message}
        />

        <Divider textAlign="left">
          <Typography variant="subtitle2">餵食紀錄</Typography>
        </Divider>
        {feedings.fields.map((field, i) => (
          <Stack
            key={field.id}
            direction={{ xs: 'column', sm: 'row' }}
            spacing={1}
            sx={{ alignItems: { xs: 'stretch', sm: 'flex-start' } }}
          >
            <TextField
              label="時間"
              type="time"
              sx={{ width: { xs: '100%', sm: 150 }, flexShrink: 0 }}
              slotProps={{ inputLabel: { shrink: true } }}
              {...register(`feedings.${i}.feedingTime`)}
              error={!!errors.feedings?.[i]?.feedingTime}
              helperText={errors.feedings?.[i]?.feedingTime?.message}
            />
            <TextField
              label="份量 (g)"
              type="number"
              sx={{ width: { xs: '100%', sm: 240 }, flexShrink: 0 }}
              slotProps={{ htmlInput: { step: '1', min: '0' } }}
              {...register(`feedings.${i}.foodGram`)}
            />
            <TextField
              label="狀況"
              sx={{ width: { xs: '100%', sm: 'auto' }, flexGrow: 1 }}
              {...register(`feedings.${i}.conditionText`)}
            />
            <IconButton
              aria-label="刪除餵食"
              onClick={() => feedings.remove(i)}
              sx={{ alignSelf: { xs: 'flex-end', sm: 'flex-start' }, mt: { sm: 1 } }}
            >
              <DeleteOutlineIcon />
            </IconButton>
          </Stack>
        ))}
        <Button
          startIcon={<AddIcon />}
          onClick={() => feedings.append({ feedingTime: '', foodGram: '', conditionText: '' })}
          sx={{ alignSelf: 'flex-start' }}
        >
          新增餵食
        </Button>

        <Divider textAlign="left">
          <Typography variant="subtitle2">排便紀錄</Typography>
        </Divider>
        {stools.fields.map((field, i) => (
          <Stack
            key={field.id}
            direction={{ xs: 'column', sm: 'row' }}
            spacing={1}
            sx={{ alignItems: { xs: 'stretch', sm: 'flex-start' } }}
          >
            <TextField
              label="時間"
              type="time"
              sx={{ width: { xs: '100%', sm: 150 }, flexShrink: 0 }}
              slotProps={{ inputLabel: { shrink: true } }}
              {...register(`stools.${i}.stoolTime`)}
              error={!!errors.stools?.[i]?.stoolTime}
              helperText={errors.stools?.[i]?.stoolTime?.message}
            />
            <TextField
              label="狀況"
              sx={{ width: { xs: '100%', sm: 'auto' }, flexGrow: 1 }}
              {...register(`stools.${i}.conditionText`)}
            />
            <Stack
              direction="row"
              sx={{ justifyContent: 'space-between', alignItems: 'center' }}
            >
              <Controller
                control={control}
                name={`stools.${i}.abnormal`}
                render={({ field: f }) => (
                  <FormControlLabel
                    control={<Checkbox checked={f.value} onChange={(e) => f.onChange(e.target.checked)} />}
                    label="異常"
                    sx={{ whiteSpace: 'nowrap', mt: { sm: 0.5 } }}
                  />
                )}
              />
              <IconButton
                aria-label="刪除排便"
                onClick={() => stools.remove(i)}
                sx={{ mt: { sm: 1 } }}
              >
                <DeleteOutlineIcon />
              </IconButton>
            </Stack>
          </Stack>
        ))}
        <Button
          startIcon={<AddIcon />}
          onClick={() => stools.append({ stoolTime: '', conditionText: '', abnormal: false })}
          sx={{ alignSelf: 'flex-start' }}
        >
          新增排便
        </Button>

        <Stack direction="row" spacing={2} sx={{ justifyContent: 'flex-end' }}>
          <Button onClick={onDone} disabled={pending}>
            取消
          </Button>
          <Button type="submit" variant="contained" disabled={pending}>
            {pending ? '儲存中…' : '儲存'}
          </Button>
        </Stack>
      </Stack>
    </Box>
  );
}
