import { Ambulance, Hospital, Pill } from 'lucide-react';
import type { LucideIcon } from 'lucide-react';
import type { InstitutionType } from '../types/institution';

export const TYPE_ICONS: Record<InstitutionType, LucideIcon> = {
  HOSPITAL: Hospital,
  PHARMACY: Pill,
  EMERGENCY_ROOM: Ambulance
};
