import { useMutation, useQueryClient } from '@tanstack/react-query';
import { fileApi } from '../api/fileApi';
import { compressAvatar } from '../utils/imageCompression';

// Orchestrates the Signed-URL direct-upload flow for a pet avatar:
// compress -> request signed PUT URL -> upload straight to R2 -> complete.
export function useAvatarUpload(petId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (file: File) => {
      const compressed = await compressAvatar(file);
      const signed = await fileApi.signedUploadUrl({
        petId,
        category: 'AVATAR',
        originalFilename: file.name,
        contentType: compressed.type,
        fileSize: compressed.size,
      });
      // PUT directly to R2 with a plain fetch (no app Authorization header —
      // the presigned URL carries its own signature).
      const res = await fetch(signed.uploadUrl, {
        method: 'PUT',
        body: compressed,
        headers: { 'Content-Type': compressed.type },
      });
      if (!res.ok) {
        throw new Error(`R2 upload failed (${res.status})`);
      }
      return fileApi.completeUpload(signed.uploadSessionId);
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['pet', petId] });
      qc.invalidateQueries({ queryKey: ['pets'] });
    },
  });
}
