import type { NearbyInstitution } from '../types/institution';

export function formatTime(value: string | null): string {
  return value ? value.slice(0, 5) : '--:--';
}

export function formatDistance(value: number): string {
  return value >= 1000 ? `${(value / 1000).toFixed(1)}km` : `${value.toLocaleString('ko-KR')}m`;
}

export function toMapUrl(institution: NearbyInstitution): string {
  return `https://www.google.com/maps/search/?api=1&query=${institution.latitude},${institution.longitude}`;
}
