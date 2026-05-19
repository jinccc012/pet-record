import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { petApi } from '../api/petApi';
import type { CreatePetRequest, Pet, UpdatePetRequest } from '../types/pet';

const petsKey = ['pets'] as const;
const petKey = (id: number) => ['pet', id] as const;

export function usePets() {
  return useQuery<Pet[]>({
    queryKey: petsKey,
    queryFn: petApi.list,
  });
}

export function usePet(id: number | undefined) {
  return useQuery<Pet>({
    queryKey: id ? petKey(id) : ['pet', 'none'],
    queryFn: () => petApi.get(id as number),
    enabled: typeof id === 'number' && !Number.isNaN(id),
  });
}

export function useCreatePet() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: CreatePetRequest) => petApi.create(payload),
    onSuccess: (pet) => {
      qc.invalidateQueries({ queryKey: petsKey });
      qc.setQueryData(petKey(pet.id), pet);
    },
  });
}

export function useUpdatePet(id: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: UpdatePetRequest) => petApi.update(id, payload),
    onSuccess: (pet) => {
      qc.setQueryData(petKey(id), pet);
      qc.invalidateQueries({ queryKey: petsKey });
    },
  });
}

export function useDeletePet() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => petApi.remove(id),
    onSuccess: (_void, id) => {
      qc.removeQueries({ queryKey: petKey(id) });
      qc.invalidateQueries({ queryKey: petsKey });
    },
  });
}
