package com.example.medicalsearch.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "data_sync_histories")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DataSyncHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String sourceName;

    @Column(nullable = false, length = 50)
    private String targetType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DataSyncStatus status;

    @Column(nullable = false)
    private LocalDateTime syncedAt;

    @Column(length = 1000)
    private String message;
}
