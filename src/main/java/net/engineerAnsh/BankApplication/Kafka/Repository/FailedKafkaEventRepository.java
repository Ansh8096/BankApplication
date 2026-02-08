package net.engineerAnsh.BankApplication.Kafka.Repository;

import net.engineerAnsh.BankApplication.Kafka.Entity.FailedKafkaEvent;
import net.engineerAnsh.BankApplication.Kafka.Enums.FailedEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FailedKafkaEventRepository extends JpaRepository<FailedKafkaEvent,Long>{

    List<FailedKafkaEvent> findByStatusNot(FailedEventStatus status);

    Optional<FailedKafkaEvent> findByTransactionReference(String transactionReference);
}
