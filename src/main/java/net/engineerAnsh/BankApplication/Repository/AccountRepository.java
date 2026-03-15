package net.engineerAnsh.BankApplication.Repository;

import jakarta.persistence.LockModeType;
import net.engineerAnsh.BankApplication.Entity.Account;
import net.engineerAnsh.BankApplication.Entity.User;
import net.engineerAnsh.BankApplication.Enum.AccountStatus;
import net.engineerAnsh.BankApplication.Enum.AccountType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    List<Account> findByUserEmail(String email);

    @Query("""
             SELECT a FROM Account a
             JOIN FETCH a.user
             WHERE a.accountStatus = :status
            """)
    List<Account> findActiveAccountsWithUser(@Param("status") AccountStatus status); // This query fetches the users eagerly...

    @Lock(LockModeType.PESSIMISTIC_WRITE) // Lock this row until transaction commits...
    @EntityGraph(attributePaths = "user") // Tells JPA: “ When loading Account, also load the user eagerly...
    @Query("""
       SELECT a
       FROM Account a
       WHERE a.accountNumber = :accountNumber
       AND a.accountStatus = :accountStatus
       """)
    Optional<Account> findAccountForUpdate(
            @Param("accountNumber") String accountNumber,
            @Param("accountStatus") AccountStatus accountStatus
    );

    // This query is for read-only services (like: statement job)...
    @EntityGraph(attributePaths = "user") // Tells JPA: “ When loading Account, also load the user eagerly...
    // Now this method: Fetches account, Fetches user in same query, Prevents LazyInitializationException
    Optional<Account> findByAccountNumberAndAccountStatus(
            String accountNumber,
            AccountStatus accountStatus
    );

    Optional<Account> findByAccountNumber(String accountNumber);

    Optional<Account> findByAccountNumberAndAccountStatusNot(String accountNumber, AccountStatus accountStatus);

    boolean existsByUserAndAccountTypeAndAccountStatusNot(
            User user,
            AccountType accountType,
            AccountStatus accountStatus
    );

}
