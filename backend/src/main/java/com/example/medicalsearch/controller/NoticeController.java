package com.example.medicalsearch.controller;

import com.example.medicalsearch.dto.NoticeResponse;
import com.example.medicalsearch.service.NoticeService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notices")
public class NoticeController {

    private final NoticeService noticeService;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @GetMapping
    List<NoticeResponse> notices() {
        return noticeService.getNotices();
    }
}
