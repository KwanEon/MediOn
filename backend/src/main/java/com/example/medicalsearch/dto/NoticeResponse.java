package com.example.medicalsearch.dto;

import com.example.medicalsearch.entity.Notice;
import java.time.LocalDateTime;

public record NoticeResponse(
        Long id,
        String category,
        String title,
        String content,
        boolean pinned,
        LocalDateTime publishedAt,
        LocalDateTime updatedAt
) {

    public static NoticeResponse from(Notice notice) {
        return new NoticeResponse(
                notice.getId(),
                notice.getCategory().name(),
                notice.getTitle(),
                notice.getContent(),
                notice.isPinned(),
                notice.getPublishedAt(),
                notice.getUpdatedAt()
        );
    }
}
