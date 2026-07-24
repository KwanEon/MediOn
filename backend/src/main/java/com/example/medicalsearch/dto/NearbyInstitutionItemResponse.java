package com.example.medicalsearch.dto;

import com.example.medicalsearch.entity.InstitutionType;
import com.example.medicalsearch.entity.HospitalDepartment;
import com.example.medicalsearch.entity.OperatingScheduleFilter;
import com.example.medicalsearch.repository.NearbyInstitutionRow;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

public record NearbyInstitutionItemResponse(
        Long id,
        InstitutionType type,
        String name,
        String institutionKind,
        Set<String> medicalDepartments,
        String phoneNumber,
        String roadAddress,
        BigDecimal latitude,
        BigDecimal longitude,
        long distanceMeters,
        boolean open,
        boolean operatingHoursKnown,
        LocalTime todayOpenTime,
        LocalTime todayCloseTime,
        Integer availableEmergencyBeds,
        Set<OperatingScheduleFilter> operatingSchedules,
        LocalDateTime lastSyncedAt
) {

    public static NearbyInstitutionItemResponse from(
            NearbyInstitutionRow row,
            Integer availableEmergencyBeds
    ) {
        return new NearbyInstitutionItemResponse(
                row.getId(),
                InstitutionType.valueOf(row.getType()),
                row.getName(),
                row.getInstitutionKind(),
                parseMedicalDepartments(row.getMedicalDepartmentCodes()),
                row.getPhoneNumber(),
                row.getRoadAddress(),
                row.getLatitude(),
                row.getLongitude(),
                Math.round(row.getDistanceMeters()),
                isTrue(row.getOpenNow()),
                isTrue(row.getOperatingHoursKnown()),
                row.getTodayOpenTime(),
                row.getTodayCloseTime(),
                availableEmergencyBeds,
                operatingSchedules(row),
                row.getLastSyncedAt()
        );
    }

    private static Set<String> parseMedicalDepartments(String departmentCodes) {
        if (departmentCodes == null || departmentCodes.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(departmentCodes.split("\\|"))
                .map(String::trim)
                .filter(part -> !part.isEmpty())
                .map(HospitalDepartment::officialNameFor)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static boolean isTrue(Long value) {
        return value != null && value == 1L;
    }

    private static Set<OperatingScheduleFilter> operatingSchedules(NearbyInstitutionRow row) {
        Set<OperatingScheduleFilter> schedules = EnumSet.noneOf(OperatingScheduleFilter.class);
        addIfTrue(schedules, row.getNightService(), OperatingScheduleFilter.NIGHT);
        addIfTrue(schedules, row.getTwentyFourHours(), OperatingScheduleFilter.TWENTY_FOUR_HOURS);
        addIfTrue(schedules, row.getSaturdayService(), OperatingScheduleFilter.SATURDAY);
        addIfTrue(schedules, row.getSundayService(), OperatingScheduleFilter.SUNDAY);
        addIfTrue(schedules, row.getHolidayService(), OperatingScheduleFilter.HOLIDAY);
        return Set.copyOf(schedules);
    }

    private static void addIfTrue(
            Set<OperatingScheduleFilter> schedules,
            Boolean enabled,
            OperatingScheduleFilter schedule
    ) {
        if (Boolean.TRUE.equals(enabled)) {
            schedules.add(schedule);
        }
    }
}
