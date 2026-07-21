package com.example.medicalsearch.repository;

import com.example.medicalsearch.entity.MedicalInstitution;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
                mi.type AS type,
                mi.name AS name,
                mi.phone_number AS phoneNumber,
                mi.road_address AS roadAddress,
                mi.latitude AS latitude,
                mi.longitude AS longitude,
                ST_Distance_Sphere(POINT(mi.longitude, mi.latitude), POINT(:lng, :lat)) AS distanceMeters,
                oh.open_time AS todayOpenTime,
                oh.close_time AS todayCloseTime,
                mi.last_synced_at AS lastSyncedAt
            FROM medical_institutions mi
            JOIN operating_hours oh ON oh.institution_id = mi.id
            WHERE mi.active = true
              AND mi.type IN (:types)
              AND oh.day_of_week = :dayOfWeek
              AND oh.closed = false
              AND (
                    (oh.open_time <= oh.close_time AND :currentTime >= oh.open_time AND :currentTime < oh.close_time)
                    OR
                    (oh.open_time > oh.close_time AND (:currentTime >= oh.open_time OR :currentTime < oh.close_time))
                  )
              AND (
                    oh.lunch_start_time IS NULL
                    OR oh.lunch_end_time IS NULL
                    OR NOT (:currentTime >= oh.lunch_start_time AND :currentTime < oh.lunch_end_time)
                  )
              AND ST_Distance_Sphere(POINT(mi.longitude, mi.latitude), POINT(:lng, :lat)) <= :radiusMeters
            ORDER BY distanceMeters ASC
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM medical_institutions mi
            JOIN operating_hours oh ON oh.institution_id = mi.id
            WHERE mi.active = true
              AND mi.type IN (:types)
              AND oh.day_of_week = :dayOfWeek
              AND oh.closed = false
              AND (
                    (oh.open_time <= oh.close_time AND :currentTime >= oh.open_time AND :currentTime < oh.close_time)
                    OR
                    (oh.open_time > oh.close_time AND (:currentTime >= oh.open_time OR :currentTime < oh.close_time))
                  )
              AND (
                    oh.lunch_start_time IS NULL
                    OR oh.lunch_end_time IS NULL
                    OR NOT (:currentTime >= oh.lunch_start_time AND :currentTime < oh.lunch_end_time)
                  )
              AND ST_Distance_Sphere(POINT(mi.longitude, mi.latitude), POINT(:lng, :lat)) <= :radiusMeters
            """,
            nativeQuery = true)
    Page<NearbyInstitutionRow> findOpenNearby(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radiusMeters") int radiusMeters,
            @Param("types") List<String> types,
            @Param("dayOfWeek") String dayOfWeek,
            @Param("currentTime") LocalTime currentTime,
            Pageable pageable
    );

    @Query("select max(m.lastSyncedAt) from MedicalInstitution m where m.active = true")
    Optional<LocalDateTime> findLatestSyncedAt();
}
