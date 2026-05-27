import { axiosInstance } from './axiosInstance';
import type { HealthRecord, HealthRecordRequest } from '../types/healthRecord';

export const healthRecordApi = {
  async list(petId: number): Promise<HealthRecord[]> {
    const { data } = await axiosInstance.get<HealthRecord[]>(`/api/pets/${petId}/health-records`);
    return data;
  },

  async create(petId: number, payload: HealthRecordRequest): Promise<HealthRecord> {
    const { data } = await axiosInstance.post<HealthRecord>(`/api/pets/${petId}/health-records`, payload);
    return data;
  },

  async update(petId: number, id: number, payload: HealthRecordRequest): Promise<HealthRecord> {
    const { data } = await axiosInstance.put<HealthRecord>(`/api/pets/${petId}/health-records/${id}`, payload);
    return data;
  },

  async remove(petId: number, id: number): Promise<void> {
    await axiosInstance.delete(`/api/pets/${petId}/health-records/${id}`);
  },
};
