package net.engineerAnsh.BankApplication.Repository;

import net.engineerAnsh.BankApplication.Entity.Account;
import net.engineerAnsh.BankApplication.Enum.AccountStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
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

    @EntityGraph(attributePaths = "user") // Tells JPA: “When loading Account, also load the user eagerly.”
    Optional<Account> findByAccountNumberAndAccountStatus( // Now this method: Fetches account, Fetches user in same query, Prevents LazyInitializationException
            @Param("accountNumber") String accountNumber,
            @Param("accountStatus") AccountStatus accountStatus
    );

    Optional<Account> findByAccountNumber(String accountNumber);

    Optional<Account> findByAccountNumberAndAccountStatusNot(String accountNumber, AccountStatus accountStatus);

}
