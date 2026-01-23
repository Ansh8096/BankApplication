package net.engineerAnsh.BankApplication.Repository;

import net.engineerAnsh.BankApplication.Entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction,Long> {

    // Spring Data JPA parses the method name and automatically creates a SQL query...
    // we will send accountId, and it will collect all the transactions.
    // that has same accountId in 'fromAccountId' or 'toAccountId' columns, then we will be collecting that transaction in the List...
    List<Transaction> findByFromAccount_AccountNumberOrToAccount_AccountNumber(
            String fromAccountNumber,
            String toAccountNumber
    );

    // What this query means :->
    // “Give me all successful transactions
    // where this account was either the sender or the receiver,
    // and that happened before a specific date/time.”
    @Query("""
        SELECT t FROM Transaction t
        WHERE
            (t.fromAccount.accountNumber = :accountNumber
             OR t.toAccount.accountNumber = :accountNumber)
        AND t.status = 'SUCCESS'
        AND t.createdAt < :beforeDate
    """)
    List<Transaction> findAllSuccessfulTransactionsBeforeDate( // Used for calculating opening balance
            @Param("accountNumber") String accountNumber,
            @Param("beforeDate") LocalDateTime beforeDate
    );


    // What this query means :->
    // “Give me all successful transactions
    // involving this account
    // that happened inside the statement period,
    // sorted in time order.”
    @Query("""
        SELECT t FROM Transaction t
        WHERE
            (t.fromAccount.accountNumber = :accountNumber
             OR t.toAccount.accountNumber = :accountNumber)
        AND t.status = 'SUCCESS'
        AND t.createdAt >= :start
        AND t.createdAt < :end
        ORDER BY t.createdAt
    """)
    List<Transaction> findStatementTransactions( // used for generating statement rows
            @Param("accountNumber") String accountNumber,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );


}







// 🧪 Example (Real Banking Scenario)
// Suppose:
// Account ID = 101
// You call:
// findByFromAccount_IdOrToAccount_Id(101L, 101L);

// This returns:
// All withdrawals from account 101
// All deposits into account 101
// All transfers sent from account 101
// All transfers received by account 101
//👉 Complete transaction history for that account