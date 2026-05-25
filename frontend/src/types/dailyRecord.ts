export interface Feeding {
  id?: number;
  feedingTime: string; // "HH:mm" or "HH:mm:ss"
  foodGram: number | null;
  conditionText: string | null;
}

export interface Stool {
  id?: number;
  stoolTime: string;
  conditionText: string | null;
  abnormal: boolean;
}

export interface DailyRecord {
  id: number;
  recordDate: string; // "YYYY-MM-DD"
  weightKg: number | null;
  waterMl: number | null;
  dailyNote: string | null;
  feedings: Feeding[];
  stools: Stool[];
  createdAt: string;
  updatedAt: string;
}

export interface FeedingInput {
  feedingTime: string;
  foodGram?: number | null;
  conditionText?: string | null;
}

export interface StoolInput {
  stoolTime: string;
  conditionText?: string | null;
  abnormal: boolean;
}

export interface CreateDailyRecordRequest {
  recordDate: string;
  weightKg?: number | null;
  waterMl?: number | null;
  dailyNote?: string | null;
  feedings: FeedingInput[];
  stools: StoolInput[];
}

export type UpdateDailyRecordRequest = Omit<CreateDailyRecordRequest, 'recordDate'>;
