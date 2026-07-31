package com.example.medicalsearch.service;

import com.example.medicalsearch.client.NaverMapsGeocodingClient;
import com.example.medicalsearch.client.NaverMapsGeocodingClient.GeocodedAddress;
import com.example.medicalsearch.dto.AuthUserResponse;
import com.example.medicalsearch.dto.AddressSearchItemResponse;
import com.example.medicalsearch.dto.RegisterRequest;
import com.example.medicalsearch.dto.UpdateAddressRequest;
import com.example.medicalsearch.dto.UpdateProfileRequest;
import com.example.medicalsearch.entity.AppUser;
import com.example.medicalsearch.repository.AppUserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService implements UserDetailsService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final NaverMapsGeocodingClient geocodingClient;

    public AuthService(
            AppUserRepository userRepository,
            PasswordEncoder passwordEncoder,
            NaverMapsGeocodingClient geocodingClient
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.geocodingClient = geocodingClient;
    }

    @Transactional
    public AuthUserResponse register(RegisterRequest request) {
        String username = request.username().trim();
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        GeocodedAddress geocodedAddress = geocodingClient.geocode(request.address());
        LocalDateTime now = LocalDateTime.now();
        AppUser user = new AppUser(
                username,
                passwordEncoder.encode(request.password()),
                request.name().trim(),
                email,
                request.phoneNumber().trim(),
                geocodedAddress.address(),
                geocodedAddress.latitude(),
                geocodedAddress.longitude(),
                now
        );
        return AuthUserResponse.from(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public AuthUserResponse getUser(String username) {
        return AuthUserResponse.from(findUser(username));
    }

    @Transactional
    public AuthUserResponse updateAddress(String username, UpdateAddressRequest request) {
        GeocodedAddress geocodedAddress = geocodingClient.geocode(request.address());
        AppUser user = findUser(username);
        user.updateAddress(
                geocodedAddress.address(),
                geocodedAddress.latitude(),
                geocodedAddress.longitude(),
                LocalDateTime.now()
        );
        return AuthUserResponse.from(user);
    }

    @Transactional
    public AuthUserResponse updateProfile(String username, UpdateProfileRequest request) {
        AppUser user = findUser(username);
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, user.getId())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        GeocodedAddress geocodedAddress = geocodingClient.geocode(request.address());
        user.updateProfile(
                request.name().trim(),
                email,
                geocodedAddress.address(),
                geocodedAddress.latitude(),
                geocodedAddress.longitude(),
                LocalDateTime.now()
        );
        return AuthUserResponse.from(user);
    }

    public List<AddressSearchItemResponse> searchAddresses(String query) {
        if (query == null || query.trim().length() < 2) {
            throw new IllegalArgumentException("주소를 두 글자 이상 입력해 주세요.");
        }
        return geocodingClient.search(query.trim(), 10).stream()
                .map(AddressSearchItemResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser user = findUser(username);
        return User.withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .roles(user.getRole().name())
                .build();
    }

    private AppUser findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
    }
}
