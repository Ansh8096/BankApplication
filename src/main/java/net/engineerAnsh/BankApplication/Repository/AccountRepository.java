package net.engineerAnsh.BankApplication.Repository;

import net.engineerAnsh.BankApplication.Entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccountRepository extends JpaRepository<Account,Long> {

    List<Account> findByUserUserId(Long userId);

    List<Account> findByUserEmail(String email);


}
