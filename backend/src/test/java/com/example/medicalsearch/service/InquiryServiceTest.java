package com.example.medicalsearch.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.medicalsearch.dto.InquiryCreateRequest;
import com.example.medicalsearch.dto.InquiryResponse;
import com.example.medicalsearch.entity.AppUser;
import com.example.medicalsearch.entity.Inquiry;
import com.example.medicalsearch.repository.AppUserRepository;
import com.example.medicalsearch.repository.InquiryRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class InquiryServiceTest {

    @Test
    void createsReceivedInquiryForAuthenticatedUser() {
        InquiryRepository inquiryRepository = mock(InquiryRepository.class);
        AppUserRepository userRepository = mock(AppUserRepository.class);
        AppUser user = user();
        ReflectionTestUtils.setField(user, "id", 7L);
        when(userRepository.findByUsername("member")).thenReturn(Optional.of(user));
        when(inquiryRepository.save(any(Inquiry.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        InquiryService service = new InquiryService(inquiryRepository, userRepository);

        InquiryResponse response = service.create(
                "member",
                new InquiryCreateRequest("ERROR", "  지도 오류  ", "  위치가 다르게 표시됩니다.  ")
        );

        assertThat(response.category()).isEqualTo("ERROR");
        assertThat(response.title()).isEqualTo("지도 오류");
        assertThat(response.content()).isEqualTo("위치가 다르게 표시됩니다.");
        assertThat(response.status()).isEqualTo("RECEIVED");
    }

    @Test
    void deletesOnlyAnInquiryOwnedByTheAuthenticatedUser() {
        InquiryRepository inquiryRepository = mock(InquiryRepository.class);
        AppUserRepository userRepository = mock(AppUserRepository.class);
        AppUser user = user();
        ReflectionTestUtils.setField(user, "id", 7L);
        Inquiry inquiry = new Inquiry(
                7L,
                com.example.medicalsearch.entity.InquiryCategory.GENERAL,
                "문의",
                "내용",
                LocalDateTime.now()
        );
        when(userRepository.findByUsername("member")).thenReturn(Optional.of(user));
        when(inquiryRepository.findByIdAndUserId(12L, 7L))
                .thenReturn(Optional.of(inquiry));
        InquiryService service = new InquiryService(inquiryRepository, userRepository);

        service.deleteOwn("member", 12L);

        verify(inquiryRepository).delete(inquiry);
    }

    @Test
    void developerCanDeleteAnyInquiry() {
        InquiryRepository inquiryRepository = mock(InquiryRepository.class);
        AppUserRepository userRepository = mock(AppUserRepository.class);
        Inquiry inquiry = new Inquiry(
                7L,
                com.example.medicalsearch.entity.InquiryCategory.GENERAL,
                "문의",
                "내용",
                LocalDateTime.now()
        );
        when(inquiryRepository.findById(12L)).thenReturn(Optional.of(inquiry));
        InquiryService service = new InquiryService(inquiryRepository, userRepository);

        service.deleteForDeveloper(12L);

        verify(inquiryRepository).delete(inquiry);
    }

    private AppUser user() {
        return new AppUser(
                "member",
                "encoded-password",
                "테스트 사용자",
                "member@example.com",
                "010-1234-5678",
                "서울특별시",
                new BigDecimal("37.5000000"),
                new BigDecimal("127.0000000"),
                LocalDateTime.now()
        );
    }
}
