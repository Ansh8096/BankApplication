package net.engineerAnsh.BankApplication.Repository;

import net.engineerAnsh.BankApplication.Entity.Account;
import net.engineerAnsh.BankApplication.Enum.AccountStatus;
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

    Optional<Account> findByAccountNumberAndAccountStatus(String accountNumber, AccountStatus accountStatus);

    Optional<Account> findByAccountNumber(String accountNumber);

    Optional<Account> findByAccountNumberAndAccountStatusNot(String accountNumber, AccountStatus accountStatus);

}
