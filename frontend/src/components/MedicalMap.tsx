import { useEffect, useRef } from 'react';
import { renderToStaticMarkup } from 'react-dom/server';
import { divIcon } from 'leaflet';
import type { DivIcon, Marker as LeafletMarker } from 'leaflet';
import { MapContainer, Marker, Popup, TileLayer, ZoomControl, useMap } from 'react-leaflet';
import { TYPE_ICONS } from '../constants/institutionIcons';
import { TYPE_META } from '../constants/institutions';
import type {
  Coordinates,
  InstitutionId,
  InstitutionType,
  NearbyInstitution
} from '../types/institution';
import { formatDistance, formatTime } from '../utils/institutionFormat';

interface MedicalMapProps {
  location: Coordinates;
  institutions: NearbyInstitution[];
  selectedId: InstitutionId | null;
  onSelect: (id: InstitutionId) => void;
}

function MedicalMap({ location, institutions, selectedId, onSelect }: MedicalMapProps) {
  const selectedInstitution = institutions.find((institution) => institution.id === selectedId) ?? null;

  return (
    <div className="map-panel" aria-label="의료기관 지도">
      <MapContainer center={[location.lat, location.lng]} zoom={14} zoomControl={false} scrollWheelZoom>
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
        <ZoomControl position="bottomright" />
        <MapViewport center={location} selectedInstitution={selectedInstitution} />
        <Marker position={[location.lat, location.lng]} icon={currentLocationIcon()}>
          <Popup>현재 위치</Popup>
        </Marker>
        {institutions.map((institution) => (
          <InstitutionMarker
            key={institution.id}
            institution={institution}
            selected={selectedId === institution.id}
            onSelect={onSelect}
          />
        ))}
      </MapContainer>
    </div>
  );
}

interface InstitutionMarkerProps {
  institution: NearbyInstitution;
  selected: boolean;
  onSelect: (id: InstitutionId) => void;
}

function InstitutionMarker({ institution, selected, onSelect }: InstitutionMarkerProps) {
  const markerRef = useRef<LeafletMarker | null>(null);
  const institutionKind = institution.type === 'HOSPITAL' && institution.institutionKind
    ? institution.institutionKind
    : TYPE_META[institution.type].label;
  const operatingStatus = institution.operatingHoursKnown
    ? institution.open ? '진료 중' : '진료 종료'
    : '운영 여부 확인 필요';
  const operatingHours = institution.todayOpenTime && institution.todayCloseTime
    ? `${formatTime(institution.todayOpenTime)} ~ ${formatTime(institution.todayCloseTime)}`
    : institution.operatingHoursKnown ? '오늘 휴무' : '정보 없음';

  useEffect(() => {
    if (selected) {
      markerRef.current?.openPopup();
    }
  }, [selected]);

  return (
    <Marker
      ref={markerRef}
      position={[institution.latitude, institution.longitude]}
      icon={institutionIcon(institution.type, selected)}
      eventHandlers={{ click: () => onSelect(institution.id) }}
    >
      <Popup minWidth={280} maxWidth={340}>
        <div className="institution-map-popup">
          <strong className="institution-map-popup-name">{institution.name}</strong>
          <dl>
            <div>
              <dt>진료 종류</dt>
              <dd title={institutionKind}>{institutionKind}</dd>
            </div>
            <div>
              <dt>진료 상태</dt>
              <dd className={institution.open ? 'is-open' : ''}>{operatingStatus}</dd>
            </div>
            <div>
              <dt>운영시간</dt>
              <dd>{operatingHours}</dd>
            </div>
            <div>
              <dt>거리</dt>
              <dd>{formatDistance(institution.distanceMeters)}</dd>
            </div>
            <div>
              <dt>주소</dt>
              <dd>{institution.roadAddress ?? '주소 정보 없음'}</dd>
            </div>
            <div>
              <dt>전화번호</dt>
              <dd>{institution.phoneNumber ?? '전화번호 정보 없음'}</dd>
            </div>
          </dl>
        </div>
      </Popup>
    </Marker>
  );
}

interface MapViewportProps {
  center: Coordinates;
  selectedInstitution: NearbyInstitution | null;
}

function MapViewport({ center, selectedInstitution }: MapViewportProps) {
  const map = useMap();

  useEffect(() => {
    const target = selectedInstitution
      ? [selectedInstitution.latitude, selectedInstitution.longitude] as const
      : [center.lat, center.lng] as const;
    const zoom = selectedInstitution ? Math.max(map.getZoom(), 16) : map.getZoom();
    map.flyTo(target, zoom, { duration: 0.7 });
  }, [
    center.lat,
    center.lng,
    map,
    selectedInstitution?.latitude,
    selectedInstitution?.longitude
  ]);

  return null;
}

function institutionIcon(type: InstitutionType, selected: boolean): DivIcon {
  const TypeIcon = TYPE_ICONS[type];
  const iconMarkup = renderToStaticMarkup(<TypeIcon size={22} strokeWidth={2.2} aria-hidden="true" />);
  return divIcon({
    className: 'custom-marker-wrapper',
    html: `<div class="institution-map-marker ${type.toLowerCase()}${selected ? ' is-selected' : ''}">${iconMarkup}</div>`,
    iconSize: [38, 46],
    iconAnchor: [19, 43],
    popupAnchor: [0, -42]
  });
}

function currentLocationIcon(): DivIcon {
  return divIcon({
    className: 'current-location-wrapper',
    html: '<span class="current-location-dot"></span>',
    iconSize: [42, 42],
    iconAnchor: [21, 21]
  });
}

export default MedicalMap;
