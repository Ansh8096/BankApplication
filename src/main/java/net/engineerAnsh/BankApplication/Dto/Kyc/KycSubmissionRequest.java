package net.engineerAnsh.BankApplication.Dto.Kyc;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import net.engineerAnsh.BankApplication.Enum.kyc.DocumentType;

@Data
public class KycSubmissionRequest {

    @NotNull(message = "Document type is required")
    private DocumentType documentType;

    @NotBlank(message = "Document number is required")
    private String documentNumber;

    @NotBlank(message = "Document URL is required")
    private String documentUrl;
}