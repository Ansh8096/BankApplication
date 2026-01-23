package net.engineerAnsh.BankApplication.Dto.Statements;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class StatementRowDto {

    private LocalDate date;

    private String description;

    private BigDecimal debit;

    private BigDecimal credit;

    private BigDecimal balance;
}
