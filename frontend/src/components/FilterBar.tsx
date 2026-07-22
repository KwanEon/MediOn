import { useEffect, useRef, useState } from 'react';
import {
  Ambulance,
  CalendarClock,
  Check,
  ChevronDown,
  Clock3,
  Grid2X2,
  Hospital,
  ListFilter,
  MapPin,
  Pill,
  Star,
  Stethoscope
} from 'lucide-react';
import type { LucideIcon } from 'lucide-react';
import { CATEGORY_OPTIONS, HOSPITAL_DEPARTMENT_OPTIONS } from '../constants/institutions';
import { useAuthStore } from '../store/useAuthStore';
import { useMedicalSearchStore } from '../store/useMedicalSearchStore';
import type { CategoryId, HospitalDepartmentId, OperatingScheduleFilter } from '../types/institution';

const CATEGORY_ICONS: Record<CategoryId, LucideIcon> = {
  ALL: Grid2X2,
  HOSPITAL: Hospital,
  PHARMACY: Pill,
  EMERGENCY_ROOM: Ambulance
};

const RADIUS_OPTIONS = [
  { value: '1000', label: '반경 1km' },
  { value: '2000', label: '반경 2km' },
  { value: '3000', label: '반경 3km' },
  { value: '5000', label: '반경 5km' }
] as const;

const RESULT_SIZE_OPTIONS = [100, 200, 300, 400, 500].map((size) => ({
  value: String(size),
  label: `${size}개`
}));

const OPERATING_SCHEDULE_OPTIONS = [
  { value: 'ALL', label: '전체' },
  { value: 'NIGHT', label: '야간진료' },
  { value: 'TWENTY_FOUR_HOURS', label: '24시간진료' },
  { value: 'SATURDAY', label: '토요일진료' },
  { value: 'SUNDAY', label: '일요일진료' },
  { value: 'HOLIDAY', label: '공휴일진료' }
] as const;

function FilterBar() {
  const user = useAuthStore((state) => state.user);
  const selectedCategory = useMedicalSearchStore((state) => state.selectedCategory);
  const selectedHospitalDepartment = useMedicalSearchStore((state) => state.selectedHospitalDepartment);
  const radiusMeters = useMedicalSearchStore((state) => state.radiusMeters);
  const operatingSchedule = useMedicalSearchStore((state) => state.operatingSchedule);
  const openNowOnly = useMedicalSearchStore((state) => state.openNowOnly);
  const resultSize = useMedicalSearchStore((state) => state.resultSize);
  const favoritesOnly = useMedicalSearchStore((state) => state.favoritesOnly);
  const setSelectedCategory = useMedicalSearchStore((state) => state.setSelectedCategory);
  const setSelectedHospitalDepartment = useMedicalSearchStore(
    (state) => state.setSelectedHospitalDepartment
  );
  const setRadiusMeters = useMedicalSearchStore((state) => state.setRadiusMeters);
  const setOperatingSchedule = useMedicalSearchStore((state) => state.setOperatingSchedule);
  const setOpenNowOnly = useMedicalSearchStore((state) => state.setOpenNowOnly);
  const setResultSize = useMedicalSearchStore((state) => state.setResultSize);
  const setFavoritesOnly = useMedicalSearchStore((state) => state.setFavoritesOnly);

  return (
    <section className="filter-bar" aria-label="검색 조건">
      <div className="category-tabs" role="group" aria-label="기관 유형">
        {CATEGORY_OPTIONS.map((option) => {
          const Icon = CATEGORY_ICONS[option.id];
          const categoryClass = option.id.toLowerCase();
          return (
            <button
              key={option.id}
              className={selectedCategory === option.id ? `category-button ${categoryClass} is-active` : `category-button ${categoryClass}`}
              type="button"
              onClick={() => setSelectedCategory(option.id)}
            >
              <Icon size={19} />
              {option.label}
            </button>
          );
        })}
      </div>

      <div className="filter-actions">
        {user && (
          <button
            className={favoritesOnly ? 'filter-favorite-button is-active' : 'filter-favorite-button'}
            type="button"
            aria-pressed={favoritesOnly}
            onClick={() => setFavoritesOnly(!favoritesOnly)}
          >
            <Star size={18} fill={favoritesOnly ? 'currentColor' : 'none'} />
            내 즐겨찾기
          </button>
        )}
        <button
          className={openNowOnly ? 'open-now-filter-button is-active' : 'open-now-filter-button'}
          type="button"
          aria-pressed={openNowOnly}
          onClick={() => setOpenNowOnly(!openNowOnly)}
        >
          <Clock3 size={18} />
          진료 중
        </button>
        <FilterDropdown
          ariaLabel="진료과목 종류"
          className="hospital-kind-select-control"
          icon={Stethoscope}
          value={selectedHospitalDepartment}
          options={HOSPITAL_DEPARTMENT_OPTIONS}
          onChange={(value) => setSelectedHospitalDepartment(value as HospitalDepartmentId)}
        />
        <FilterDropdown
          ariaLabel="검색 반경"
          icon={MapPin}
          value={String(radiusMeters)}
          options={RADIUS_OPTIONS}
          onChange={(value) => setRadiusMeters(Number(value))}
        />
        <FilterDropdown
          ariaLabel="검색 결과 개수"
          className="result-size-select-control"
          icon={ListFilter}
          value={String(resultSize)}
          options={RESULT_SIZE_OPTIONS}
          onChange={(value) => setResultSize(Number(value))}
        />
        <FilterDropdown
          ariaLabel="야간 및 휴일 진료"
          className="schedule-select-control"
          icon={CalendarClock}
          value={operatingSchedule}
          options={OPERATING_SCHEDULE_OPTIONS}
          onChange={(value) => setOperatingSchedule(value as OperatingScheduleFilter)}
        />
      </div>
    </section>
  );
}

interface DropdownOption {
  value: string;
  label: string;
}

interface FilterDropdownProps {
  ariaLabel: string;
  className?: string;
  icon: LucideIcon;
  value: string;
  options: readonly DropdownOption[];
  onChange: (value: string) => void;
}

function FilterDropdown({
  ariaLabel,
  className = '',
  icon: Icon,
  value,
  options,
  onChange
}: FilterDropdownProps) {
  const [open, setOpen] = useState(false);
  const rootRef = useRef<HTMLDivElement>(null);
  const buttonRef = useRef<HTMLButtonElement>(null);
  const selectedOption = options.find((option) => option.value === value) ?? options[0];

  useEffect(() => {
    if (!open) {
      return;
    }

    function handlePointerDown(event: PointerEvent) {
      if (!rootRef.current?.contains(event.target as Node)) {
        setOpen(false);
      }
    }

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        setOpen(false);
        buttonRef.current?.focus();
      }
    }

    document.addEventListener('pointerdown', handlePointerDown);
    document.addEventListener('keydown', handleKeyDown);
    return () => {
      document.removeEventListener('pointerdown', handlePointerDown);
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [open]);

  return (
    <div ref={rootRef} className={`select-dropdown ${className}${open ? ' is-open' : ''}`}>
      <button
        ref={buttonRef}
        className="select-control"
        type="button"
        aria-label={ariaLabel}
        aria-haspopup="listbox"
        aria-expanded={open}
        onClick={() => setOpen((current) => !current)}
      >
        <Icon size={18} />
        <span>{selectedOption.label}</span>
        <ChevronDown className="select-chevron" size={16} />
      </button>
      {open && (
        <div className="select-options" role="listbox" aria-label={ariaLabel}>
          {options.map((option) => {
            const selected = option.value === value;
            return (
              <button
                key={option.value}
                className={selected ? 'select-option is-selected' : 'select-option'}
                type="button"
                role="option"
                aria-selected={selected}
                onClick={() => {
                  onChange(option.value);
                  setOpen(false);
                  buttonRef.current?.focus();
                }}
              >
                <span>{option.label}</span>
                {selected && <Check size={16} />}
              </button>
            );
          })}
        </div>
      )}
    </div>
  );
}

export default FilterBar;
