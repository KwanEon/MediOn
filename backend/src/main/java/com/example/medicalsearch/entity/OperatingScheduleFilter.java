package com.example.medicalsearch.entity;

import java.util.Set;

public enum OperatingScheduleFilter {
    ALL,
    NIGHT,
    TWENTY_FOUR_HOURS,
    SATURDAY,
    SUNDAY,
    HOLIDAY;

    public boolean matches(Set<OperatingScheduleFilter> schedules) {
        return this == ALL || schedules.contains(this);
    }
}
