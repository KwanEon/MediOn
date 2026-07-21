package com.example.medicalsearch.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

public interface NearbyInstitutionRow {

    Long getId();

    String getType();

    String getName();

    String getPhoneNumber();

    String getRoadAddress();

    BigDecimal getLatitude();

    BigDecimal getLongitude();

    Double getDistanceMeters();

    LocalTime getTodayOpenTime();

    LocalTime getTodayCloseTime();

    LocalDateTime getLastSyncedAt();
}
