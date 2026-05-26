import imageCompression from 'browser-image-compression';

// Compress an avatar image to webp, max ~1MB / 1024px (plan §9.3).
export async function compressAvatar(file: File): Promise<File> {
  return imageCompression(file, {
    maxSizeMB: 1,
    maxWidthOrHeight: 1024,
    useWebWorker: true,
    fileType: 'image/webp',
  });
}
