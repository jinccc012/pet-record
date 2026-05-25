import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { dailyRecordApi } from '../api/dailyRecordApi';
import type {
  CreateDailyRecordRequest,
  DailyRecord,
  UpdateDailyRecordRequest,
} from '../types/dailyRecord';

const recordsKey = (petId: number) => ['daily-records', petId] as const;

export function useDailyRecords(petId: number) {
  return useQuery<DailyRecord[]>({
    queryKey: recordsKey(petId),
    queryFn: () => dailyRecordApi.list(petId),
    enabled: Number.isFinite(petId),
  });
}

export function useCreateDailyRecord(petId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: CreateDailyRecordRequest) => dailyRecordApi.create(petId, payload),
    onSuccess: () => qc.invalidateQueries({ queryKey: recordsKey(petId) }),
  });
}

export function useUpdateDailyRecord(petId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: UpdateDailyRecordRequest }) =>
      dailyRecordApi.update(petId, id, payload),
    onSuccess: () => qc.invalidateQueries({ queryKey: recordsKey(petId) }),
  });
}

export function useDeleteDailyRecord(petId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => dailyRecordApi.remove(petId, id),
    onSuccess: () => qc.invalidateQueries({ queryKey: recordsKey(petId) }),
  });
}
