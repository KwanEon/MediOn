package com.example.medicalsearch.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.medicalsearch.dto.NoticeResponse;
import com.example.medicalsearch.dto.NoticeUpsertRequest;
import com.example.medicalsearch.entity.AppUser;
import com.example.medicalsearch.entity.Notice;
import com.example.medicalsearch.repository.AppUserRepository;
import com.example.medicalsearch.repository.NoticeRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class NoticeServiceTest {

    @Test
    void developerCanCreatePinnedNotice() {
        NoticeRepository noticeRepository = mock(NoticeRepository.class);
        AppUserRepository userRepository = mock(AppUserRepository.class);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(developer()));
        when(noticeRepository.save(any(Notice.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        NoticeService service = new NoticeService(noticeRepository, userRepository);

        NoticeResponse response = service.create(
                "admin",
                new NoticeUpsertRequest("IMPORTANT", "  점검 안내  ", "  점검 내용입니다.  ", true)
        );

        assertThat(response.category()).isEqualTo("IMPORTANT");
        assertThat(response.title()).isEqualTo("점검 안내");
        assertThat(response.content()).isEqualTo("점검 내용입니다.");
        assertThat(response.pinned()).isTrue();
    }

    private AppUser developer() {
        return new AppUser(
                "admin",
                "encoded-password",
                "개발자",
                "admin@example.com",
                "010-0000-0000",
                "서울특별시",
                new BigDecimal("37.5000000"),
                new BigDecimal("127.0000000"),
                LocalDateTime.now()
        );
    }
}
