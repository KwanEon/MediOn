package com.example.medicalsearch.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.medicalsearch.client.NationalMedicalCenterFullDataClient;
import com.example.medicalsearch.client.NationalMedicalCenterFullDataClient.DailyOperatingHours;
import com.example.medicalsearch.client.NationalMedicalCenterFullDataClient.DepartmentPage;
import com.example.medicalsearch.client.NationalMedicalCenterFullDataClient.FullDataInstitution;
import com.example.medicalsearch.client.NationalMedicalCenterFullDataClient.FullDataPage;
import com.example.medicalsearch.client.PublicDataClientException;
import com.example.medicalsearch.config.AppProperties;
import com.example.medicalsearch.entity.DataSyncStatus;
import com.example.medicalsearch.repository.MedicalInstitutionSyncWriter;
import java.math.BigDecimal;
import java.net.URI;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MedicalInstitutionDataSyncServiceTest {

    @Mock
    private NationalMedicalCenterFullDataClient fullDataClient;

    @Mock
    private MedicalInstitutionSyncWriter syncWriter;

    private MedicalInstitutionDataSyncService service;

    @BeforeEach
    void setUp() {
        service = new MedicalInstitutionDataSyncService(
                fullDataClient,
                syncWriter,
                appProperties()
        );
        when(fullDataClient.isEnabled()).thenReturn(true);
    }

    @Test
    void finalizesMissingInstitutionsOnlyAfterACompleteSync() {
        FullDataInstitution institution = institution();
        when(fullDataClient.fetchFullDataPage(1))
                .thenReturn(new FullDataPage(List.of(institution), 1));
        when(fullDataClient.fetchDepartmentPage(anyString(), anyInt()))
                .thenReturn(new DepartmentPage(List.of(), 0));

        service.synchronize();

        ArgumentCaptor<String> upsertRunId = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> finalizeRunId = ArgumentCaptor.forClass(String.class);
        verify(syncWriter).upsertInstitutions(
                any(),
                upsertRunId.capture(),
                any(LocalDateTime.class)
        );
        verify(syncWriter).deactivateMissingHospitals(
                finalizeRunId.capture(),
                any(LocalDateTime.class)
        );
        verify(syncWriter).deleteStaleDepartments(finalizeRunId.getValue());
        verify(syncWriter).recordHistory(
                org.mockito.ArgumentMatchers.eq(DataSyncStatus.SUCCESS),
                any(LocalDateTime.class),
                anyString()
        );
        assertThat(finalizeRunId.getValue()).isEqualTo(upsertRunId.getValue());
    }

    @Test
    void finalizesCompleteBaseDataButKeepsDepartmentsWhenDepartmentSyncFails() {
        when(fullDataClient.fetchFullDataPage(1))
                .thenReturn(new FullDataPage(List.of(institution()), 1));
        when(fullDataClient.fetchDepartmentPage(anyString(), anyInt()))
                .thenThrow(new PublicDataClientException("department API failure"));

        service.synchronize();

        verify(syncWriter).deactivateMissingHospitals(anyString(), any(LocalDateTime.class));
        verify(syncWriter).recordHospitalBaseHistory(any(LocalDateTime.class), anyString());
        verify(syncWriter, never()).deleteStaleDepartments(anyString());
        verify(syncWriter).recordHistory(
                org.mockito.ArgumentMatchers.eq(DataSyncStatus.FAILED),
                any(LocalDateTime.class),
                anyString()
        );
    }

    @Test
    void synchronizesOnStartupWhenDatabaseIsEmptyEvenIfStartupSyncIsDisabled() {
        when(syncWriter.isMedicalInstitutionTableEmpty()).thenReturn(true);
        when(fullDataClient.fetchFullDataPage(1))
                .thenReturn(new FullDataPage(List.of(institution()), 1));
        when(fullDataClient.fetchDepartmentPage(anyString(), anyInt()))
                .thenReturn(new DepartmentPage(List.of(), 0));

        service.synchronizeOnStartup();

        verify(syncWriter, timeout(2_000)).recordHistory(
                org.mockito.ArgumentMatchers.eq(DataSyncStatus.SUCCESS),
                any(LocalDateTime.class),
                anyString()
        );
    }

    @Test
    void retriesInitialSyncWhenOnlyAPartialPageExists() {
        when(syncWriter.isMedicalInstitutionTableEmpty()).thenReturn(false);
        when(syncWriter.hasCompletedFullDataSync()).thenReturn(false);
        when(fullDataClient.fetchFullDataPage(1))
                .thenReturn(new FullDataPage(List.of(institution()), 1));
        when(fullDataClient.fetchDepartmentPage(anyString(), anyInt()))
                .thenReturn(new DepartmentPage(List.of(), 0));

        service.synchronizeOnStartup();

        verify(syncWriter, timeout(2_000)).recordHistory(
                org.mockito.ArgumentMatchers.eq(DataSyncStatus.SUCCESS),
                any(LocalDateTime.class),
                anyString()
        );
    }

    @Test
    void loadsTheNextPageAfterTheFirstThousandInstitutions() {
        List<FullDataInstitution> firstPage = IntStream.range(0, 1_000)
                .mapToObj(index -> institution("A" + String.format("%07d", index)))
                .toList();
        FullDataInstitution lastInstitution = institution("A9999999");
        when(fullDataClient.fetchFullDataPage(1))
                .thenReturn(new FullDataPage(firstPage, 1_001));
        when(fullDataClient.fetchFullDataPage(2))
                .thenReturn(new FullDataPage(List.of(lastInstitution), 1_001));
        when(fullDataClient.fetchDepartmentPage(anyString(), anyInt()))
                .thenReturn(new DepartmentPage(List.of(), 0));

        service.synchronize();

        verify(fullDataClient).fetchFullDataPage(2);
        verify(syncWriter, times(2)).upsertInstitutions(
                any(),
                anyString(),
                any(LocalDateTime.class)
        );
        verify(syncWriter).recordHistory(
                org.mockito.ArgumentMatchers.eq(DataSyncStatus.SUCCESS),
                any(LocalDateTime.class),
                anyString()
        );
    }

    @Test
    void completesWhenTotalCountIncludesDuplicateHpidRows() {
        FullDataInstitution duplicatedInstitution = institution("A0000001");
        FullDataInstitution secondInstitution = institution("A0000002");
        when(fullDataClient.fetchFullDataPage(1))
                .thenReturn(new FullDataPage(
                        List.of(duplicatedInstitution, duplicatedInstitution),
                        3
                ));
        when(fullDataClient.fetchFullDataPage(2))
                .thenReturn(new FullDataPage(List.of(secondInstitution), 3));
        when(fullDataClient.fetchDepartmentPage(anyString(), anyInt()))
                .thenReturn(new DepartmentPage(List.of(), 0));

        service.synchronize();

        verify(fullDataClient).fetchFullDataPage(2);
        verify(fullDataClient, never()).fetchFullDataPage(3);
        verify(syncWriter).deactivateMissingHospitals(anyString(), any(LocalDateTime.class));
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(syncWriter).recordHistory(
                org.mockito.ArgumentMatchers.eq(DataSyncStatus.SUCCESS),
                any(LocalDateTime.class),
                messageCaptor.capture()
        );
        assertThat(messageCaptor.getValue()).contains("기관 upsert=2");
    }

    @Test
    void restartsFullDataFromTheFirstPageWhenTotalCountChanges() {
        List<FullDataInstitution> firstPage = IntStream.range(0, 1_000)
                .mapToObj(index -> institution("A" + String.format("%07d", index)))
                .toList();
        FullDataInstitution lastInstitution = institution("A9999998");
        FullDataInstitution addedInstitution = institution("A9999999");
        when(fullDataClient.fetchFullDataPage(1))
                .thenReturn(
                        new FullDataPage(firstPage, 1_001),
                        new FullDataPage(firstPage, 1_002)
                );
        when(fullDataClient.fetchFullDataPage(2))
                .thenReturn(
                        new FullDataPage(List.of(lastInstitution), 1_002),
                        new FullDataPage(List.of(lastInstitution, addedInstitution), 1_002)
                );
        when(fullDataClient.fetchDepartmentPage(anyString(), anyInt()))
                .thenReturn(new DepartmentPage(List.of(), 0));

        service.synchronize();

        verify(fullDataClient, times(2)).fetchFullDataPage(1);
        verify(fullDataClient, times(2)).fetchFullDataPage(2);
        verify(syncWriter, times(3)).upsertInstitutions(
                any(),
                anyString(),
                any(LocalDateTime.class)
        );
        verify(syncWriter).deactivateMissingHospitals(anyString(), any(LocalDateTime.class));
        verify(syncWriter).recordHistory(
                org.mockito.ArgumentMatchers.eq(DataSyncStatus.SUCCESS),
                any(LocalDateTime.class),
                anyString()
        );
    }

    @Test
    void preservesExistingInstitutionsWhenTotalCountNeverStabilizes() {
        List<FullDataInstitution> firstPage = IntStream.range(0, 1_000)
                .mapToObj(index -> institution("A" + String.format("%07d", index)))
                .toList();
        FullDataInstitution lastInstitution = institution("A9999999");
        when(fullDataClient.fetchFullDataPage(1))
                .thenReturn(
                        new FullDataPage(firstPage, 1_001),
                        new FullDataPage(firstPage, 1_002),
                        new FullDataPage(firstPage, 1_003)
                );
        when(fullDataClient.fetchFullDataPage(2))
                .thenReturn(
                        new FullDataPage(List.of(lastInstitution), 1_002),
                        new FullDataPage(List.of(lastInstitution), 1_003),
                        new FullDataPage(List.of(lastInstitution), 1_004)
                );

        service.synchronize();

        verify(fullDataClient, times(3)).fetchFullDataPage(1);
        verify(fullDataClient, times(3)).fetchFullDataPage(2);
        verify(syncWriter, never()).deactivateMissingHospitals(
                anyString(),
                any(LocalDateTime.class)
        );
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(syncWriter).recordHistory(
                org.mockito.ArgumentMatchers.eq(DataSyncStatus.FAILED),
                any(LocalDateTime.class),
                messageCaptor.capture()
        );
        assertThat(messageCaptor.getValue()).contains("3회 수집 시도");
    }

    @Test
    void restartsDepartmentFromTheFirstPageWhenTotalCountChanges() {
        when(fullDataClient.fetchFullDataPage(1))
                .thenReturn(new FullDataPage(List.of(institution()), 1));
        when(fullDataClient.fetchDepartmentPage(anyString(), anyInt()))
                .thenReturn(new DepartmentPage(List.of(), 0));
        when(fullDataClient.fetchDepartmentPage("D001", 1))
                .thenReturn(
                        new DepartmentPage(List.of("A0000001", "A0000002"), 3, 2),
                        new DepartmentPage(List.of("A0000001", "A0000002"), 4, 2)
                );
        when(fullDataClient.fetchDepartmentPage("D001", 2))
                .thenReturn(
                        new DepartmentPage(List.of("A0000003"), 4, 1),
                        new DepartmentPage(List.of("A0000003", "A0000004"), 4, 2)
                );

        service.synchronize();

        verify(fullDataClient, times(2)).fetchDepartmentPage("D001", 1);
        verify(fullDataClient, times(2)).fetchDepartmentPage("D001", 2);
        verify(syncWriter).deleteStaleDepartments(anyString());
        verify(syncWriter).recordHistory(
                org.mockito.ArgumentMatchers.eq(DataSyncStatus.SUCCESS),
                any(LocalDateTime.class),
                anyString()
        );
    }

    @Test
    void synchronizesEveryQdCode() {
        when(fullDataClient.fetchFullDataPage(1))
                .thenReturn(new FullDataPage(List.of(institution()), 1));
        when(fullDataClient.fetchDepartmentPage(anyString(), anyInt()))
                .thenReturn(new DepartmentPage(List.of(), 0));

        service.synchronize();

        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(syncWriter, times(29)).upsertDepartments(
                codeCaptor.capture(),
                any(),
                anyString()
        );
        assertThat(codeCaptor.getAllValues()).containsExactly(
                "D001", "D002", "D003", "D004", "D005", "D006", "D007", "D008",
                "D009", "D010", "D011", "D012", "D013", "D014", "D015", "D016",
                "D017", "D018", "D019", "D020", "D021", "D022", "D023", "D024",
                "D025", "D026", "D027", "D028", "D029"
        );
    }

    private FullDataInstitution institution() {
        return institution("A0000001");
    }

    private FullDataInstitution institution(String hpid) {
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
                hpid,
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

    private AppProperties appProperties() {
        URI unusedUrl = URI.create("http://127.0.0.1/unused");
        return new AppProperties(
                ZoneId.of("Asia/Seoul"),
                new AppProperties.Cors(List.of()),
                new AppProperties.PublicData(
                        true,
                        "test-service-key",
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
