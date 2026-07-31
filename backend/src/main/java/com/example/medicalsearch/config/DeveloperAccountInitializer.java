package com.example.medicalsearch.config;

import com.example.medicalsearch.entity.AppUser;
import com.example.medicalsearch.entity.UserRole;
import com.example.medicalsearch.repository.AppUserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DeveloperAccountInitializer implements ApplicationRunner {

    private static final String DEFAULT_NAME = "MediOn 개발자";
    private static final String DEFAULT_EMAIL = "admin@medion.local";
    private static final String DEFAULT_PHONE_NUMBER = "010-0000-0000";
    private static final String DEFAULT_ADDRESS = "서울특별시 중구 세종대로 110";
    private static final BigDecimal DEFAULT_LATITUDE = new BigDecimal("37.5665000");
    private static final BigDecimal DEFAULT_LONGITUDE = new BigDecimal("126.9780000");

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final boolean enabled;
    private final String username;
    private final String password;

    public DeveloperAccountInitializer(
            AppUserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.developer-account.enabled:true}") boolean enabled,
            @Value("${app.developer-account.username:admin}") String username,
            @Value("${app.developer-account.password:12341234}") String password
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.enabled = enabled;
        this.username = username.trim();
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }
        if (username.isBlank() || password.length() < 8) {
            throw new IllegalStateException("개발자 계정 아이디와 8자 이상의 비밀번호가 필요합니다.");
        }

        LocalDateTime now = LocalDateTime.now();
        String encodedPassword = passwordEncoder.encode(password);
        AppUser existingUser = userRepository.findByUsername(username).orElse(null);
        if (existingUser != null) {
            existingUser.configureDeveloperAccount(encodedPassword, now);
            return;
        }

        AppUser developer = new AppUser(
                username,
                encodedPassword,
                UserRole.DEVELOPER,
                DEFAULT_NAME,
                DEFAULT_EMAIL,
                DEFAULT_PHONE_NUMBER,
                DEFAULT_ADDRESS,
                DEFAULT_LATITUDE,
                DEFAULT_LONGITUDE,
                now
        );
        userRepository.save(developer);
    }
}
