import { axiosInstance } from './axiosInstance';
import type {
  CreateDailyRecordRequest,
  DailyRecord,
  UpdateDailyRecordRequest,
} from '../types/dailyRecord';

export const dailyRecordApi = {
  async list(petId: number): Promise<DailyRecord[]> {
    const { data } = await axiosInstance.get<DailyRecord[]>(`/api/pets/${petId}/daily-records`);
    return data;
  },

  async create(petId: number, payload: CreateDailyRecordRequest): Promise<DailyRecord> {
    const { data } = await axiosInstance.post<DailyRecord>(`/api/pets/${petId}/daily-records`, payload);
    return data;
  },

  async update(petId: number, id: number, payload: UpdateDailyRecordRequest): Promise<DailyRecord> {
    const { data } = await axiosInstance.put<DailyRecord>(`/api/pets/${petId}/daily-records/${id}`, payload);
    return data;
  },

  async remove(petId: number, id: number): Promise<void> {
    await axiosInstance.delete(`/api/pets/${petId}/daily-records/${id}`);
  },
};
