import { fileApi } from '../api/fileApi';
import type { CompleteUploadResponse, FileCategory } from '../types/file';
import { compressImage } from './imageCompression';

export type UploadableCategory = Extract<
  FileCategory,
  'AVATAR' | 'HEALTH_REPORT' | 'HEALTH_IMAGE' | 'ULTRASOUND_VIDEO'
>;

async function prepare(file: File, category: UploadableCategory): Promise<File> {
  if (!file.type.startsWith('image/')) return file;
  if (category === 'AVATAR') return compressImage(file, { maxSizeMB: 1, maxWidthOrHeight: 1024 });
  if (category === 'HEALTH_IMAGE') return compressImage(file, { maxSizeMB: 2, maxWidthOrHeight: 1600 });
  return file;
}

function isMkv(filename: string): boolean {
  return filename.toLowerCase().endsWith('.mkv');
}

function uploadContentType(file: File): string {
  if (isMkv(file.name)) return 'video/x-matroska';
  return file.type;
}

export async function uploadFile(
  petId: number,
  file: File,
  category: UploadableCategory,
): Promise<CompleteUploadResponse> {
  const toUpload = await prepare(file, category);
  const contentType = uploadContentType(toUpload);
  const signed = await fileApi.signedUploadUrl({
    petId,
    category,
    originalFilename: file.name,
    contentType,
    fileSize: toUpload.size,
  });
  const res = await fetch(signed.uploadUrl, {
    method: 'PUT',
    body: toUpload,
    headers: { 'Content-Type': contentType },
  });
  if (!res.ok) {
    throw new Error(`R2 upload failed (${res.status})`);
  }
  return fileApi.completeUpload(signed.uploadSessionId);
}

export function categoryFromFile(file: File): UploadableCategory | null {
  const mime = file.type;
  if (mime === 'application/pdf') return 'HEALTH_REPORT';
  if (mime.startsWith('image/')) return 'HEALTH_IMAGE';
  if (
    mime === 'video/mp4'
    || mime === 'video/quicktime'
    || mime === 'video/x-matroska'
    || mime === 'video/matroska'
    || isMkv(file.name)
  ) {
    return 'ULTRASOUND_VIDEO';
  }
  return null;
}
