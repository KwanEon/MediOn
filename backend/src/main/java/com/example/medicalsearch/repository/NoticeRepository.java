package com.example.medicalsearch.repository;

import com.example.medicalsearch.entity.Notice;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    List<Notice> findAllByOrderByPinnedDescPublishedAtDesc();
}
