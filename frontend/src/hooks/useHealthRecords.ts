import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { healthRecordApi } from '../api/healthRecordApi';
import type { HealthRecord, HealthRecordRequest } from '../types/healthRecord';

const recordsKey = (petId: number) => ['health-records', petId] as const;

export function useHealthRecords(petId: number) {
  return useQuery<HealthRecord[]>({
    queryKey: recordsKey(petId),
    queryFn: () => healthRecordApi.list(petId),
    enabled: Number.isFinite(petId),
  });
}

export function useCreateHealthRecord(petId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: HealthRecordRequest) => healthRecordApi.create(petId, payload),
    onSuccess: () => qc.invalidateQueries({ queryKey: recordsKey(petId) }),
  });
}

export function useUpdateHealthRecord(petId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: HealthRecordRequest }) =>
      healthRecordApi.update(petId, id, payload),
    onSuccess: () => qc.invalidateQueries({ queryKey: recordsKey(petId) }),
  });
}

export function useDeleteHealthRecord(petId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => healthRecordApi.remove(petId, id),
    onSuccess: () => qc.invalidateQueries({ queryKey: recordsKey(petId) }),
  });
}
