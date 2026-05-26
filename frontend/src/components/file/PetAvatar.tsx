import { Avatar } from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { fileApi } from '../../api/fileApi';
import type { Pet, PetSpecies } from '../../types/pet';

const SPECIES_EMOJI: Record<PetSpecies, string> = {
  DOG: '🐶',
  CAT: '🐱',
  RABBIT: '🐰',
  BIRD: '🐦',
  OTHER: '🐾',
};

export function PetAvatar({ pet, size = 40 }: { pet: Pet; size?: number }) {
  const { data } = useQuery({
    queryKey: ['avatar-url', pet.avatarFileId],
    queryFn: () => fileApi.signedViewUrl(pet.avatarFileId as number),
    enabled: !!pet.avatarFileId,
    staleTime: 10 * 60 * 1000, // view URL valid ~15 min
  });

  return (
    <Avatar src={data?.url} sx={{ width: size, height: size, fontSize: size * 0.5 }}>
      {SPECIES_EMOJI[pet.species]}
    </Avatar>
  );
}
