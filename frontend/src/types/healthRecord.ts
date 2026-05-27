import type { FileCategory } from './file';

export interface Attachment {
  fileId: number;
  category: FileCategory;
  contentType: string;
  originalFilename: string;
  fileSize: number;
}

export interface HealthRecord {
  id: number;
  visitDate: string;
  hospitalName: string | null;
  doctorName: string | null;
  medicalNote: string | null;
  attachments: Attachment[];
  createdAt: string;
  updatedAt: string;
}

export interface HealthRecordRequest {
  visitDate: string;
  hospitalName?: string | null;
  doctorName?: string | null;
  medicalNote?: string | null;
  attachedFileIds: number[];
}
