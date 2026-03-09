package net.engineerAnsh.BankApplication.Dto.Kyc;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class KycReviewRequest {

    @NotNull(message = "Approval decision is required")
    private Boolean approved;

    private String rejectionReason;
}