import { Box, Card, CardActionArea, CardContent, Chip, Stack, Typography } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import { EmptyState } from '../components/common/EmptyState';
import { ErrorState } from '../components/common/ErrorState';
import { Loading } from '../components/common/Loading';
import { usePets } from '../hooks/usePets';
import { PET_SPECIES_OPTIONS, type PetSpecies } from '../types/pet';
import { formatAge } from '../utils/ageUtils';

const SPECIES_LABEL: Record<PetSpecies, string> = Object.fromEntries(
  PET_SPECIES_OPTIONS.map((o) => [o.value, o.label]),
) as Record<PetSpecies, string>;

const SPECIES_EMOJI: Record<PetSpecies, string> = {
  DOG: '🐶',
  CAT: '🐱',
  RABBIT: '🐰',
  BIRD: '🐦',
  OTHER: '🐾',
};

const ADD_CARDS: { type: PetSpecies; label: string }[] = [
  { type: 'DOG', label: '新增狗狗' },
  { type: 'CAT', label: '新增貓咪' },
  { type: 'OTHER', label: '其他' },
];

export function HomePage() {
  const navigate = useNavigate();
  const { data, isLoading, isError, refetch } = usePets();

  return (
    <Stack spacing={4}>
      <Box>
        <Typography variant="h5" gutterBottom>
          新增寵物
        </Typography>
        <Box
          sx={{
            display: 'grid',
            gridTemplateColumns: { xs: '1fr', sm: 'repeat(3, 1fr)' },
            gap: 2,
          }}
        >
          {ADD_CARDS.map((card) => (
            <Card key={card.type}>
              <CardActionArea
                onClick={() => navigate(`/pets/new?type=${card.type}`)}
                sx={{ py: 3, textAlign: 'center' }}
              >
                <CardContent>
                  <Typography sx={{ fontSize: 56, lineHeight: 1 }}>
                    {SPECIES_EMOJI[card.type]}
                  </Typography>
                  <Typography variant="subtitle1" sx={{ mt: 1 }}>
                    {card.label}
                  </Typography>
                </CardContent>
              </CardActionArea>
            </Card>
          ))}
        </Box>
      </Box>

      <Box>
        <Typography variant="h5" gutterBottom>
          我的寵物
        </Typography>
        {isLoading && <Loading />}
        {isError && <ErrorState onRetry={() => refetch()} />}
        {!isLoading && !isError && data && data.length === 0 && (
          <EmptyState title="還沒有寵物" description="從上方卡片新增第一隻寵物吧" />
        )}
        {!isLoading && !isError && data && data.length > 0 && (
          <Box
            sx={{
              display: 'grid',
              gridTemplateColumns: { xs: '1fr', sm: 'repeat(2, 1fr)' },
              gap: 2,
            }}
          >
            {data.map((pet) => {
              const age = formatAge(pet.birthDate);
              return (
                <Card key={pet.id}>
                  <CardActionArea onClick={() => navigate(`/pets/${pet.id}`)}>
                    <CardContent>
                      <Stack direction="row" spacing={2} sx={{ alignItems: 'center' }}>
                        <Typography sx={{ fontSize: 40, lineHeight: 1 }}>
                          {SPECIES_EMOJI[pet.species]}
                        </Typography>
                        <Box sx={{ flexGrow: 1 }}>
                          <Typography variant="h6">{pet.name}</Typography>
                          <Stack direction="row" spacing={1} sx={{ mt: 0.5, flexWrap: 'wrap' }}>
                            <Chip size="small" label={SPECIES_LABEL[pet.species]} />
                            {age && <Chip size="small" label={age} />}
                            {pet.breed && <Chip size="small" variant="outlined" label={pet.breed} />}
                          </Stack>
                        </Box>
                      </Stack>
                    </CardContent>
                  </CardActionArea>
                </Card>
              );
            })}
          </Box>
        )}
      </Box>
    </Stack>
  );
}
