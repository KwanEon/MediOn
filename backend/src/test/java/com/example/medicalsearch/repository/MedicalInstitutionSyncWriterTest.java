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
        writer.upsertInstitutions(List.of(institution()), "sync-run-1", syncedAt);

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
    void storesOnlyQdCodesInDepartmentRelations() {
        writer.upsertDepartments(
                "D013",
                List.of("A0000001"),
                "sync-run-1"
        );

        ArgumentCaptor<String> batchSql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<SqlParameterSource[]> batchParameters =
                ArgumentCaptor.forClass(SqlParameterSource[].class);
        verify(jdbcTemplate).batchUpdate(batchSql.capture(), batchParameters.capture());
        assertThat(batchSql.getValue())
                .contains("department_code", ":departmentCode")
                .doesNotContain("department_name", ":departmentName");
        assertThat(batchParameters.getValue()[0].getValue("departmentCode"))
                .isEqualTo("D013");
    }

    @Test
    void writesPharmaciesWithPharmacyTypeAndDeactivatesOnlyMissingPharmacies() {
        LocalDateTime syncedAt = LocalDateTime.of(2026, 7, 22, 3, 0);
        writer.upsertPharmacies(List.of(institution()), "pharmacy-run-1", syncedAt);

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

    private FullDataInstitution institution() {
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
                "의원",
                false,
                "02-0000-0000",
                "서울특별시 강남구 테스트로 1",
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
