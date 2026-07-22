package com.example.medicalsearch.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

public interface NearbyInstitutionRow {

    Long getId();

    String getHpid();

    String getType();

    String getName();

    String getInstitutionKind();

    String getMedicalDepartmentCodes();

    String getPhoneNumber();

    String getRoadAddress();

    BigDecimal getLatitude();

    BigDecimal getLongitude();

    Double getDistanceMeters();

    Long getOpenNow();

    Long getOperatingHoursKnown();

    LocalTime getTodayOpenTime();

    LocalTime getTodayCloseTime();

    Boolean getNightService();

    Boolean getTwentyFourHours();

    Boolean getSaturdayService();

    Boolean getSundayService();

    Boolean getHolidayService();

    LocalDateTime getLastSyncedAt();
}
