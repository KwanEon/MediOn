export interface DeveloperMetrics {
  totalUsers: number;
  developerUsers: number;
  newUsersLast7Days: number;
  activeInstitutions: number;
  hospitals: number;
  pharmacies: number;
  emergencyRooms: number;
  inactiveInstitutions: number;
  staleInstitutions: number;
  latestInstitutionSync: string | null;
}

export interface DeveloperDashboard {
  generatedAt: string;
  serverStartedAt: string;
  uptimeSeconds: number;
  serviceStatus: 'OPERATIONAL';
  applicationVersion: string;
  metrics: DeveloperMetrics;
  syncState: {
    publicDataEnabled: boolean;
    hospitalSyncRunning: boolean;
    pharmacySyncRunning: boolean;
  };
  externalServices: ExternalServiceStatus[];
  recentSyncs: SyncHistory[];
}

export interface ExternalServiceStatus {
  key: string;
  name: string;
  status: 'READY' | 'DISABLED' | 'CONFIG_REQUIRED';
  description: string;
}

export interface SyncHistory {
  id: number;
  sourceName: string;
  targetType: string;
  status: 'SUCCESS' | 'FAILED';
  syncedAt: string;
  message: string | null;
}

export interface DeveloperUser {
  id: number;
  username: string;
  role: 'USER' | 'DEVELOPER';
  name: string;
  email: string;
  phoneNumber: string;
  address: string;
  latitude: number;
  longitude: number;
  favoriteCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface DeveloperUserPage {
  items: DeveloperUser[];
  page: {
    number: number;
    size: number;
    totalElements: number;
    totalPages: number;
  };
}

export interface SyncTriggerResult {
  accepted: boolean;
  target: 'hospitals' | 'pharmacies';
  message: string;
}
