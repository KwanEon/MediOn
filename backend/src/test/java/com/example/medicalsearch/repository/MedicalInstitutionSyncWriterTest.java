package com.example.medicalsearch.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.medicalsearch.client.NationalMedicalCenterFullDataClient.DailyOperatingHours;
import com.example.medicalsearch.client.NationalMedicalCenterFullDataClient.FullDataInstitution;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

@ExtendWith(MockitoExtension.class)
class MedicalInstitutionSyncWriterTest {

    @Mock
    private NamedParameterJdbcTemplate jdbcTemplate;

    private MedicalInstitutionSyncWriter writer;

    @BeforeEach
    void setUp() {
        writer = new MedicalInstitutionSyncWriter(jdbcTemplate);
    }

    @Test
    void usesHpidUpsertAndMarksOnlyInstitutionsMissingFromTheRunInactive() {
        LocalDateTime syncedAt = LocalDateTime.of(2026, 7, 22, 3, 0);
        String longNote = "상세안내".repeat(300);
        writer.upsertInstitutions(List.of(institution(longNote)), "sync-run-1", syncedAt);

        ArgumentCaptor<String> batchSql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<SqlParameterSource[]> batchParameters =
                ArgumentCaptor.forClass(SqlParameterSource[].class);
        verify(jdbcTemplate, times(2)).batchUpdate(
                batchSql.capture(),
                batchParameters.capture()
        );
        assertThat(batchSql.getAllValues().get(0))
                .contains("INSERT INTO medical_institutions", "hpid", "ON DUPLICATE KEY UPDATE")
                .contains(
                        "latitude = COALESCE(VALUES(latitude), latitude)",
                        "longitude = COALESCE(VALUES(longitude), longitude)"
                )
                .doesNotContain("DELETE FROM medical_institutions");
        assertThat(batchParameters.getAllValues().get(0)[0].getValue("hpid"))
                .isEqualTo("A0000001");
        assertThat(batchParameters.getAllValues().get(0)[0].getValue("note"))
                .isEqualTo(longNote);

        when(jdbcTemplate.update(anyString(), any(SqlParameterSource.class))).thenReturn(2);
        int inactiveCount = writer.deactivateMissingHospitals("sync-run-1", syncedAt);

        ArgumentCaptor<String> updateSql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).update(updateSql.capture(), any(SqlParameterSource.class));
        assertThat(updateSql.getValue())
                .contains("active = FALSE", "last_seen_sync_id <> :syncRunId")
                .doesNotContain("DELETE FROM medical_institutions");
        assertThat(inactiveCount).isEqualTo(2);
    }

    @Test
    void storesOnlyQdCodesAndAggregatesThemOntoInstitutions() {
        LocalDateTime syncedAt = LocalDateTime.of(2026, 7, 22, 3, 0);

        writer.upsertDepartments(
                "D013",
                List.of("A0000001"),
                "sync-run-1",
                syncedAt
        );

        ArgumentCaptor<String> batchSql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<SqlParameterSource[]> batchParameters =
                ArgumentCaptor.forClass(SqlParameterSource[].class);
        verify(jdbcTemplate, times(2)).batchUpdate(batchSql.capture(), batchParameters.capture());
        assertThat(batchSql.getAllValues().get(0))
                .contains("department_code", ":departmentCode")
                .doesNotContain("department_name", ":departmentName");
        assertThat(batchSql.getAllValues().get(1))
                .contains("UPDATE medical_institutions", "department_codes", "LOCATE");
        assertThat(batchParameters.getAllValues().get(0)[0].getValue("departmentCode"))
                .isEqualTo("D013");

        when(jdbcTemplate.update(anyString(), any(SqlParameterSource.class))).thenReturn(1);
        int updatedCount = writer.refreshInstitutionDepartmentCodes();

        ArgumentCaptor<String> updateSql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).update(updateSql.capture(), any(SqlParameterSource.class));
        assertThat(updateSql.getValue())
                .contains("GROUP_CONCAT", "department_code", "institution.department_codes");
        assertThat(updatedCount).isEqualTo(1);
    }

    @Test
    void writesPharmaciesWithPharmacyTypeAndDeactivatesOnlyMissingPharmacies() {
        LocalDateTime syncedAt = LocalDateTime.of(2026, 7, 22, 3, 0);
        writer.upsertPharmacies(List.of(institution(null)), "pharmacy-run-1", syncedAt);

        ArgumentCaptor<SqlParameterSource[]> batchParameters =
                ArgumentCaptor.forClass(SqlParameterSource[].class);
        verify(jdbcTemplate, times(2)).batchUpdate(anyString(), batchParameters.capture());
        assertThat(batchParameters.getAllValues().get(0)[0].getValue("type"))
                .isEqualTo("PHARMACY");

        when(jdbcTemplate.update(anyString(), any(SqlParameterSource.class))).thenReturn(3);
        int inactiveCount = writer.deactivateMissingPharmacies("pharmacy-run-1", syncedAt);

        ArgumentCaptor<String> updateSql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<SqlParameterSource> updateParameters =
                ArgumentCaptor.forClass(SqlParameterSource.class);
        verify(jdbcTemplate).update(updateSql.capture(), updateParameters.capture());
        assertThat(updateSql.getValue()).contains("WHERE type = :type", "active = FALSE");
        assertThat(updateParameters.getValue().getValue("type")).isEqualTo("PHARMACY");
        assertThat(inactiveCount).isEqualTo(3);
    }

    private FullDataInstitution institution(String note) {
        Map<DayOfWeek, DailyOperatingHours> hours = Arrays.stream(DayOfWeek.values())
                .collect(java.util.stream.Collectors.toMap(
                        day -> day,
                        day -> new DailyOperatingHours(
                                LocalTime.of(9, 0),
                                LocalTime.of(18, 0),
                                false
                        )
                ));
        return new FullDataInstitution(
                "A0000001",
                "테스트의원",
                "C",
                "의원",
                null,
                null,
                false,
                "02-0000-0000",
                null,
                "서울특별시 강남구 테스트로 1",
                "12345",
                note,
                null,
                null,
                new BigDecimal("37.5000000"),
                new BigDecimal("127.0000000"),
                hours,
                false,
                false,
                true,
                true,
                false
        );
    }
}
