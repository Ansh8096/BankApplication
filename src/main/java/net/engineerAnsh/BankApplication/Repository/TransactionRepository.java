package net.engineerAnsh.BankApplication.Repository;

import net.engineerAnsh.BankApplication.Entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction,Long> {

    // Spring Data JPA parses the method name and automatically creates a SQL query...
    // we will send accountId, and it will collect all the transactions.
    // that has same accountId in 'fromAccountId' or 'toAccountId' columns, then we will be collecting that transaction in the List...
    List<Transaction> findByFromAccount_AccountNumberOrToAccount_AccountNumber(
            String fromAccountNumber,
            String toAccountNumber
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