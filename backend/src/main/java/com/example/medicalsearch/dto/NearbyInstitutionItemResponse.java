package com.example.medicalsearch.dto;

import com.example.medicalsearch.entity.InstitutionType;
import com.example.medicalsearch.entity.OperatingScheduleFilter;
import com.example.medicalsearch.repository.NearbyInstitutionRow;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.Set;

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

    public static NearbyInstitutionItemResponse from(NearbyInstitutionRow row) {
        return new NearbyInstitutionItemResponse(
                row.getId(),
                InstitutionType.valueOf(row.getType()),
                row.getName(),
                inferInstitutionKind(InstitutionType.valueOf(row.getType()), row.getName()),
                inferMedicalDepartments(InstitutionType.valueOf(row.getType()), row.getName()),
                row.getPhoneNumber(),
                row.getRoadAddress(),
                row.getLatitude(),
                row.getLongitude(),
                Math.round(row.getDistanceMeters()),
                true,
                true,
                row.getTodayOpenTime(),
                row.getTodayCloseTime(),
                null,
                Set.of(),
                row.getLastSyncedAt()
        );
    }

    private static String inferInstitutionKind(InstitutionType type, String name) {
        if (type == InstitutionType.PHARMACY || name == null) {
            return null;
        }
        if (name.contains("종합병원")) {
            return "종합병원";
        }
        if (name.contains("한의원")) {
            return "한의원";
        }
        if (name.contains("치과")) {
            return "치과의원";
        }
        if (name.contains("의원")) {
            return "의원";
        }
        return type == InstitutionType.EMERGENCY_ROOM ? "응급의료기관" : "병원";
    }

    private static Set<String> inferMedicalDepartments(InstitutionType type, String name) {
        if (type != InstitutionType.HOSPITAL || name == null) {
            return Set.of();
        }
        String normalized = name.replaceAll("\\s+", "");
        Set<String> specialties = new LinkedHashSet<>();
        if (normalized.contains("이비인후과")) {
            specialties.add("이비인후과");
        }
        if (normalized.contains("소아청소년과")) {
            specialties.add("소아과");
        }
        if (normalized.contains("내과")) {
            specialties.add("내과");
        }
        if (normalized.contains("안과")) {
            specialties.add("안과");
        }
        if (normalized.contains("피부과")) {
            specialties.add("피부과");
        }
        if (normalized.contains("산부인과")) {
            specialties.add("산부인과");
        }
        if (normalized.contains("정형외과")) {
            specialties.add("정형외과");
        }
        if (normalized.contains("외과") && !normalized.contains("정형외과")) {
            specialties.add("외과");
        }
        return Set.copyOf(specialties);
    }
}
