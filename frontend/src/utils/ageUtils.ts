import dayjs from 'dayjs';

export function formatAge(birthDate: string | null | undefined): string | null {
  if (!birthDate) return null;
  const birth = dayjs(birthDate);
  if (!birth.isValid()) return null;
  const now = dayjs();
  const months = now.diff(birth, 'month');
  if (months < 0) return null;
  if (months < 12) return `${months} 個月`;
  const years = Math.floor(months / 12);
  return `${years} 歲`;
}
