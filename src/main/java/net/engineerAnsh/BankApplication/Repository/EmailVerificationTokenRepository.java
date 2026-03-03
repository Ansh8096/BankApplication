package net.engineerAnsh.BankApplication.Repository;

import net.engineerAnsh.BankApplication.Entity.EmailVerificationToken;
import net.engineerAnsh.BankApplication.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EmailVerificationTokenRepository
        extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByToken(String token);

    List<EmailVerificationToken> findByUserAndUsedFalse(User user);

    // It means find: The most recently created verification token for a given user.
    Optional<EmailVerificationToken>findTopByUserOrderByCreatedAtDesc(User user);

    // Count how many tokens this user has created after a specific time.
    long countByUserAndCreatedAtAfter(User user, LocalDateTime time);
}
