package net.engineerAnsh.BankApplication.Dto.user;

import lombok.*;
import net.engineerAnsh.BankApplication.Enum.kyc.KycStatus;
import net.engineerAnsh.BankApplication.Enum.user.PhoneVerificationStatus;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse {

    private String name;
    private String email;
    private boolean emailVerified;

    private String phoneNumber;
    private PhoneVerificationStatus phoneVerificationStatus;

    private Integer age;

    private KycStatus kycStatus;

    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
    private String lastLoginDevice;
}