package net.engineerAnsh.BankApplication.Repository;

import net.engineerAnsh.BankApplication.Entity.Transaction;
import net.engineerAnsh.BankApplication.Enum.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

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

    // It returns the total transaction amount (SUM(amount)) for a given:
    // transaction type (WITHDRAWAL / TRANSFER / DEPOSIT), direction (DEBIT or CREDIT), account number, time range (start → end)
    // It returns 0 if no transactions exist...
    // SUM(t.amount) → adds all amounts that match the conditions, Sometimes SUM returns null if no rows matched (Eg: No transactions today)...
    // Direction Filter: We are using one query to support both debit and credit totals.
    // If direction = "DEBIT", Then we Count outgoing transactions from that account (e.g: WITHDRAWAL (money going out) AND TRANSFER (money going out))...
    // If direction = "CREDIT", Then we Count incoming transactions into that account:t (e.g: DEPOSIT (money in) AND TRANSFER received (money in)))...
    // Date Range filter is how we filter to a single day...
    @Query("""
                SELECT COALESCE(SUM(t.amount), 0)
                FROM Transaction t
                WHERE t.type = :type
                  AND t.status = 'SUCCESS'
                  AND (
                        (:direction = 'DEBIT' AND t.fromAccount.accountNumber = :accountNumber)
                     OR (:direction = 'CREDIT' AND t.toAccount.accountNumber = :accountNumber)
                  )
                  AND t.createdAt >= :start
                  AND t.createdAt < :end
            """)
    BigDecimal sumDailyAmount(
            @Param("type") TransactionType type,
            @Param("direction") String direction,
            @Param("accountNumber") String accountNumber,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    Optional<Transaction> findByClientTransactionId(String clientTransactionId);

    // This query used to count transactions for an account after a specific time...
    @Query("""
               SELECT COUNT(t)
               FROM Transaction t
               WHERE(
                       (:type = 'DEPOSIT' AND t.toAccount.accountNumber = :accountNumber)
                       OR
                       (:type != 'DEPOSIT' AND t.fromAccount.accountNumber = :accountNumber)
                   )
               AND t.type = :type
               AND t.createdAt >= :since
            """)
    int countTransactionsSince(
            String accountNumber,
            TransactionType type,
            LocalDateTime since
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