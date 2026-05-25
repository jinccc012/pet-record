import AddIcon from '@mui/icons-material/Add';
import {
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Dialog,
  DialogContent,
  DialogTitle,
  IconButton,
  Stack,
  Typography,
} from '@mui/material';
import DeleteIcon from '@mui/icons-material/Delete';
import EditIcon from '@mui/icons-material/Edit';
import { useState } from 'react';
import { ConfirmDialog } from '../common/ConfirmDialog';
import { EmptyState } from '../common/EmptyState';
import { ErrorState } from '../common/ErrorState';
import { Loading } from '../common/Loading';
import { useDailyRecords, useDeleteDailyRecord } from '../../hooks/useDailyRecords';
import type { DailyRecord } from '../../types/dailyRecord';
import { DailyRecordForm } from './DailyRecordForm';

interface DailyRecordsTabProps {
  petId: number;
}

export function DailyRecordsTab({ petId }: DailyRecordsTabProps) {
  const { data, isLoading, isError, refetch } = useDailyRecords(petId);
  const deleteMutation = useDeleteDailyRecord(petId);

  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<DailyRecord | null>(null);
  const [toDelete, setToDelete] = useState<DailyRecord | null>(null);

  const openCreate = () => {
    setEditing(null);
    setFormOpen(true);
  };
  const openEdit = (record: DailyRecord) => {
    setEditing(record);
    setFormOpen(true);
  };
  const closeForm = () => setFormOpen(false);

  const confirmDelete = () => {
    if (!toDelete) return;
    deleteMutation.mutate(toDelete.id, { onSuccess: () => setToDelete(null) });
  };

  return (
    <Box>
      <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Typography variant="h6">日常生活紀錄</Typography>
        <Button variant="contained" startIcon={<AddIcon />} onClick={openCreate}>
          新增當日紀錄
        </Button>
      </Stack>

      {isLoading && <Loading />}
      {isError && <ErrorState onRetry={() => refetch()} />}
      {!isLoading && !isError && data && data.length === 0 && (
        <EmptyState title="還沒有日常紀錄" description="點右上「新增當日紀錄」開始記錄" />
      )}

      {!isLoading && !isError && data && data.length > 0 && (
        <Stack spacing={1.5}>
          {data.map((r) => (
            <Card key={r.id} variant="outlined">
              <CardContent>
                <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'flex-start' }}>
                  <Box>
                    <Typography variant="subtitle1">{r.recordDate}</Typography>
                    <Stack direction="row" spacing={1} sx={{ mt: 0.5, flexWrap: 'wrap' }}>
                      {r.weightKg != null && <Chip size="small" label={`${r.weightKg} kg`} />}
                      {r.waterMl != null && <Chip size="small" label={`${r.waterMl} ml`} />}
                      <Chip size="small" variant="outlined" label={`餵食 ${r.feedings.length}`} />
                      <Chip size="small" variant="outlined" label={`排便 ${r.stools.length}`} />
                      {r.stools.some((s) => s.abnormal) && (
                        <Chip size="small" color="warning" label="排便異常" />
                      )}
                    </Stack>
                    {r.dailyNote && (
                      <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                        {r.dailyNote}
                      </Typography>
                    )}
                  </Box>
                  <Stack direction="row">
                    <IconButton aria-label="編輯" onClick={() => openEdit(r)}>
                      <EditIcon />
                    </IconButton>
                    <IconButton aria-label="刪除" color="error" onClick={() => setToDelete(r)}>
                      <DeleteIcon />
                    </IconButton>
                  </Stack>
                </Stack>
              </CardContent>
            </Card>
          ))}
        </Stack>
      )}

      <Dialog open={formOpen} onClose={closeForm} maxWidth="sm" fullWidth>
        <DialogTitle>{editing ? `編輯 ${editing.recordDate}` : '新增當日紀錄'}</DialogTitle>
        <DialogContent dividers>
          {formOpen && <DailyRecordForm petId={petId} existing={editing} onDone={closeForm} />}
        </DialogContent>
      </Dialog>

      <ConfirmDialog
        open={!!toDelete}
        title="刪除日常紀錄"
        message={`確定刪除 ${toDelete?.recordDate ?? ''} 的紀錄嗎？`}
        loading={deleteMutation.isPending}
        onCancel={() => setToDelete(null)}
        onConfirm={confirmDelete}
      />
    </Box>
  );
}
