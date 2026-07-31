package com.example.medicalsearch.service;

import com.example.medicalsearch.dto.DeveloperInquiryPageResponse;
import com.example.medicalsearch.dto.DeveloperInquiryResponse;
import com.example.medicalsearch.dto.InquiryCreateRequest;
import com.example.medicalsearch.dto.InquiryResponse;
import com.example.medicalsearch.dto.PageResponse;
import com.example.medicalsearch.entity.AppUser;
import com.example.medicalsearch.entity.Inquiry;
import com.example.medicalsearch.entity.InquiryCategory;
import com.example.medicalsearch.repository.AppUserRepository;
import com.example.medicalsearch.repository.InquiryRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InquiryService {

    private static final int MAX_PAGE_SIZE = 100;

    private final InquiryRepository inquiryRepository;
    private final AppUserRepository userRepository;

    public InquiryService(
            InquiryRepository inquiryRepository,
            AppUserRepository userRepository
    ) {
        this.inquiryRepository = inquiryRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public InquiryResponse create(String username, InquiryCreateRequest request) {
        AppUser user = findUser(username);
        Inquiry inquiry = new Inquiry(
                user.getId(),
                parseCategory(request.category()),
                request.title().trim(),
                request.content().trim(),
                LocalDateTime.now()
        );
        return InquiryResponse.from(inquiryRepository.save(inquiry));
    }

    @Transactional(readOnly = true)
    public List<InquiryResponse> getMyInquiries(String username) {
        AppUser user = findUser(username);
        return inquiryRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(InquiryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public DeveloperInquiryPageResponse getDeveloperInquiries(int page, int size) {
        int normalizedPage = Math.max(0, page);
        int normalizedSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        Page<Inquiry> inquiries = inquiryRepository.findAll(
                PageRequest.of(
                        normalizedPage,
                        normalizedSize,
                        Sort.by(Sort.Direction.DESC, "createdAt")
                )
        );
        List<DeveloperInquiryResponse> items = inquiries.getContent().stream()
                .map(inquiry -> DeveloperInquiryResponse.from(
                        inquiry,
                        userRepository.findById(inquiry.getUserId())
                                .orElseThrow(() -> new IllegalArgumentException("문의 작성자를 찾을 수 없습니다."))
                ))
                .toList();
        return new DeveloperInquiryPageResponse(
                items,
                PageResponse.of(
                        inquiries.getNumber(),
                        inquiries.getSize(),
                        inquiries.getTotalElements()
                )
        );
    }

    @Transactional
    public void deleteOwn(String username, Long inquiryId) {
        AppUser user = findUser(username);
        Inquiry inquiry = inquiryRepository.findByIdAndUserId(inquiryId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("삭제할 문의를 찾을 수 없습니다."));
        inquiryRepository.delete(inquiry);
    }

    @Transactional
    public void deleteForDeveloper(Long inquiryId) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new IllegalArgumentException("삭제할 문의를 찾을 수 없습니다."));
        inquiryRepository.delete(inquiry);
    }

    private InquiryCategory parseCategory(String value) {
        try {
            return InquiryCategory.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("지원하지 않는 문의 유형입니다.");
        }
    }

    private AppUser findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }
}
