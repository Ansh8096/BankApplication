package net.engineerAnsh.BankApplication.Dto.Kyc;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.engineerAnsh.BankApplication.Enum.kyc.KycStatus;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class KycStatusResponse {

    private String referenceId;
    private KycStatus status;
    private String documentType;
    private LocalDateTime submittedAt;
    private String documentNumber;
    private String documentUrl;

}
