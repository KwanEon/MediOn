package com.example.medicalsearch.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.medicalsearch.client.NaverMapsGeocodingClient;
import com.example.medicalsearch.client.NaverMapsGeocodingClient.GeocodedAddress;
import com.example.medicalsearch.client.OpenStreetMapStationSearchClient;
import com.example.medicalsearch.dto.AuthUserResponse;
import com.example.medicalsearch.dto.UpdateProfileRequest;
import com.example.medicalsearch.entity.AppUser;
import com.example.medicalsearch.repository.AppUserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class AuthServiceTest {

    @Test
    void updatesNameEmailAndGeocodedAddressTogether() {
        AppUserRepository userRepository = mock(AppUserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        NaverMapsGeocodingClient geocodingClient = mock(NaverMapsGeocodingClient.class);
        OpenStreetMapStationSearchClient stationSearchClient = mock(OpenStreetMapStationSearchClient.class);
        AppUser user = new AppUser(
                "member",
                "encoded-password",
                "기존 이름",
                "before@example.com",
                "010-1234-5678",
                "기존 주소",
                new BigDecimal("37.5000000"),
                new BigDecimal("127.0000000"),
                LocalDateTime.now()
        );
        ReflectionTestUtils.setField(user, "id", 7L);
        when(userRepository.findByUsername("member")).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailIgnoreCaseAndIdNot("after@example.com", 7L))
                .thenReturn(false);
        when(geocodingClient.geocode("서울특별시 중구 세종대로 110"))
                .thenReturn(new GeocodedAddress(
                        "서울특별시 중구 세종대로 110",
                        "서울특별시 중구 세종대로 110",
                        null,
                        new BigDecimal("37.5665000"),
                        new BigDecimal("126.9780000")
                ));
        AuthService service = new AuthService(
                userRepository,
                passwordEncoder,
                geocodingClient,
                stationSearchClient
        );

        AuthUserResponse response = service.updateProfile(
                "member",
                new UpdateProfileRequest(
                        "  변경 이름  ",
                        "  AFTER@EXAMPLE.COM  ",
                        "서울특별시 중구 세종대로 110"
                )
        );

        assertThat(response.name()).isEqualTo("변경 이름");
        assertThat(response.email()).isEqualTo("after@example.com");
        assertThat(response.address()).isEqualTo("서울특별시 중구 세종대로 110");
        assertThat(response.latitude()).isEqualByComparingTo("37.5665000");
        assertThat(response.longitude()).isEqualByComparingTo("126.9780000");
    }
}
