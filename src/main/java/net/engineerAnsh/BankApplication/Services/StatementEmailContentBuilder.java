package net.engineerAnsh.BankApplication.Services;

import net.engineerAnsh.BankApplication.Dto.StatementEmailContent.StatementEmailContent;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

@Service
public class StatementEmailContentBuilder {

    public StatementEmailContent build(
            String accountNumber,
            String userName,
            LocalDate from,
            LocalDate to,
            String maskedAccountNumber,
            YearMonth month
    ) {
        String monthLabel = month.format(DateTimeFormatter.ofPattern("MMM-yyyy"));

        String subject = "Monthly Account Statement - " + monthLabel;

        String body = """
                Hello %s,
                
                Please find attached your monthly account statement.
                
                Period: %s to %s
                Account: %s
                
                Thanks,
                BANK OF ANSH
                """.formatted(userName, from, to, maskedAccountNumber);

        String fileName = "statement_" + accountNumber + "_" + monthLabel + ".pdf";

        return new StatementEmailContent(subject, body, fileName);
    }
}
