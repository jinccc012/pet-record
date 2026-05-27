import AddIcon from '@mui/icons-material/Add';
import DeleteIcon from '@mui/icons-material/Delete';
import EditIcon from '@mui/icons-material/Edit';
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
import { useState } from 'react';
import { ConfirmDialog } from '../common/ConfirmDialog';
import { EmptyState } from '../common/EmptyState';
import { ErrorState } from '../common/ErrorState';
import { Loading } from '../common/Loading';
import { useDeleteHealthRecord, useHealthRecords } from '../../hooks/useHealthRecords';
import type { HealthRecord } from '../../types/healthRecord';
import { HealthRecordForm } from './HealthRecordForm';

interface HealthRecordsTabProps {
  petId: number;
}

export function HealthRecordsTab({ petId }: HealthRecordsTabProps) {
  const { data, isLoading, isError, refetch } = useHealthRecords(petId);
  const deleteMutation = useDeleteHealthRecord(petId);

  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<HealthRecord | null>(null);
  const [toDelete, setToDelete] = useState<HealthRecord | null>(null);

  const openCreate = () => {
    setEditing(null);
    setFormOpen(true);
  };
  const openEdit = (record: HealthRecord) => {
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
        <Typography variant="h6">健康紀錄</Typography>
        <Button variant="contained" startIcon={<AddIcon />} onClick={openCreate}>
          新增就醫紀錄
        </Button>
      </Stack>

      {isLoading && <Loading />}
      {isError && <ErrorState onRetry={() => refetch()} />}
      {!isLoading && !isError && data && data.length === 0 && (
        <EmptyState title="尚無健康紀錄" description="點右上「新增就醫紀錄」開始記錄" />
      )}

      {!isLoading && !isError && data && data.length > 0 && (
        <Stack spacing={1.5}>
          {data.map((r) => (
            <Card key={r.id} variant="outlined">
              <CardContent>
                <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'flex-start' }}>
                  <Box sx={{ flexGrow: 1 }}>
                    <Typography variant="subtitle1">{r.visitDate}</Typography>
                    <Stack direction="row" spacing={1} sx={{ mt: 0.5, flexWrap: 'wrap' }}>
                      {r.hospitalName && <Chip size="small" label={r.hospitalName} />}
                      {r.doctorName && <Chip size="small" variant="outlined" label={r.doctorName} />}
                      <Chip size="small" variant="outlined" label={`附件 ${r.attachments.length}`} />
                    </Stack>
                    {r.medicalNote && (
                      <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                        {r.medicalNote}
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

      <Dialog open={formOpen} onClose={closeForm} maxWidth="md" fullWidth>
        <DialogTitle>{editing ? `編輯 ${editing.visitDate}` : '新增就醫紀錄'}</DialogTitle>
        <DialogContent dividers>
          {formOpen && <HealthRecordForm petId={petId} existing={editing} onDone={closeForm} />}
        </DialogContent>
      </Dialog>

      <ConfirmDialog
        open={!!toDelete}
        title="刪除健康紀錄"
        message={`確定刪除 ${toDelete?.visitDate ?? ''} 的紀錄嗎？附件本身會保留(之後可清理)。`}
        loading={deleteMutation.isPending}
        onCancel={() => setToDelete(null)}
        onConfirm={confirmDelete}
      />
    </Box>
  );
}
