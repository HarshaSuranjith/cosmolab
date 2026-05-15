package com.arcticsurge.cosmolab.domain.patient;

import com.arcticsurge.cosmolab.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "patients")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class Patient extends BaseEntity {

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

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
