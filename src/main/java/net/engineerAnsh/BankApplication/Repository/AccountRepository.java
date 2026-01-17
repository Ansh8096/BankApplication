package net.engineerAnsh.BankApplication.Repository;

import net.engineerAnsh.BankApplication.Entity.Account;
import net.engineerAnsh.BankApplication.Enum.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account,Long> {

    List<Account> findByUserUserId(Long userId);

    List<Account> findByUserEmail(String email);

    Optional<Account> findByIdAndAccountStatus(Long id, AccountStatus accountStatus);

    Optional<Account> findByAccountNumberAndAccountStatus(String accountNumber, AccountStatus accountStatus);

    Optional<Account> findByAccountNumber(String accountNumber);

    Optional<Account> findByAccountNumberAndAccountStatusNot(String accountNumber,AccountStatus accountStatus);

}
