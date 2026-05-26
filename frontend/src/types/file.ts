export type FileCategory = 'AVATAR' | 'STOOL_IMAGE' | 'HEALTH_REPORT' | 'HEALTH_IMAGE' | 'ULTRASOUND_VIDEO';

export interface SignedUploadUrlRequest {
  petId: number;
  category: FileCategory;
  originalFilename: string;
  contentType: string;
  fileSize: number;
}

export interface SignedUploadUrlResponse {
  uploadSessionId: number;
  uploadUrl: string;
  objectKey: string;
  expiresInSeconds: number;
}

export interface CompleteUploadResponse {
  fileId: number;
  category: FileCategory;
  originalFilename: string;
  contentType: string;
  fileSize: number;
}

export interface SignedViewUrlResponse {
  url: string;
  expiresInSeconds: number;
}
