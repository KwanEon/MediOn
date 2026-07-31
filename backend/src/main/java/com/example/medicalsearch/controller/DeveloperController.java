package com.example.medicalsearch.controller;

import com.example.medicalsearch.dto.DeveloperDashboardResponse;
import com.example.medicalsearch.dto.DeveloperInquiryPageResponse;
import com.example.medicalsearch.dto.DeveloperUserPageResponse;
import com.example.medicalsearch.dto.NoticeResponse;
import com.example.medicalsearch.dto.NoticeUpsertRequest;
import com.example.medicalsearch.dto.SyncTriggerResponse;
import com.example.medicalsearch.service.DeveloperService;
import com.example.medicalsearch.service.InquiryService;
import com.example.medicalsearch.service.NoticeService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/developer")
public class DeveloperController {

    private final DeveloperService developerService;
    private final NoticeService noticeService;
    private final InquiryService inquiryService;

    public DeveloperController(
            DeveloperService developerService,
            NoticeService noticeService,
            InquiryService inquiryService
    ) {
        this.developerService = developerService;
        this.noticeService = noticeService;
        this.inquiryService = inquiryService;
    }

    @GetMapping("/dashboard")
    DeveloperDashboardResponse dashboard() {
        return developerService.getDashboard();
    }

    @GetMapping("/users")
    DeveloperUserPageResponse users(
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return developerService.getUsers(query, page, size);
    }

    @PostMapping("/sync/{target}")
    ResponseEntity<SyncTriggerResponse> triggerSynchronization(@PathVariable String target) {
        SyncTriggerResponse response = developerService.triggerSynchronization(target);
        HttpStatus status = response.accepted() ? HttpStatus.ACCEPTED : HttpStatus.CONFLICT;
        return ResponseEntity.status(status).body(response);
    }

    @GetMapping("/notices")
    List<NoticeResponse> notices() {
        return noticeService.getNotices();
    }

    @PostMapping("/notices")
    ResponseEntity<NoticeResponse> createNotice(
            Authentication authentication,
            @Valid @RequestBody NoticeUpsertRequest request
    ) {
        NoticeResponse response = noticeService.create(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/notices/{noticeId}")
    NoticeResponse updateNotice(
            @PathVariable Long noticeId,
            @Valid @RequestBody NoticeUpsertRequest request
    ) {
        return noticeService.update(noticeId, request);
    }

    @DeleteMapping("/notices/{noticeId}")
    ResponseEntity<Void> deleteNotice(@PathVariable Long noticeId) {
        noticeService.delete(noticeId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/inquiries")
    DeveloperInquiryPageResponse inquiries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return inquiryService.getDeveloperInquiries(page, size);
    }

    @DeleteMapping("/inquiries/{inquiryId}")
    ResponseEntity<Void> deleteInquiry(@PathVariable Long inquiryId) {
        inquiryService.deleteForDeveloper(inquiryId);
        return ResponseEntity.noContent().build();
    }
}
