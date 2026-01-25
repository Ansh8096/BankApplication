package net.engineerAnsh.BankApplication.Services;

import net.engineerAnsh.BankApplication.Dto.Statements.StatementRowDto;
import net.engineerAnsh.BankApplication.Entity.Transaction;
import net.engineerAnsh.BankApplication.Enum.TransactionType;
import net.engineerAnsh.BankApplication.Util.AccountMaskingUtil;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class StatementBuilder{

    // Convert transactions → statement rows
    public List<StatementRowDto> buildStatementRows(
            List<Transaction> transactions,
            String accountNumber,
            BigDecimal runningBalance
    ){
        List<StatementRowDto> statementRows = new ArrayList<>();

        // traversing the list of all the valid transactions, and mapping it to row of statement...
        for (Transaction tx : transactions){

            // Setting these values initially empty...
            BigDecimal debit = null;
            BigDecimal credit = null;
            String transactionType;
            String description;

            // Debit the opening balance, Because the amount was debited...
            if(tx.getFromAccount() != null &&
                    tx.getFromAccount().getAccountNumber().equals(accountNumber)){
                debit = tx.getAmount();
                runningBalance = runningBalance.subtract(debit);
            }
            // Credit the opening balance, Because the amount was credited...
            else if(tx.getToAccount() != null &&
                    tx.getToAccount().getAccountNumber().equals(accountNumber)){
                credit = tx.getAmount();
                runningBalance = runningBalance.add(credit);
            }

            if(tx.getType() == TransactionType.DEPOSIT
                    || tx.getType() == TransactionType.WITHDRAW)
            {
                transactionType = tx.getType().name();
                description = tx.getRemark();
            }

            else {
                if(tx.getFromAccount() != null &&
                        tx.getFromAccount().getAccountNumber().equals(accountNumber))
                {
                    transactionType = "TRF-SENT";
                    description = "To A/C " + AccountMaskingUtil.maskAccountNumber(
                            tx.getToAccount().getAccountNumber());
                }
                else {
                    transactionType = "TRF-REC";
                    assert tx.getFromAccount() != null;
                    description = "From A/C " + AccountMaskingUtil.maskAccountNumber(
                            tx.getFromAccount().getAccountNumber());
                }

            }

            // Map every transaction in row of statement & add it to the List of 'rowStatements'...
            statementRows.add( new StatementRowDto(
                    tx.getCreatedAt().toLocalDate(),
                    description,
                    debit,
                    credit,
                    runningBalance,
                    tx.getTransactionReference(),
                    transactionType
            ));
        }

        // returning list of all the rows in statement...
        return statementRows;
    }

}
