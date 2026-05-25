import { Box, Card, CardContent, Stack, ToggleButton, ToggleButtonGroup, Typography } from '@mui/material';
import ReactECharts from 'echarts-for-react';
import { useState } from 'react';
import { EmptyState } from '../common/EmptyState';
import { ErrorState } from '../common/ErrorState';
import { Loading } from '../common/Loading';
import { useDailyChart } from '../../hooks/useDailyChart';

interface DailyChartCardProps {
  petId: number;
}

export function DailyChartCard({ petId }: DailyChartCardProps) {
  const [range, setRange] = useState(30);
  const { data, isLoading, isError, refetch } = useDailyChart(petId, range);

  const hasData = !!data && data.labels.length > 0;

  const option = {
    tooltip: { trigger: 'axis' },
    legend: { top: 4, data: ['體重 (kg)', '喝水 (ml)', '進食 (g)'] },
    grid: { left: 8, right: 8, top: 56, bottom: 8, containLabel: true },
    xAxis: { type: 'category', data: data?.labels ?? [], boundaryGap: false },
    yAxis: [
      { type: 'value', name: 'kg', position: 'left' },
      { type: 'value', name: 'ml / g', position: 'right' },
    ],
    series: [
      {
        name: '體重 (kg)',
        type: 'line',
        yAxisIndex: 0,
        data: data?.weightKg ?? [],
        connectNulls: true,
        smooth: true,
      },
      { name: '喝水 (ml)', type: 'line', yAxisIndex: 1, data: data?.waterMl ?? [], connectNulls: true },
      { name: '進食 (g)', type: 'line', yAxisIndex: 1, data: data?.foodGram ?? [] },
    ],
  };

  return (
    <Card variant="outlined" sx={{ mb: 2 }}>
      <CardContent>
        <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
          <Typography variant="h6">趨勢圖</Typography>
          <ToggleButtonGroup
            size="small"
            exclusive
            value={range}
            onChange={(_e, v) => v && setRange(v)}
          >
            <ToggleButton value={7}>近 7 天</ToggleButton>
            <ToggleButton value={30}>近 30 天</ToggleButton>
          </ToggleButtonGroup>
        </Stack>

        {isLoading && <Loading />}
        {isError && <ErrorState onRetry={() => refetch()} />}
        {!isLoading && !isError && !hasData && (
          <EmptyState title="尚無資料可繪圖" description="新增日常紀錄後這裡會顯示趨勢" />
        )}
        {!isLoading && !isError && hasData && (
          <Box sx={{ height: 320 }}>
            <ReactECharts option={option} style={{ height: '100%', width: '100%' }} notMerge />
          </Box>
        )}
      </CardContent>
    </Card>
  );
}
