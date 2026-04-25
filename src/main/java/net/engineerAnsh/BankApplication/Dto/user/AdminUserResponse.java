package net.engineerAnsh.BankApplication.Dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.engineerAnsh.BankApplication.Enum.kyc.KycStatus;
import net.engineerAnsh.BankApplication.Enum.user.PhoneVerificationStatus;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminUserResponse {

    private String name;
    private String email;
    private boolean emailVerified;

    private String phoneNumber;
    private PhoneVerificationStatus phoneVerificationStatus;

    private Integer age;

    private KycStatus kycStatus;

    private boolean active;
    private boolean accountLocked;
    private int failedAttempts;

    private LocalDateTime lockTime;
    private LocalDateTime lastFailedAttempt;

    private String lastLoginIp;
    private String lastLoginLocation;
    private String lastLoginDevice;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLoginAt;
}
