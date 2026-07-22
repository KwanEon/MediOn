package com.example.medicalsearch.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_favorites",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_favorites_user_institution",
                columnNames = {"user_id", "institution_id"}
        )
)
public class UserFavorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "institution_id", nullable = false)
    private Long institutionId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected UserFavorite() {
    }

    public UserFavorite(Long userId, Long institutionId, LocalDateTime createdAt) {
        this.userId = userId;
        this.institutionId = institutionId;
        this.createdAt = createdAt;
    }

}
