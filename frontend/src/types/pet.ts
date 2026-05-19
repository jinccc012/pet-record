export type PetSpecies = 'DOG' | 'CAT' | 'RABBIT' | 'BIRD' | 'OTHER';
export type PetGender = 'MALE' | 'FEMALE' | 'UNKNOWN';

export const PET_SPECIES_OPTIONS: { value: PetSpecies; label: string }[] = [
  { value: 'DOG', label: '狗' },
  { value: 'CAT', label: '貓' },
  { value: 'RABBIT', label: '兔子' },
  { value: 'BIRD', label: '鳥' },
  { value: 'OTHER', label: '其他' },
];

export const PET_GENDER_OPTIONS: { value: PetGender; label: string }[] = [
  { value: 'MALE', label: '公' },
  { value: 'FEMALE', label: '母' },
  { value: 'UNKNOWN', label: '未知' },
];

export interface Pet {
  id: number;
  name: string;
  species: PetSpecies;
  breed: string | null;
  gender: PetGender | null;
  birthDate: string | null;
  color: string | null;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreatePetRequest {
  name: string;
  species: PetSpecies;
  breed?: string | null;
  gender?: PetGender | null;
  birthDate?: string | null;
  color?: string | null;
  notes?: string | null;
}

export type UpdatePetRequest = Partial<CreatePetRequest>;
