package com.example.medicalsearch.service;

import com.example.medicalsearch.dto.NoticeResponse;
import com.example.medicalsearch.dto.NoticeUpsertRequest;
import com.example.medicalsearch.entity.AppUser;
import com.example.medicalsearch.entity.Notice;
import com.example.medicalsearch.entity.NoticeCategory;
import com.example.medicalsearch.repository.AppUserRepository;
import com.example.medicalsearch.repository.NoticeRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final AppUserRepository userRepository;

    public NoticeService(
            NoticeRepository noticeRepository,
            AppUserRepository userRepository
    ) {
        this.noticeRepository = noticeRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<NoticeResponse> getNotices() {
        return noticeRepository.findAllByOrderByPinnedDescPublishedAtDesc()
                .stream()
                .map(NoticeResponse::from)
                .toList();
    }

    @Transactional
    public NoticeResponse create(String username, NoticeUpsertRequest request) {
        AppUser developer = findUser(username);
        LocalDateTime now = LocalDateTime.now();
        Notice notice = new Notice(
                parseCategory(request.category()),
                request.title().trim(),
                request.content().trim(),
                request.pinned(),
                developer.getId(),
                now
        );
        return NoticeResponse.from(noticeRepository.save(notice));
    }

    @Transactional
    public NoticeResponse update(Long noticeId, NoticeUpsertRequest request) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new IllegalArgumentException("공지를 찾을 수 없습니다."));
        notice.update(
                parseCategory(request.category()),
                request.title().trim(),
                request.content().trim(),
                request.pinned(),
                LocalDateTime.now()
        );
        return NoticeResponse.from(notice);
    }

    @Transactional
    public void delete(Long noticeId) {
        if (!noticeRepository.existsById(noticeId)) {
            throw new IllegalArgumentException("공지를 찾을 수 없습니다.");
        }
        noticeRepository.deleteById(noticeId);
    }

    private NoticeCategory parseCategory(String value) {
        try {
            return NoticeCategory.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("지원하지 않는 공지 유형입니다.");
        }
    }

    private AppUser findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }
}
