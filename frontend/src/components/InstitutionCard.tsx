import { BedDouble, ChevronRight, Clock3, Navigation, Phone, Star } from 'lucide-react';
import { TYPE_ICONS } from '../constants/institutionIcons';
import { useAuthStore } from '../store/useAuthStore';
import type { NearbyInstitution } from '../types/institution';
import {
  formatDistance,
  formatInstitutionType,
  formatTime,
  toMapUrl
} from '../utils/institutionFormat';

interface InstitutionCardProps {
  institution: NearbyInstitution;
  favorite: boolean;
  selected: boolean;
  onFavorite: () => void;
  onSelect: () => void;
}

function InstitutionCard({ institution, favorite, selected, onFavorite, onSelect }: InstitutionCardProps) {
  const user = useAuthStore((state) => state.user);
  const TypeIcon = TYPE_ICONS[institution.type];
  const typeClass = institution.type.toLowerCase();
  const typeBadgeLabel = formatInstitutionType(institution);
  const hoursText = institution.todayOpenTime && institution.todayCloseTime
    ? `${formatTime(institution.todayOpenTime)} ~ ${formatTime(institution.todayCloseTime)}`
    : institution.operatingHoursKnown ? '오늘 휴무' : '정보 없음';
  const isEmergencyRoom = institution.type === 'EMERGENCY_ROOM';
  const hasEmergencyAvailability = isEmergencyRoom
    && institution.availableEmergencyBeds !== null;

  return (
    <article className={selected ? `institution-card ${typeClass} is-selected` : `institution-card ${typeClass}`} onClick={onSelect}>
      <div className="institution-type-icon"><TypeIcon size={25} /></div>
      <div className="institution-main">
        <div className="institution-title-row">
          <h2>{institution.name}</h2>
          <span
            className="type-badge"
            title={typeBadgeLabel}
          >
            {typeBadgeLabel}
          </span>
          {institution.type === 'EMERGENCY_ROOM'
            && institution.institutionKind
            && institution.institutionKind !== '응급의료기관'
            && <span className="kind-badge">{institution.institutionKind}</span>}
          {!isEmergencyRoom && (
            institution.operatingHoursKnown ? (
              <span className={institution.open ? 'open-badge' : 'closed-badge'}>
                {institution.open ? '진료 중' : '진료 종료'}
              </span>
            ) : (
              <span className="unknown-hours-badge">운영시간 확인 필요</span>
            )
          )}
        </div>
        <p className="institution-address">{institution.roadAddress ?? '주소 정보 없음'}</p>
        <div className="institution-distance">
          <span>{formatDistance(institution.distanceMeters)}</span>
          <i />
          <Navigation size={14} />
          <span>도보 약 {Math.max(1, Math.ceil(institution.distanceMeters / 80))}분</span>
          {institution.phoneNumber && (
            <>
              <i />
              <Phone size={13} />
              <span>{institution.phoneNumber}</span>
            </>
          )}
        </div>
      </div>
      <div className="hours-box">
        <span>
          {isEmergencyRoom ? <BedDouble size={14} /> : <Clock3 size={14} />}
          {isEmergencyRoom ? '실시간 가용 병상' : '운영시간'}
        </span>
        <strong className={hasEmergencyAvailability && institution.availableEmergencyBeds === 0 ? 'no-beds' : ''}>
          {isEmergencyRoom
            ? institution.emergencyBedAvailabilityLoading
              ? '불러오는 중'
              : hasEmergencyAvailability ? `${institution.availableEmergencyBeds}개` : '정보 없음'
            : hoursText}
        </strong>
      </div>
      <div className="card-actions">
        {user && (
          <button
            className={favorite ? 'favorite-button is-active' : 'favorite-button'}
            type="button"
            aria-label={favorite ? '즐겨찾기 해제' : '즐겨찾기 추가'}
            title={favorite ? '즐겨찾기 해제' : '즐겨찾기 추가'}
            onClick={(event) => {
              event.stopPropagation();
              onFavorite();
            }}
          >
            <Star size={21} fill={favorite ? 'currentColor' : 'none'} />
          </button>
        )}
        <a href={toMapUrl(institution)} target="_blank" rel="noreferrer" onClick={(event) => event.stopPropagation()}>
          상세보기 <ChevronRight size={17} />
        </a>
      </div>
    </article>
  );
}

export default InstitutionCard;
