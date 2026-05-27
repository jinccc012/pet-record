import imageCompression from 'browser-image-compression';

export interface ImageCompressOptions {
  maxSizeMB: number;
  maxWidthOrHeight: number;
}

export function compressImage(file: File, opts: ImageCompressOptions): Promise<File> {
  return imageCompression(file, {
    maxSizeMB: opts.maxSizeMB,
    maxWidthOrHeight: opts.maxWidthOrHeight,
    useWebWorker: true,
    fileType: 'image/webp',
  });
}

// Convenience for avatar uploads (plan §9.3).
export function compressAvatar(file: File): Promise<File> {
  return compressImage(file, { maxSizeMB: 1, maxWidthOrHeight: 1024 });
}
