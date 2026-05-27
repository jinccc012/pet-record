import { useMutation, useQueryClient } from '@tanstack/react-query';
import { uploadFile } from '../utils/fileUpload';

// Avatar-specific wrapper around the generic uploadFile() helper.
// Server side, complete-upload sets pet.avatar_file_id automatically when
// category is AVATAR, so we only need to invalidate the pet/list queries.
export function useAvatarUpload(petId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (file: File) => uploadFile(petId, file, 'AVATAR'),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['pet', petId] });
      qc.invalidateQueries({ queryKey: ['pets'] });
    },
  });
}
