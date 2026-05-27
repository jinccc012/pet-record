import { zodResolver } from '@hookform/resolvers/zod';
import AddIcon from '@mui/icons-material/Add';
import {
  Alert,
  Box,
  Button,
  Divider,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import dayjs from 'dayjs';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { useCreateHealthRecord, useUpdateHealthRecord } from '../../hooks/useHealthRecords';
import type { HealthRecord, HealthRecordRequest, Attachment } from '../../types/healthRecord';
import { categoryFromFile, uploadFile } from '../../utils/fileUpload';
import { AttachmentChip } from './AttachmentChip';

const schema = z.object({
  visitDate: z
    .string()
    .min(1, '請選日期')
    .refine((v) => !dayjs(v).isAfter(dayjs(), 'day'), '就醫日不可為未來'),
  hospitalName: z.string().max(255, '醫院最多 255 字').optional(),
  doctorName: z.string().max(255, '醫生最多 255 字').optional(),
  medicalNote: z.string().max(5000, '備註最多 5000 字').optional(),
});

type FormValues = z.infer<typeof schema>;

function toDefaults(existing: HealthRecord | null): FormValues {
  return {
    visitDate: existing?.visitDate ?? dayjs().format('YYYY-MM-DD'),
    hospitalName: existing?.hospitalName ?? '',
    doctorName: existing?.doctorName ?? '',
    medicalNote: existing?.medicalNote ?? '',
  };
}

const ACCEPT =
  'application/pdf,image/jpeg,image/png,image/webp,video/mp4,video/quicktime,video/x-matroska,video/matroska,.mkv';

interface HealthRecordFormProps {
  petId: number;
  existing: HealthRecord | null;
  onDone: () => void;
}

export function HealthRecordForm({ petId, existing, onDone }: HealthRecordFormProps) {
  const isEdit = !!existing;
  const createMutation = useCreateHealthRecord(petId);
  const updateMutation = useUpdateHealthRecord(petId);

  const [attachments, setAttachments] = useState<Attachment[]>(existing?.attachments ?? []);
  const [uploading, setUploading] = useState(false);
  const [uploadError, setUploadError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: toDefaults(existing),
  });

  const onPickFile = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    e.target.value = ''; // allow re-selecting same file
    if (!file) return;
    const category = categoryFromFile(file);
    if (!category) {
      setUploadError(`不支援的檔案類型:${file.type || '(未知)'}`);
      return;
    }
    setUploadError(null);
    setUploading(true);
    try {
      const completed = await uploadFile(petId, file, category);
      setAttachments((prev) => [
        ...prev,
        {
          fileId: completed.fileId,
          category: completed.category,
          contentType: completed.contentType,
          originalFilename: completed.originalFilename,
          fileSize: completed.fileSize,
        },
      ]);
    } catch (err) {
      const msg = err instanceof Error ? err.message : '上傳失敗';
      setUploadError(msg);
    } finally {
      setUploading(false);
    }
  };

  const removeAttachment = (fileId: number) => {
    setAttachments((prev) => prev.filter((a) => a.fileId !== fileId));
  };

  const onSubmit = (values: FormValues) => {
    const payload: HealthRecordRequest = {
      visitDate: values.visitDate,
      hospitalName: values.hospitalName || null,
      doctorName: values.doctorName || null,
      medicalNote: values.medicalNote || null,
      attachedFileIds: attachments.map((a) => a.fileId),
    };
    if (isEdit) {
      updateMutation.mutate({ id: existing.id, payload }, { onSuccess: onDone });
    } else {
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
          label="就醫日"
          type="date"
          fullWidth
          slotProps={{ inputLabel: { shrink: true } }}
          {...register('visitDate')}
          error={!!errors.visitDate}
          helperText={errors.visitDate?.message}
        />
        <TextField
          label="醫院"
          fullWidth
          {...register('hospitalName')}
          error={!!errors.hospitalName}
          helperText={errors.hospitalName?.message}
        />
        <TextField
          label="醫生"
          fullWidth
          {...register('doctorName')}
          error={!!errors.doctorName}
          helperText={errors.doctorName?.message}
        />
        <TextField
          label="病歷備註"
          fullWidth
          multiline
          minRows={3}
          {...register('medicalNote')}
          error={!!errors.medicalNote}
          helperText={errors.medicalNote?.message}
        />

        <Divider textAlign="left">
          <Typography variant="subtitle2">附件</Typography>
        </Divider>

        {uploadError && <Alert severity="error">{uploadError}</Alert>}

        {attachments.length > 0 && (
          <Stack direction="row" spacing={1} sx={{ flexWrap: 'wrap', rowGap: 1 }}>
            {attachments.map((a) => (
              <AttachmentChip
                key={a.fileId}
                attachment={a}
                onRemove={() => removeAttachment(a.fileId)}
              />
            ))}
          </Stack>
        )}

        <Button
          component="label"
          startIcon={<AddIcon />}
          variant="outlined"
          disabled={uploading || pending}
          sx={{ alignSelf: 'flex-start' }}
        >
          {uploading ? '上傳中…' : '新增附件 (PDF / 圖片 / 影片)'}
          <input hidden type="file" accept={ACCEPT} onChange={onPickFile} />
        </Button>

        <Stack direction="row" spacing={2} sx={{ justifyContent: 'flex-end' }}>
          <Button onClick={onDone} disabled={pending}>
            取消
          </Button>
          <Button type="submit" variant="contained" disabled={pending || uploading}>
            {pending ? '儲存中…' : '儲存'}
          </Button>
        </Stack>
      </Stack>
    </Box>
  );
}
