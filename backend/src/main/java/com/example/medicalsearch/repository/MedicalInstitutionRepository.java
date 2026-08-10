package com.example.medicalsearch.repository;

import com.example.medicalsearch.entity.MedicalInstitution;
import com.example.medicalsearch.entity.InstitutionType;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MedicalInstitutionRepository extends JpaRepository<MedicalInstitution, Long> {

    @Query(value = """
            SELECT
                mi.id AS id,
                mi.hpid AS hpid,
                CASE
                    WHEN mi.type = 'HOSPITAL'
                         AND :includeEmergencyRooms = TRUE
                         AND mi.emergency_room_available = TRUE
                    THEN 'EMERGENCY_ROOM'
                    ELSE mi.type
                END AS type,
                mi.name AS name,
                mi.institution_kind_name AS institutionKind,
                (
                    SELECT GROUP_CONCAT(
                        department.department_code
                        ORDER BY department.department_code
                        SEPARATOR '|'
                    )
                    FROM medical_institution_departments department
                    WHERE department.institution_id = mi.id
                ) AS medicalDepartmentCodes,
                mi.phone_number AS phoneNumber,
                mi.road_address AS roadAddress,
                mi.latitude AS latitude,
                mi.longitude AS longitude,
                ST_Distance_Sphere(POINT(mi.longitude, mi.latitude), POINT(:lng, :lat)) AS distanceMeters,
                CASE
                    WHEN :includeEmergencyRooms = TRUE
                         AND mi.emergency_room_available = TRUE
                    THEN TRUE
                    WHEN oh.closed = FALSE
                         AND (
                             (oh.open_time < oh.close_time
                                 AND :currentTime >= oh.open_time AND :currentTime < oh.close_time)
                             OR
                             (oh.open_time > oh.close_time
                                 AND (:currentTime >= oh.open_time OR :currentTime < oh.close_time))
                         )
                    THEN TRUE
                    ELSE FALSE
                END AS openNow,
                CASE WHEN oh.id IS NULL THEN FALSE ELSE TRUE END AS operatingHoursKnown,
                CASE WHEN oh.closed = FALSE THEN oh.open_time ELSE NULL END AS todayOpenTime,
                CASE WHEN oh.closed = FALSE THEN oh.close_time ELSE NULL END AS todayCloseTime,
                mi.night_service AS nightService,
                mi.twenty_four_hours AS twentyFourHours,
                mi.saturday_service AS saturdayService,
                mi.sunday_service AS sundayService,
                mi.holiday_service AS holidayService,
                mi.last_synced_at AS lastSyncedAt
            FROM medical_institutions mi
            LEFT JOIN operating_hours oh
              ON oh.institution_id = mi.id
             AND oh.day_of_week = :dayOfWeek
            WHERE mi.active = TRUE
              AND (
                    :favoriteUsername IS NULL
                    OR EXISTS (
                        SELECT 1
                        FROM user_favorites favorite
                        JOIN app_users favorite_user ON favorite_user.id = favorite.user_id
                        WHERE favorite.institution_id = mi.id
                          AND favorite_user.username = :favoriteUsername
                    )
                  )
              AND (
                    :keyword IS NULL
                    OR LOCATE(:keyword, mi.name) > 0
                    OR LOCATE(:keyword, COALESCE(mi.road_address, '')) > 0
                  )
              AND (
                    (:includeHospitals = TRUE AND mi.type = 'HOSPITAL')
                    OR (:includePharmacies = TRUE AND mi.type = 'PHARMACY')
                    OR (:includeEmergencyRooms = TRUE AND mi.emergency_room_available = TRUE)
                  )
              AND (
                    :departmentCode IS NULL
                    OR (
                        :departmentCode = 'KOREAN_CLINIC'
                        AND mi.institution_kind_name IN ('한의원', '한방병원')
                    )
                    OR (
                        :departmentCode <> 'KOREAN_CLINIC'
                        AND EXISTS (
                            SELECT 1
                            FROM medical_institution_departments department_filter
                            WHERE department_filter.institution_id = mi.id
                              AND department_filter.department_code = :departmentCode
                        )
                    )
                  )
              AND (
                    :operatingSchedule = 'ALL'
                    OR (:operatingSchedule = 'NIGHT' AND mi.night_service = TRUE)
                    OR (:operatingSchedule = 'TWENTY_FOUR_HOURS' AND mi.twenty_four_hours = TRUE)
                    OR (:operatingSchedule = 'SATURDAY' AND mi.saturday_service = TRUE)
                    OR (:operatingSchedule = 'SUNDAY' AND mi.sunday_service = TRUE)
                    OR (:operatingSchedule = 'HOLIDAY' AND mi.holiday_service = TRUE)
                  )
              AND (
                    :openNowOnly = FALSE
                    OR (
                        :includeEmergencyRooms = TRUE
                        AND mi.emergency_room_available = TRUE
                    )
                    OR (
                        oh.closed = FALSE
                        AND (
                            (oh.open_time < oh.close_time
                                AND :currentTime >= oh.open_time AND :currentTime < oh.close_time)
                            OR
                            (oh.open_time > oh.close_time
                                AND (:currentTime >= oh.open_time OR :currentTime < oh.close_time))
                        )
                    )
                  )
              AND mi.latitude BETWEEN
                    :lat - (:radiusMeters / 111320.0)
                    AND :lat + (:radiusMeters / 111320.0)
              AND mi.longitude BETWEEN
                    :lng - (:radiusMeters / (111320.0 * GREATEST(COS(RADIANS(:lat)), 0.01)))
                    AND :lng + (:radiusMeters / (111320.0 * GREATEST(COS(RADIANS(:lat)), 0.01)))
              AND ST_Distance_Sphere(
                    POINT(mi.longitude, mi.latitude),
                    POINT(:lng, :lat)
                  ) <= :radiusMeters
            ORDER BY distanceMeters ASC
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM medical_institutions mi
            LEFT JOIN operating_hours oh
              ON oh.institution_id = mi.id
             AND oh.day_of_week = :dayOfWeek
            WHERE mi.active = TRUE
              AND (
                    :favoriteUsername IS NULL
                    OR EXISTS (
                        SELECT 1
                        FROM user_favorites favorite
                        JOIN app_users favorite_user ON favorite_user.id = favorite.user_id
                        WHERE favorite.institution_id = mi.id
                          AND favorite_user.username = :favoriteUsername
                    )
                  )
              AND (
                    :keyword IS NULL
                    OR LOCATE(:keyword, mi.name) > 0
                    OR LOCATE(:keyword, COALESCE(mi.road_address, '')) > 0
                  )
              AND (
                    (:includeHospitals = TRUE AND mi.type = 'HOSPITAL')
                    OR (:includePharmacies = TRUE AND mi.type = 'PHARMACY')
                    OR (:includeEmergencyRooms = TRUE AND mi.emergency_room_available = TRUE)
                  )
              AND (
                    :departmentCode IS NULL
                    OR (
                        :departmentCode = 'KOREAN_CLINIC'
                        AND mi.institution_kind_name IN ('한의원', '한방병원')
                    )
                    OR (
                        :departmentCode <> 'KOREAN_CLINIC'
                        AND EXISTS (
                            SELECT 1
                            FROM medical_institution_departments department_filter
                            WHERE department_filter.institution_id = mi.id
                              AND department_filter.department_code = :departmentCode
                        )
                    )
                  )
              AND (
                    :operatingSchedule = 'ALL'
                    OR (:operatingSchedule = 'NIGHT' AND mi.night_service = TRUE)
                    OR (:operatingSchedule = 'TWENTY_FOUR_HOURS' AND mi.twenty_four_hours = TRUE)
                    OR (:operatingSchedule = 'SATURDAY' AND mi.saturday_service = TRUE)
                    OR (:operatingSchedule = 'SUNDAY' AND mi.sunday_service = TRUE)
                    OR (:operatingSchedule = 'HOLIDAY' AND mi.holiday_service = TRUE)
                  )
              AND (
                    :openNowOnly = FALSE
                    OR (
                        :includeEmergencyRooms = TRUE
                        AND mi.emergency_room_available = TRUE
                    )
                    OR (
                        oh.closed = FALSE
                        AND (
                            (oh.open_time < oh.close_time
                                AND :currentTime >= oh.open_time AND :currentTime < oh.close_time)
                            OR
                            (oh.open_time > oh.close_time
                                AND (:currentTime >= oh.open_time OR :currentTime < oh.close_time))
                        )
                    )
                  )
              AND mi.latitude BETWEEN
                    :lat - (:radiusMeters / 111320.0)
                    AND :lat + (:radiusMeters / 111320.0)
              AND mi.longitude BETWEEN
                    :lng - (:radiusMeters / (111320.0 * GREATEST(COS(RADIANS(:lat)), 0.01)))
                    AND :lng + (:radiusMeters / (111320.0 * GREATEST(COS(RADIANS(:lat)), 0.01)))
              AND ST_Distance_Sphere(
                    POINT(mi.longitude, mi.latitude),
                    POINT(:lng, :lat)
                  ) <= :radiusMeters
            """,
            nativeQuery = true)
    Page<NearbyInstitutionRow> findNearby(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radiusMeters") int radiusMeters,
            @Param("keyword") String keyword,
            @Param("includeHospitals") boolean includeHospitals,
            @Param("includePharmacies") boolean includePharmacies,
            @Param("includeEmergencyRooms") boolean includeEmergencyRooms,
            @Param("dayOfWeek") String dayOfWeek,
            @Param("currentTime") LocalTime currentTime,
            @Param("departmentCode") String departmentCode,
            @Param("operatingSchedule") String operatingSchedule,
            @Param("openNowOnly") boolean openNowOnly,
            @Param("favoriteUsername") String favoriteUsername,
            Pageable pageable
    );

    @Query("select max(m.lastSyncedAt) from MedicalInstitution m where m.active = true")
    Optional<LocalDateTime> findLatestSyncedAt();

    long countByActiveTrue();

    long countByActiveTrueAndType(InstitutionType type);

    long countByActiveTrueAndEmergencyRoomAvailableTrue();

    long countByActiveFalse();

    long countByActiveTrueAndLastSyncedAtBefore(LocalDateTime threshold);

    @Query("""
            select
                m.id as id,
                m.hpid as hpid,
                m.roadAddress as roadAddress
            from MedicalInstitution m
            where m.id in :institutionIds
              and m.active = true
              and m.emergencyRoomAvailable = true
            """)
    List<EmergencyInstitutionRow> findActiveEmergencyInstitutionsByIdIn(
            @Param("institutionIds") Collection<Long> institutionIds
    );
}
