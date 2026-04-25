package net.engineerAnsh.BankApplication.Dto.Otp;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyOtpRequest {

    @NotBlank(message = "Phone number is required")
    private String phoneNumber;

    @NotBlank(message = "Otp is required")
    private String otp;

}
