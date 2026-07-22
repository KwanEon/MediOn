package com.example.medicalsearch.repository;

import com.example.medicalsearch.client.NationalMedicalCenterFullDataClient.DailyOperatingHours;
import com.example.medicalsearch.client.NationalMedicalCenterFullDataClient.FullDataInstitution;
import com.example.medicalsearch.entity.DataSyncStatus;
import com.example.medicalsearch.entity.InstitutionType;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class MedicalInstitutionSyncWriter {

    private static final String UPSERT_INSTITUTION_SQL = """
            INSERT INTO medical_institutions (
                hpid, type, name, institution_kind_code, institution_kind_name,
                emergency_class_code, emergency_class_name, emergency_room_available,
                phone_number, emergency_phone, road_address, lot_address, postal_code,
                note, map_description, description, latitude, longitude,
                night_service, twenty_four_hours, saturday_service, sunday_service,
                holiday_service, active, last_seen_sync_id, inactive_at,
                last_synced_at, created_at, updated_at
            ) VALUES (
                :hpid, :type, :name, :institutionKindCode, :institutionKindName,
                :emergencyClassCode, :emergencyClassName, :emergencyRoomAvailable,
                :phoneNumber, :emergencyPhone, :roadAddress, NULL, :postalCode,
                :note, :mapDescription, :description, :latitude, :longitude,
                :nightService, :twentyFourHours, :saturdayService, :sundayService,
                :holidayService, TRUE, :syncRunId, NULL,
                :syncedAt, :syncedAt, :syncedAt
            )
            ON DUPLICATE KEY UPDATE
                type = VALUES(type),
                name = VALUES(name),
                institution_kind_code = VALUES(institution_kind_code),
                institution_kind_name = VALUES(institution_kind_name),
                emergency_class_code = VALUES(emergency_class_code),
                emergency_class_name = VALUES(emergency_class_name),
                emergency_room_available = VALUES(emergency_room_available),
                phone_number = VALUES(phone_number),
                emergency_phone = VALUES(emergency_phone),
                road_address = VALUES(road_address),
                postal_code = VALUES(postal_code),
                note = VALUES(note),
                map_description = VALUES(map_description),
                description = VALUES(description),
                latitude = COALESCE(VALUES(latitude), latitude),
                longitude = COALESCE(VALUES(longitude), longitude),
                night_service = VALUES(night_service),
                twenty_four_hours = VALUES(twenty_four_hours),
                saturday_service = VALUES(saturday_service),
                sunday_service = VALUES(sunday_service),
                holiday_service = VALUES(holiday_service),
                active = TRUE,
                last_seen_sync_id = VALUES(last_seen_sync_id),
                inactive_at = NULL,
                last_synced_at = VALUES(last_synced_at),
                updated_at = VALUES(updated_at)
            """;

    private static final String UPSERT_OPERATING_HOURS_SQL = """
            INSERT INTO operating_hours (
                institution_id, day_of_week, open_time, close_time, closed,
                lunch_start_time, lunch_end_time
            )
            SELECT id, :dayOfWeek, :openTime, :closeTime, :closed, NULL, NULL
            FROM medical_institutions
            WHERE hpid = :hpid
            ON DUPLICATE KEY UPDATE
                open_time = VALUES(open_time),
                close_time = VALUES(close_time),
                closed = VALUES(closed),
                lunch_start_time = NULL,
                lunch_end_time = NULL
            """;

    private static final String UPSERT_DEPARTMENT_SQL = """
            INSERT INTO medical_institution_departments (
                institution_id, department_code, last_seen_sync_id, updated_at
            )
            SELECT id, :departmentCode, :syncRunId, :syncedAt
            FROM medical_institutions
            WHERE hpid = :hpid
            ON DUPLICATE KEY UPDATE
                last_seen_sync_id = VALUES(last_seen_sync_id),
                updated_at = VALUES(updated_at)
            """;

    private static final String APPEND_INSTITUTION_DEPARTMENT_CODE_SQL = """
            UPDATE medical_institutions
            SET department_codes = CASE
                WHEN department_codes IS NULL OR department_codes = '' THEN :departmentCode
                WHEN LOCATE(
                    CONCAT('|', :departmentCode, '|'),
                    CONCAT('|', department_codes, '|')
                ) > 0 THEN department_codes
                ELSE CONCAT(department_codes, '|', :departmentCode)
            END
            WHERE hpid = :hpid
              AND type = 'HOSPITAL'
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public MedicalInstitutionSyncWriter(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void upsertInstitutions(
            List<FullDataInstitution> institutions,
            String syncRunId,
            LocalDateTime syncedAt
    ) {
        upsertInstitutions(institutions, InstitutionType.HOSPITAL, syncRunId, syncedAt);
    }

    @Transactional
    public void upsertPharmacies(
            List<FullDataInstitution> institutions,
            String syncRunId,
            LocalDateTime syncedAt
    ) {
        upsertInstitutions(institutions, InstitutionType.PHARMACY, syncRunId, syncedAt);
    }

    private void upsertInstitutions(
            List<FullDataInstitution> institutions,
            InstitutionType type,
            String syncRunId,
            LocalDateTime syncedAt
    ) {
        if (institutions.isEmpty()) {
            return;
        }
        SqlParameterSource[] institutionParameters = institutions.stream()
                .map(institution -> institutionParameters(institution, type, syncRunId, syncedAt))
                .toArray(SqlParameterSource[]::new);
        jdbcTemplate.batchUpdate(UPSERT_INSTITUTION_SQL, institutionParameters);

        List<SqlParameterSource> operatingHourParameters = new ArrayList<>(
                institutions.size() * DayOfWeek.values().length
        );
        for (FullDataInstitution institution : institutions) {
            for (DayOfWeek dayOfWeek : DayOfWeek.values()) {
                DailyOperatingHours hours = institution.operatingHours().get(dayOfWeek);
                operatingHourParameters.add(new MapSqlParameterSource()
                        .addValue("hpid", institution.hpid())
                        .addValue("dayOfWeek", dayOfWeek.name())
                        .addValue("openTime", hours.openTime())
                        .addValue("closeTime", hours.closeTime())
                        .addValue("closed", hours.closed()));
            }
        }
        jdbcTemplate.batchUpdate(
                UPSERT_OPERATING_HOURS_SQL,
                operatingHourParameters.toArray(SqlParameterSource[]::new)
        );
    }

    @Transactional
    public void upsertDepartments(
            String departmentCode,
            List<String> hpids,
            String syncRunId,
            LocalDateTime syncedAt
    ) {
        if (hpids.isEmpty()) {
            return;
        }
        SqlParameterSource[] parameters = hpids.stream()
                .map(hpid -> new MapSqlParameterSource()
                        .addValue("hpid", hpid)
                        .addValue("departmentCode", departmentCode)
                        .addValue("syncRunId", syncRunId)
                        .addValue("syncedAt", syncedAt))
                .toArray(SqlParameterSource[]::new);
        jdbcTemplate.batchUpdate(UPSERT_DEPARTMENT_SQL, parameters);
        jdbcTemplate.batchUpdate(APPEND_INSTITUTION_DEPARTMENT_CODE_SQL, parameters);
    }

    @Transactional
    public int deactivateMissingHospitals(String syncRunId, LocalDateTime inactiveAt) {
        return deactivateMissingInstitutions(InstitutionType.HOSPITAL, syncRunId, inactiveAt);
    }

    @Transactional
    public int deactivateMissingPharmacies(String syncRunId, LocalDateTime inactiveAt) {
        return deactivateMissingInstitutions(InstitutionType.PHARMACY, syncRunId, inactiveAt);
    }

    private int deactivateMissingInstitutions(
            InstitutionType type,
            String syncRunId,
            LocalDateTime inactiveAt
    ) {
        return jdbcTemplate.update("""
                UPDATE medical_institutions
                SET active = FALSE,
                    inactive_at = :inactiveAt,
                    updated_at = :inactiveAt
                WHERE type = :type
                  AND active = TRUE
                  AND (last_seen_sync_id IS NULL OR last_seen_sync_id <> :syncRunId)
                """, new MapSqlParameterSource()
                .addValue("type", type.name())
                .addValue("syncRunId", syncRunId)
                .addValue("inactiveAt", inactiveAt));
    }

    @Transactional
    public int deleteStaleDepartments(String syncRunId) {
        return jdbcTemplate.update("""
                DELETE FROM medical_institution_departments
                WHERE last_seen_sync_id <> :syncRunId
                  AND institution_id IN (
                      SELECT id
                      FROM medical_institutions
                      WHERE type = 'HOSPITAL'
                  )
                """, new MapSqlParameterSource("syncRunId", syncRunId));
    }

    @Transactional
    public int refreshInstitutionDepartmentCodes() {
        return jdbcTemplate.update("""
                UPDATE medical_institutions institution
                LEFT JOIN (
                    SELECT institution_id,
                           GROUP_CONCAT(
                               department_code
                               ORDER BY department_code
                               SEPARATOR '|'
                           ) AS department_codes
                    FROM medical_institution_departments
                    GROUP BY institution_id
                ) departments ON departments.institution_id = institution.id
                SET institution.department_codes = departments.department_codes
                WHERE institution.type = 'HOSPITAL'
                """, new MapSqlParameterSource());
    }

    public boolean hasSuccessfulSyncSince(LocalDateTime since) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM data_sync_histories
                WHERE source_name = 'NATIONAL_MEDICAL_CENTER_FULL_DATA'
                  AND target_type = 'HOSPITAL'
                  AND status = 'SUCCESS'
                  AND synced_at >= :since
                """, new MapSqlParameterSource("since", since), Integer.class);
        return count != null && count > 0;
    }

    public boolean hasCompletedFullDataSync() {
        Integer exists = jdbcTemplate.queryForObject("""
                SELECT EXISTS (
                    SELECT 1
                    FROM data_sync_histories
                    WHERE source_name = 'NATIONAL_MEDICAL_CENTER_FULL_DATA'
                      AND target_type = 'HOSPITAL'
                      AND status = 'SUCCESS'
                    LIMIT 1
                )
                """, new MapSqlParameterSource(), Integer.class);
        return exists != null && exists == 1;
    }

    public boolean hasSuccessfulPharmacySyncSince(LocalDateTime since) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM data_sync_histories
                WHERE source_name = 'NATIONAL_MEDICAL_CENTER_PHARMACY_FULL_DATA'
                  AND target_type = 'PHARMACY'
                  AND status = 'SUCCESS'
                  AND synced_at >= :since
                """, new MapSqlParameterSource("since", since), Integer.class);
        return count != null && count > 0;
    }

    public boolean hasCompletedPharmacyFullDataSync() {
        Integer exists = jdbcTemplate.queryForObject("""
                SELECT EXISTS (
                    SELECT 1
                    FROM data_sync_histories
                    WHERE source_name = 'NATIONAL_MEDICAL_CENTER_PHARMACY_FULL_DATA'
                      AND target_type = 'PHARMACY'
                      AND status = 'SUCCESS'
                    LIMIT 1
                )
                """, new MapSqlParameterSource(), Integer.class);
        return exists != null && exists == 1;
    }

    public boolean isMedicalInstitutionTableEmpty() {
        Integer exists = jdbcTemplate.queryForObject("""
                SELECT EXISTS (
                    SELECT 1
                    FROM medical_institutions
                    LIMIT 1
                )
                """, new MapSqlParameterSource(), Integer.class);
        return exists == null || exists == 0;
    }

    public boolean isPharmacyTableEmpty() {
        Integer exists = jdbcTemplate.queryForObject("""
                SELECT EXISTS (
                    SELECT 1
                    FROM medical_institutions
                    WHERE type = 'PHARMACY'
                      AND active = TRUE
                    LIMIT 1
                )
                """, new MapSqlParameterSource(), Integer.class);
        return exists == null || exists == 0;
    }

    @Transactional
    public void recordHistory(
            DataSyncStatus status,
            LocalDateTime syncedAt,
            String message
    ) {
        jdbcTemplate.update("""
                INSERT INTO data_sync_histories (
                    source_name, target_type, status, synced_at, message
                ) VALUES (
                    'NATIONAL_MEDICAL_CENTER_FULL_DATA', 'HOSPITAL',
                    :status, :syncedAt, :message
                )
                """, new MapSqlParameterSource()
                .addValue("status", status.name())
                .addValue("syncedAt", syncedAt)
                .addValue("message", truncate(message, 1000)));
    }

    @Transactional
    public void recordPharmacyHistory(
            DataSyncStatus status,
            LocalDateTime syncedAt,
            String message
    ) {
        jdbcTemplate.update("""
                INSERT INTO data_sync_histories (
                    source_name, target_type, status, synced_at, message
                ) VALUES (
                    'NATIONAL_MEDICAL_CENTER_PHARMACY_FULL_DATA', 'PHARMACY',
                    :status, :syncedAt, :message
                )
                """, new MapSqlParameterSource()
                .addValue("status", status.name())
                .addValue("syncedAt", syncedAt)
                .addValue("message", truncate(message, 1000)));
    }

    private MapSqlParameterSource institutionParameters(
            FullDataInstitution institution,
            InstitutionType type,
            String syncRunId,
            LocalDateTime syncedAt
    ) {
        return new MapSqlParameterSource()
                .addValue("hpid", institution.hpid())
                .addValue("type", type.name())
                .addValue("name", institution.name())
                .addValue("institutionKindCode", institution.institutionKindCode())
                .addValue("institutionKindName", institution.institutionKindName())
                .addValue("emergencyClassCode", institution.emergencyClassCode())
                .addValue("emergencyClassName", institution.emergencyClassName())
                .addValue("emergencyRoomAvailable", institution.emergencyRoomAvailable())
                .addValue("phoneNumber", institution.phoneNumber())
                .addValue("emergencyPhone", institution.emergencyPhone())
                .addValue("roadAddress", institution.roadAddress())
                .addValue("postalCode", institution.postalCode())
                .addValue("note", institution.note())
                .addValue("mapDescription", institution.mapDescription())
                .addValue("description", institution.description())
                .addValue("latitude", institution.latitude())
                .addValue("longitude", institution.longitude())
                .addValue("nightService", institution.nightService())
                .addValue("twentyFourHours", institution.twentyFourHours())
                .addValue("saturdayService", institution.saturdayService())
                .addValue("sundayService", institution.sundayService())
                .addValue("holidayService", institution.holidayService())
                .addValue("syncRunId", syncRunId)
                .addValue("syncedAt", syncedAt);
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
