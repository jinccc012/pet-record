import { axiosInstance } from './axiosInstance';
import type {
  CompleteUploadResponse,
  SignedUploadUrlRequest,
  SignedUploadUrlResponse,
  SignedViewUrlResponse,
} from '../types/file';

export const fileApi = {
  async signedUploadUrl(payload: SignedUploadUrlRequest): Promise<SignedUploadUrlResponse> {
    const { data } = await axiosInstance.post<SignedUploadUrlResponse>(
      '/api/files/signed-upload-url',
      payload,
    );
    return data;
  },

  async completeUpload(uploadSessionId: number): Promise<CompleteUploadResponse> {
    const { data } = await axiosInstance.post<CompleteUploadResponse>('/api/files/complete-upload', {
      uploadSessionId,
    });
    return data;
  },

  async signedViewUrl(fileId: number): Promise<SignedViewUrlResponse> {
    const { data } = await axiosInstance.get<SignedViewUrlResponse>(
      `/api/files/${fileId}/signed-view-url`,
    );
    return data;
  },
};
