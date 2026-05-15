package com.arcticsurge.cosmolab.domain.patient;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "patients")
@Getter
@Setter
@NoArgsConstructor
public class Patient {

    @Id
    @Column(columnDefinition = "UNIQUEIDENTIFIER")
    private UUID id;

    @Column(name = "first_name", columnDefinition = "NVARCHAR(100)", nullable = false)
    private String firstName;

    @Column(name = "last_name", columnDefinition = "NVARCHAR(100)", nullable = false)
    private String lastName;

    @Column(name = "personal_number", columnDefinition = "NVARCHAR(13)", nullable = false, unique = true)
    private String personalNumber;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "NVARCHAR(20)", nullable = false)
    private Gender gender;

    @Column(columnDefinition = "NVARCHAR(100)", nullable = false)
    private String ward;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "NVARCHAR(20)", nullable = false)
    private PatientStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
