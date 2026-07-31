package com.example.medicalsearch.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.medicalsearch.client.NationalMedicalCenterFullDataClient;
import com.example.medicalsearch.client.NationalMedicalCenterFullDataClient.DailyOperatingHours;
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
class PharmacyDataSyncServiceTest {

    @Mock
    private NationalMedicalCenterFullDataClient fullDataClient;

    @Mock
    private MedicalInstitutionSyncWriter syncWriter;

    private PharmacyDataSyncService service;

    @BeforeEach
    void setUp() {
        service = new PharmacyDataSyncService(fullDataClient, syncWriter, appProperties());
        when(fullDataClient.isEnabled()).thenReturn(true);
    }

    @Test
    void upsertsPharmaciesAndDeactivatesMissingRowsOnlyAfterCompleteSync() {
        when(fullDataClient.fetchPharmacyFullDataPage(1))
                .thenReturn(new FullDataPage(List.of(pharmacy()), 1));

        service.synchronize();

        ArgumentCaptor<String> upsertRunId = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> finalizeRunId = ArgumentCaptor.forClass(String.class);
        verify(syncWriter).upsertPharmacies(
                any(),
                upsertRunId.capture(),
                any(LocalDateTime.class)
        );
        verify(syncWriter).deactivateMissingPharmacies(
                finalizeRunId.capture(),
                any(LocalDateTime.class)
        );
        verify(syncWriter).recordPharmacyHistory(
                org.mockito.ArgumentMatchers.eq(DataSyncStatus.SUCCESS),
                any(LocalDateTime.class),
                anyString()
        );
        assertThat(finalizeRunId.getValue()).isEqualTo(upsertRunId.getValue());
    }

    @Test
    void preservesExistingPharmaciesWhenTheApiFails() {
        when(fullDataClient.fetchPharmacyFullDataPage(1))
                .thenThrow(new PublicDataClientException("pharmacy API failure"));

        service.synchronize();

        verify(syncWriter, never()).deactivateMissingPharmacies(
                anyString(),
                any(LocalDateTime.class)
        );
        verify(syncWriter).recordPharmacyHistory(
                org.mockito.ArgumentMatchers.eq(DataSyncStatus.FAILED),
                any(LocalDateTime.class),
                anyString()
        );
    }

    @Test
    void synchronizesOnStartupWhenThereAreNoActivePharmacies() {
        when(syncWriter.isPharmacyTableEmpty()).thenReturn(true);
        when(fullDataClient.fetchPharmacyFullDataPage(1))
                .thenReturn(new FullDataPage(List.of(pharmacy()), 1));

        service.synchronizeOnStartup();

        verify(syncWriter, timeout(2_000)).recordPharmacyHistory(
                org.mockito.ArgumentMatchers.eq(DataSyncStatus.SUCCESS),
                any(LocalDateTime.class),
                anyString()
        );
    }

    @Test
    void restartsFullDataFromTheFirstPageWhenTotalCountChanges() {
        List<FullDataInstitution> firstPage = IntStream.range(0, 1_000)
                .mapToObj(index -> pharmacy("B" + String.format("%07d", index)))
                .toList();
        FullDataInstitution lastPharmacy = pharmacy("B9999998");
        FullDataInstitution addedPharmacy = pharmacy("B9999999");
        when(fullDataClient.fetchPharmacyFullDataPage(1))
                .thenReturn(
                        new FullDataPage(firstPage, 1_001),
                        new FullDataPage(firstPage, 1_002)
                );
        when(fullDataClient.fetchPharmacyFullDataPage(2))
                .thenReturn(
                        new FullDataPage(List.of(lastPharmacy), 1_002),
                        new FullDataPage(List.of(lastPharmacy, addedPharmacy), 1_002)
                );

        service.synchronize();

        verify(fullDataClient, times(2)).fetchPharmacyFullDataPage(1);
        verify(fullDataClient, times(2)).fetchPharmacyFullDataPage(2);
        verify(syncWriter, times(3)).upsertPharmacies(
                any(),
                anyString(),
                any(LocalDateTime.class)
        );
        verify(syncWriter).deactivateMissingPharmacies(anyString(), any(LocalDateTime.class));
        verify(syncWriter).recordPharmacyHistory(
                org.mockito.ArgumentMatchers.eq(DataSyncStatus.SUCCESS),
                any(LocalDateTime.class),
                anyString()
        );
    }

    private FullDataInstitution pharmacy() {
        return pharmacy("B0000001");
    }

    private FullDataInstitution pharmacy(String hpid) {
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
                "테스트약국",
                "약국",
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
                new AppProperties.NaverMaps(unusedUrl, "", "", Duration.ofSeconds(1))
        );
    }
}
