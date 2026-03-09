package net.engineerAnsh.BankApplication.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import net.engineerAnsh.BankApplication.Enum.KycStatus;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "kyc_verifications",
        indexes = {
                @Index(name = "idx_kyc_user", columnList = "user_id"),
                @Index(name = "idx_kyc_status", columnList = "status")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KycVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Each KYC belongs to one user...
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    // PAN / AADHAAR / PASSPORT...
    @Column(nullable = false)
    private String documentType;

    // Masked document number...
    @Column(nullable = false, length = 50)
    private String documentNumber;

    // File location (S3 / local storage)...
    @Column(nullable = false)
    private String documentUrl;

    // KYC status...
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private KycStatus status = KycStatus.SUBMITTED;

    // When user submitted KYC...
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime submittedAt;

    // When admin reviewed it...
    @Column
    private LocalDateTime reviewedAt;

    // Admin email who reviewed...
    @Column(name = "reviewed_by")
    private String reviewedBy;

    // Reason if rejected...
    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

}