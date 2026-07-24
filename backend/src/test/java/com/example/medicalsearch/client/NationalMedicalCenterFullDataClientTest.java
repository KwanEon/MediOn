package com.example.medicalsearch.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.medicalsearch.client.NationalMedicalCenterFullDataClient.DepartmentPage;
import com.example.medicalsearch.client.NationalMedicalCenterFullDataClient.FullDataInstitution;
import com.example.medicalsearch.client.NationalMedicalCenterFullDataClient.FullDataPage;
import com.example.medicalsearch.config.AppProperties;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NationalMedicalCenterFullDataClientTest {

    private HttpServer server;
    private URI serverBaseUri;
    private final AtomicReference<String> fullDataQuery = new AtomicReference<>();
    private final AtomicReference<String> departmentQuery = new AtomicReference<>();
    private final AtomicReference<String> pharmacyQuery = new AtomicReference<>();

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/full-data", exchange -> {
            fullDataQuery.set(exchange.getRequestURI().getRawQuery());
            respondXml(exchange, fullDataResponse());
        });
        server.createContext("/departments", exchange -> {
            departmentQuery.set(exchange.getRequestURI().getRawQuery());
            respondXml(exchange, departmentResponse());
        });
        server.createContext("/pharmacies", exchange -> {
            pharmacyQuery.set(exchange.getRequestURI().getRawQuery());
            respondXml(exchange, fullDataResponse());
        });
        server.start();
        serverBaseUri = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void parsesFullDataAndDepartmentMembershipByHpid() {
        NationalMedicalCenterFullDataClient client = createClient();

        FullDataPage fullDataPage = client.fetchFullDataPage(1);
        FullDataPage pharmacyPage = client.fetchPharmacyFullDataPage(1);
        DepartmentPage departmentPage = client.fetchDepartmentPage("D001", 1);

        assertThat(fullDataPage.totalCount()).isEqualTo(2);
        assertThat(fullDataPage.items()).hasSize(2);
        FullDataInstitution institution = fullDataPage.items().get(0);
        assertThat(institution.hpid()).isEqualTo("A0000001");
        assertThat(institution.name()).isEqualTo("테스트내과의원");
        assertThat(institution.institutionKindName()).isEqualTo("의원");
        assertThat(institution.emergencyRoomAvailable()).isTrue();
        assertThat(institution.operatingHours().get(DayOfWeek.MONDAY).openTime())
                .isEqualTo(LocalTime.MIDNIGHT);
        assertThat(institution.operatingHours().get(DayOfWeek.MONDAY).closeTime())
                .isEqualTo(LocalTime.of(23, 59, 59));
        assertThat(institution.twentyFourHours()).isTrue();
        assertThat(institution.saturdayService()).isFalse();
        FullDataInstitution institutionWithoutCoordinates = fullDataPage.items().get(1);
        assertThat(institutionWithoutCoordinates.hpid()).isEqualTo("A0000002");
        assertThat(institutionWithoutCoordinates.latitude()).isNull();
        assertThat(institutionWithoutCoordinates.longitude()).isNull();
        assertThat(institutionWithoutCoordinates.operatingHours().get(DayOfWeek.TUESDAY).openTime())
                .isEqualTo(LocalTime.of(18, 0));
        assertThat(institutionWithoutCoordinates.operatingHours().get(DayOfWeek.TUESDAY).closeTime())
                .isEqualTo(LocalTime.of(2, 0));
        assertThat(institutionWithoutCoordinates.operatingHours().get(DayOfWeek.TUESDAY).closed())
                .isFalse();
        assertThat(institutionWithoutCoordinates.operatingHours().get(DayOfWeek.WEDNESDAY).closed())
                .isTrue();
        assertThat(institutionWithoutCoordinates.nightService()).isTrue();
        assertThat(departmentPage.hpids()).containsExactly("A0000001");
        assertThat(fullDataQuery.get())
                .contains("pageNo=1", "numOfRows=1000", "serviceKey=test-service-key");
        assertThat(departmentQuery.get()).contains("QD=D001", "ORD=NAME");
        assertThat(pharmacyPage.items()).hasSize(2);
        assertThat(pharmacyQuery.get())
                .contains("pageNo=1", "numOfRows=1000", "serviceKey=test-service-key");
    }

    private NationalMedicalCenterFullDataClient createClient() {
        URI unusedUrl = serverBaseUri.resolve("/unused");
        AppProperties properties = new AppProperties(
                ZoneId.of("Asia/Seoul"),
                new AppProperties.Cors(List.of()),
                new AppProperties.PublicData(
                        true,
                        "test-service-key",
                        serverBaseUri.resolve("/full-data"),
                        serverBaseUri.resolve("/departments"),
                        serverBaseUri.resolve("/pharmacies"),
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
        return new NationalMedicalCenterFullDataClient(properties);
    }

    private void respondXml(HttpExchange exchange, String responseBody) throws IOException {
        byte[] body = responseBody.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/xml; charset=UTF-8");
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    private String fullDataResponse() {
        return """
                <response>
                  <header><resultCode>00</resultCode><resultMsg>NORMAL SERVICE</resultMsg></header>
                  <body>
                    <items>
                      <item>
                        <hpid>A0000001</hpid>
                        <dutyName>테스트내과의원</dutyName>
                        <dutyDivNam>의원</dutyDivNam>
                        <dutyEryn>1</dutyEryn>
                        <dutyTel1>02-0000-0001</dutyTel1>
                        <dutyAddr>서울특별시 강남구 테스트로 1</dutyAddr>
                        <wgs84Lat>37.5000000</wgs84Lat>
                        <wgs84Lon>127.0000000</wgs84Lon>
                        <dutyTime1s>0000</dutyTime1s>
                        <dutyTime1c>2400</dutyTime1c>
                      </item>
                      <item>
                        <hpid>A0000002</hpid>
                        <dutyName>좌표없는의원</dutyName>
                        <dutyDivNam>의원</dutyDivNam>
                        <dutyAddr>서울특별시 강남구 좌표없는로 1</dutyAddr>
                        <dutyTime2s>1800</dutyTime2s>
                        <dutyTime2c>2600</dutyTime2c>
                        <dutyTime3s>0900</dutyTime3s>
                        <dutyTime3c>9960</dutyTime3c>
                      </item>
                    </items>
                    <numOfRows>1000</numOfRows>
                    <pageNo>1</pageNo>
                    <totalCount>2</totalCount>
                  </body>
                </response>
                """;
    }

    private String departmentResponse() {
        return """
                <response>
                  <header><resultCode>00</resultCode><resultMsg>NORMAL SERVICE</resultMsg></header>
                  <body>
                    <items><item><hpid>A0000001</hpid><dutyName>테스트내과의원</dutyName></item></items>
                    <numOfRows>1000</numOfRows>
                    <pageNo>1</pageNo>
                    <totalCount>1</totalCount>
                  </body>
                </response>
                """;
    }
}
