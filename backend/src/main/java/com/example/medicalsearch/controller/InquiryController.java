package com.example.medicalsearch.controller;

import com.example.medicalsearch.dto.InquiryCreateRequest;
import com.example.medicalsearch.dto.InquiryResponse;
import com.example.medicalsearch.service.InquiryService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/inquiries")
public class InquiryController {

    private final InquiryService inquiryService;

    public InquiryController(InquiryService inquiryService) {
        this.inquiryService = inquiryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    InquiryResponse create(
            Authentication authentication,
            @Valid @RequestBody InquiryCreateRequest request
    ) {
        return inquiryService.create(authentication.getName(), request);
    }

    @GetMapping("/me")
    List<InquiryResponse> myInquiries(Authentication authentication) {
        return inquiryService.getMyInquiries(authentication.getName());
    }

    @DeleteMapping("/{inquiryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteMyInquiry(
            Authentication authentication,
            @PathVariable Long inquiryId
    ) {
        inquiryService.deleteOwn(authentication.getName(), inquiryId);
    }
}
