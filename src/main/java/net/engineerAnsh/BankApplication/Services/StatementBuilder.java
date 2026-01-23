package net.engineerAnsh.BankApplication.Services;

import net.engineerAnsh.BankApplication.Dto.Statements.StatementRowDto;
import net.engineerAnsh.BankApplication.Entity.Transaction;
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

            // Map every transaction in row of statement & add it to the List of 'rowStatements'...
            statementRows.add( new StatementRowDto(
                    tx.getCreatedAt().toLocalDate(),
                    tx.getRemark(),
                    debit,
                    credit,
                    runningBalance
            ));
        }

        // returning list of all the rows in statement...
        return statementRows;
    }

}
