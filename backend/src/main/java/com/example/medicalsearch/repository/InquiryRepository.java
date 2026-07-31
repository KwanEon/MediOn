package com.example.medicalsearch.repository;

import com.example.medicalsearch.entity.Inquiry;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    List<Inquiry> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Inquiry> findByIdAndUserId(Long id, Long userId);
}
