package net.engineerAnsh.BankApplication.Dto.Statements;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

//  Represents entire PDF
//  Clean, secure, immutable

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AccountStatementDto {

    private String bankName;
    private String accountHolderName;
    private String maskedAccountNumber;
    private String accountType;
    private String ifscCode;

    private LocalDate fromDate;
    private LocalDate toDate;

    private BigDecimal openingBalance;
    private List<StatementRowDto> transactions;
    private BigDecimal closingBalance;

}
