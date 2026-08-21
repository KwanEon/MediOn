package com.example.medicalsearch.serviceImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.medicalsearch.client.EmergencyBedAvailabilityClient;
import com.example.medicalsearch.config.AppProperties;
import com.example.medicalsearch.dto.NearbyInstitutionResponse;
import com.example.medicalsearch.entity.HospitalDepartment;
import com.example.medicalsearch.entity.InstitutionType;
import com.example.medicalsearch.entity.OperatingScheduleFilter;
import com.example.medicalsearch.repository.MedicalInstitutionRepository;
import com.example.medicalsearch.repository.NearbyInstitutionRow;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class InstitutionSearchServiceImplTest {

    @Mock
    private MedicalInstitutionRepository repository;

    @Mock
    private NearbyInstitutionRow row;

    @Mock
    private EmergencyBedAvailabilityClient emergencyBedAvailabilityClient;

    private InstitutionSearchServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new InstitutionSearchServiceImpl(
                repository,
                appProperties(),
                emergencyBedAvailabilityClient
        );
    }

    @Test
    void searchesOnlyTheDatabaseWithDepartmentAndScheduleConditions() {
        when(row.getId()).thenReturn(1L);
        when(row.getType()).thenReturn("HOSPITAL");
        when(row.getName()).thenReturn("DB내과의원");
        when(row.getInstitutionKind()).thenReturn("의원");
        when(row.getMedicalDepartmentCodes()).thenReturn("D001|D005");
        when(row.getLatitude()).thenReturn(new BigDecimal("37.5000000"));
        when(row.getLongitude()).thenReturn(new BigDecimal("127.0000000"));
        when(row.getDistanceMeters()).thenReturn(120.0);
        when(row.getTodayOpenTime()).thenReturn(LocalTime.of(9, 0));
        when(row.getTodayCloseTime()).thenReturn(LocalTime.of(22, 0));
        when(row.getNightService()).thenReturn(true);
        when(repository.findNearby(
                eq(37.5),
                eq(127.0),
                eq(3000),
                eq("DB내과"),
                eq(true),
                eq(false),
                eq(false),
                anyString(),
                any(LocalTime.class),
                eq("D001"),
                eq("NIGHT"),
                eq(true),
                eq(false),
                isNull(),
                eq(PageRequest.of(0, 20))
        )).thenReturn(new PageImpl<>(List.of(row), PageRequest.of(0, 20), 1));
        when(repository.findLatestSyncedAt()).thenReturn(Optional.empty());

        NearbyInstitutionResponse response = service.searchNearby(
                37.5,
                127.0,
                3000,
                "DB내과",
                List.of(InstitutionType.HOSPITAL),
                HospitalDepartment.INTERNAL_MEDICINE,
                OperatingScheduleFilter.NIGHT,
                true,
                0,
                20
        );

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).medicalDepartments())
                .containsExactlyInAnyOrder("내과", "피부과");
        assertThat(response.items().get(0).operatingSchedules())
                .contains(OperatingScheduleFilter.NIGHT);
        verify(repository).findNearby(
                eq(37.5),
                eq(127.0),
                eq(3000),
                eq("DB내과"),
                eq(true),
                eq(false),
                eq(false),
                anyString(),
                any(LocalTime.class),
                eq("D001"),
                eq("NIGHT"),
                eq(true),
                eq(false),
                isNull(),
                eq(PageRequest.of(0, 20))
        );
    }

    private AppProperties appProperties() {
        URI unusedUrl = URI.create("http://127.0.0.1/unused");
        return new AppProperties(
                ZoneId.of("Asia/Seoul"),
                new AppProperties.Cors(List.of()),
                new AppProperties.PublicData(
                        false,
                        "",
                        unusedUrl,
                        unusedUrl,
                        unusedUrl,
                        unusedUrl,
                        Duration.ofSeconds(1),
                        1000,
                        false
                ),
                new AppProperties.NaverMaps(
                        unusedUrl,
                        "",
                        "",
                        Duration.ofSeconds(1)
                )
        );
    }
}
