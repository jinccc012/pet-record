import { axiosInstance } from './axiosInstance';
import type { CreatePetRequest, Pet, UpdatePetRequest } from '../types/pet';

export const petApi = {
  async list(): Promise<Pet[]> {
    const { data } = await axiosInstance.get<Pet[]>('/api/pets');
    return data;
  },

  async get(id: number): Promise<Pet> {
    const { data } = await axiosInstance.get<Pet>(`/api/pets/${id}`);
    return data;
  },

  async create(payload: CreatePetRequest): Promise<Pet> {
    const { data } = await axiosInstance.post<Pet>('/api/pets', payload);
    return data;
  },

  async update(id: number, payload: UpdatePetRequest): Promise<Pet> {
    const { data } = await axiosInstance.patch<Pet>(`/api/pets/${id}`, payload);
    return data;
  },

  async remove(id: number): Promise<void> {
    await axiosInstance.delete(`/api/pets/${id}`);
  },
};
