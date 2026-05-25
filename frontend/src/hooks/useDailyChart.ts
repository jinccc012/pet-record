import { useQuery } from '@tanstack/react-query';
import dayjs from 'dayjs';
import { dailyRecordApi } from '../api/dailyRecordApi';
import type { ChartData } from '../types/chart';

export function useDailyChart(petId: number, rangeDays: number) {
  // Compute the window in the user's LOCAL timezone, then pass explicit dates.
  const to = dayjs().format('YYYY-MM-DD');
  const from = dayjs()
    .subtract(rangeDays - 1, 'day')
    .format('YYYY-MM-DD');

  return useQuery<ChartData>({
    queryKey: ['daily-chart', petId, rangeDays],
    queryFn: () => dailyRecordApi.chart(petId, from, to),
    enabled: Number.isFinite(petId),
  });
}
