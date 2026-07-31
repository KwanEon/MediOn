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

@Entity
@Table(name = "data_sync_histories")
public class DataSyncHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_name", nullable = false, length = 100)
    private String sourceName;

    @Column(name = "target_type", nullable = false, length = 50)
    private String targetType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DataSyncStatus status;

    @Column(name = "synced_at", nullable = false)
    private LocalDateTime syncedAt;

    @Column(length = 1000)
    private String message;

    protected DataSyncHistory() {
    }

    public Long getId() {
        return id;
    }

    public String getSourceName() {
        return sourceName;
    }

    public String getTargetType() {
        return targetType;
    }

    public DataSyncStatus getStatus() {
        return status;
    }

    public LocalDateTime getSyncedAt() {
        return syncedAt;
    }

    public String getMessage() {
        return message;
    }
}
