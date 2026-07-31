package com.example.medicalsearch.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.medicalsearch.entity.AppUser;
import com.example.medicalsearch.entity.UserRole;
import com.example.medicalsearch.repository.AppUserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

class DeveloperAccountInitializerTest {

    private final AppUserRepository userRepository = mock(AppUserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

    @Test
    void createsDefaultDeveloperAccountWhenAdminDoesNotExist() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("12341234")).thenReturn("encoded-password");
        DeveloperAccountInitializer initializer = initializer();

        initializer.run(mock(ApplicationArguments.class));

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(captor.capture());
        AppUser savedUser = captor.getValue();
        assertThat(savedUser.getUsername()).isEqualTo("admin");
        assertThat(savedUser.getPasswordHash()).isEqualTo("encoded-password");
        assertThat(savedUser.getRole()).isEqualTo(UserRole.DEVELOPER);
        assertThat(savedUser.getEmail()).isEqualTo("admin@medion.local");
    }

    @Test
    void promotesExistingAdminAndRefreshesConfiguredPassword() {
        AppUser existingUser = new AppUser(
                "admin",
                "old-password",
                "기존 관리자",
                "existing@example.com",
                "010-1111-2222",
                "서울특별시",
                new BigDecimal("37.5000000"),
                new BigDecimal("127.0000000"),
                LocalDateTime.now().minusDays(3)
        );
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.encode("12341234")).thenReturn("new-encoded-password");

        initializer().run(mock(ApplicationArguments.class));

        assertThat(existingUser.getRole()).isEqualTo(UserRole.DEVELOPER);
        assertThat(existingUser.getPasswordHash()).isEqualTo("new-encoded-password");
        verify(userRepository, never()).save(existingUser);
    }

    private DeveloperAccountInitializer initializer() {
        return new DeveloperAccountInitializer(
                userRepository,
                passwordEncoder,
                true,
                "admin",
                "12341234"
        );
    }
}
