package com.example.medicalsearch.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "medical_institutions")
public class MedicalInstitution {

    protected MedicalInstitution() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String hpid;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InstitutionType type;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 30)
    private String phoneNumber;

    @Column(length = 50)
    private String institutionKindName;

    @Column(nullable = false)
    private boolean emergencyRoomAvailable;

    @Column(length = 255)
    private String roadAddress;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private boolean nightService;

    @Column(nullable = false)
    private boolean twentyFourHours;

    @Column(nullable = false)
    private boolean saturdayService;

    @Column(nullable = false)
    private boolean sundayService;

    @Column(nullable = false)
    private boolean holidayService;

    @Column(length = 36)
    private String lastSeenSyncId;

    private LocalDateTime inactiveAt;

    @Column(nullable = false)
    private LocalDateTime lastSyncedAt;

    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
