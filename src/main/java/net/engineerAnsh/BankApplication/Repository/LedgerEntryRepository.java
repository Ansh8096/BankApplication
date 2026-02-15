package net.engineerAnsh.BankApplication.Repository;

import net.engineerAnsh.BankApplication.Entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    // QUERY (Definition): This is a JPQL (Java Persistence Query Language) query, Works on entities, not tables, LedgerEntry l refers to the entity, not the DB table
    // This method is a custom JPA query used to calculate the account balance from ledger entries.
    // It follows the double-entry principle where: Balance = Total Credits − Total Debits
    @Query("""
                SELECT
                    COALESCE(SUM(
                        CASE
                            WHEN l.entryType = 'CREDIT' THEN l.amount
                            WHEN l.entryType = 'DEBIT' THEN -l.amount
                        END
                    ), 0)
                FROM LedgerEntry l
                WHERE l.account.accountNumber = :accountNumber
            """)
    BigDecimal calculateBalanceByAccountNumber(
            @Param("accountNumber") String accountNumber
    );
}
