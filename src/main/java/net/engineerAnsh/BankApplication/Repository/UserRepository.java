package net.engineerAnsh.BankApplication.Repository;

import jakarta.persistence.LockModeType;
import net.engineerAnsh.BankApplication.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User,Long> {

    List<User> findByRoles_Name(String roleName);

    boolean existsByEmail(String email);

    Optional<User> findByEmailAndActiveTrue(String email);

    Optional<User> findByUserIdAndActiveTrue(Long userId);

    @Query("SELECT u FROM User u WHERE u.active = true")
    List<User> findAllActiveUsers();

    // It means: When transaction starts, DB locks this user row, Other resend requests must wait...
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.email = :email")
    Optional<User> findByEmailForUpdate(@Param("email") String email);
}
