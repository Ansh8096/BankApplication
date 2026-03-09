package net.engineerAnsh.BankApplication.Repository;

import net.engineerAnsh.BankApplication.Entity.KycVerification;
import net.engineerAnsh.BankApplication.Enum.KycStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface KycVerificationRepository extends JpaRepository<KycVerification, Long> {

    Optional<KycVerification> findByUserUserId(Long userId); // check if user already submitted KYC

    List<KycVerification> findByStatus(KycStatus status); // admin dashboard for pending KYC

}