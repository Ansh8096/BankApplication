package net.engineerAnsh.BankApplication.Dto.Otp;

import lombok.Data;

@Data
public class VerifyOtpRequest {

    private String phoneNumber;
    private String otp;

}
